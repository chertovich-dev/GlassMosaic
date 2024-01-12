package com.dikiyserge.glassmosaic.data

import android.os.Parcel
import android.os.Parcelable
import com.dikiyserge.glassmosaic.model.room.entities.ElementsEntity
import com.dikiyserge.glassmosaic.readParcelableData
import com.dikiyserge.glassmosaic.readParcelableListData
import com.dikiyserge.glassmosaic.view.log
import com.dikiyserge.glassmosaic.view.mosaicview.Rotation
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlin.math.*

const val TOUCH_POINT_COUNT = 21

// Смещение элемента от исходной позиции, с которой выполняется фиксирование элемента
const val FIX_OFFSET = 0.025

// Шаг, с которым выполняется фиксация элемента
const val FIX_STEP = 0.003

// Шаг, с которым выполняется движение
const val DEST_STEP = 0.02

const val INTERSECT_BORDER_SIZE = 0.01

private const val TEXTURE_SIZE = 10f

@Parcelize
class Element(val color: Int, val sourcePoints: List<Point>, var id: Long = 0) : Parcelable {
    constructor(element: Element) : this(element.color, element.sourcePoints.map { Point(it) }, element.id) {
        x = element.x
        y = element.y
        z = element.z

        rotation.set(element.rotation)

        triangles.addAll(element.triangles)
        loadVertexes()
    }

    val sourceRect = PointRect(getLeft(sourcePoints), getTop(sourcePoints), getRight(sourcePoints),
        getBottom(sourcePoints))

    val size: Double
        get() = sourceRect.size

    // Получение минимального размера элемента (минимальная ширина или высота)
    val minSize: Double
        get() = min(sourceRect.width, sourceRect.height)

    private val sourceX
        get() = sourceRect.left

    private val sourceY
        get() = sourceRect.top

    // Выполнение триангуляции. Выполняется не в конструкторе, так как занимаем много времени
    fun createTriangles() {
        loadTriangles()
        loadVertexes()
    }

    private val triangles = mutableListOf<Triangle>()

    private var _triangleVertexes = Vertexes()

    val triangleVertexes: Vertexes
        get() = _triangleVertexes

    private var _frameVertexes = Vertexes()

    val frameVertexes: Vertexes
        get() = _frameVertexes

    private var _strokeVertexes = Vertexes()

    val strokeVertexes: Vertexes
        get() = _strokeVertexes

    private var _textureVertexes = Vertexes()

    val textureVertexes: Vertexes
        get() = _textureVertexes

    private fun loadVertexes() {
        loadTriangleVertexes()
        loadTextureVertexes()
        loadFrameVertexes()
        loadStrokeVertexes()
    }

    fun refreshVertexes() {
        setTriangleVertexes()
        _triangleVertexes.loadData()

        setStrokeVertexes()
        _strokeVertexes.loadData()
    }

    fun refreshVertexesForList() {
        setTriangleVertexesForList()
        _triangleVertexes.loadData()
    }

    // Точки элемента в зависимости от его позиции и поворота
    private val _points = List(sourcePoints.size) { Point() }

    val points: List<Point>
        get() {
            val count = min(sourcePoints.size, _points.size)
            val center = sourceRect.center

            val offsetX = sourceRect.halfWidth
            val offsetY = sourceRect.halfHeight

            val angle = getAngleOfRotation(rotation)

            for (i in 0 until count) {
                // Координаты относительно центра элемента
                val x0 = sourcePoints[i].x - center.x
                val y0 = sourcePoints[i].y - center.y

                // Повернутые координаты
                val rotatedX = rotatePointX(x0, y0, angle)
                val rotatedY = rotatePointY(x0, y0, angle)

                _points[i].x = rotatedX + offsetX + x
                _points[i].y = rotatedY + offsetY + y
            }

            return _points.toList()
        }

    // Позиция элемента
    var x = sourceX
    var y = sourceY

    // Порядок отрисовки элементов. Отрисовка начинается по возрастанию значения
    var z = 0
        get() = if (fixed) 0 else field

    // Поворот элемента по часовой стрелке относительно центра элемента (centerPoint). Значение от 0.0 до 1.0.
    // 0.0 - исходной положение, 0.25 - поворот на 90 градусов, 0.5 - поворот на 180 градусов, 1.0 - поворот на 360 градусов.
    val rotation = Rotation()

    var itemX = 0.0
    var itemY = 0.0
    var itemSize = 1.0

    private fun getLeft(points: List<Point>) = points.minByOrNull { it.x }?.x ?: 0.0
    private fun getRight(points: List<Point>) = points.maxByOrNull { it.x }?.x ?: 0.0
    private fun getTop(points: List<Point>) = points.minByOrNull { it.y }?.y ?: 0.0
    private fun getBottom(points: List<Point>) = points.maxByOrNull { it.y }?.y ?: 0.0

    val left get() = getLeft(points)
    val top get() = getTop(points)
    val right get() = getRight(points)
    val bottom get() = getBottom(points)

    val rect get() = PointRect(left, top, right, bottom)

    val center get() = Point(x + sourceRect.halfWidth, y + sourceRect.halfHeight)

    fun pointIn(point: Point): Boolean {
        if (rect.pointIn(point)) {
            // Точка попадает в элемент, если ее луч пересекает нечетное количество отрезков
            var result = false

            val list = points

            for ((index, _) in list.withIndex()) {
                val point1 = list[index]
                val point2 = if (index < list.size - 1) list[index + 1] else list[0]

                if (crossLine(point, point1, point2)) {
                    result = !result
                }
            }

           return result
        }

        return false
    }

    // Определяет, пересекает ли луч, направленный от точки point0 горизонтально вправо, линию, образованную точками point1, point2.
    // При этом точка point1 включительная, а point2 - нет.
    private fun crossLine(point0: Point, point1: Point, point2: Point): Boolean {
        val topPoint: Point
        val bottomPoint: Point
        val inclusiveTop: Boolean

        if (point1.y <= point2.y) {
            topPoint = Point(point1)
            bottomPoint = Point(point2)
            inclusiveTop = true
        } else {
            topPoint = Point(point2)
            bottomPoint = Point(point1)
            inclusiveTop = false
        }

        // Входит ли точка между верхней и нижней точками по Y
        val includeY: Boolean =
            if (inclusiveTop)
                point0.y >= topPoint.y && point0.y < bottomPoint.y
            else
                point0.y > topPoint.y && point0.y <= bottomPoint.y

        if (includeY) {
            val point = Point(point0)

            val offsetY = -topPoint.y

            // Смещаем все точки так, чтобы верхняя точка была 0
            point.offset(0.0, offsetY)
            topPoint.offset(0.0, offsetY)
            bottomPoint.offset(0.0, offsetY)

            // Коэффициент, показывающий где находится точка от верхней точки до нижней
            val coeff = point.y / bottomPoint.y

            val leftPoint: Point
            val rightPoint: Point

            if (topPoint.x <= bottomPoint.x) {
                leftPoint = topPoint
                rightPoint = bottomPoint
            } else {
                leftPoint = bottomPoint
                rightPoint = topPoint
            }

            val width = rightPoint.x - leftPoint.x

            val distanceX = width * coeff

            // Получение точки гипотенузы
            val pointX = if (leftPoint == topPoint) {
                    leftPoint.x + distanceX
                } else {
                    leftPoint.x + width - distanceX
                }

            return point.x <= pointX
        }

        return false
    }

    private val _touchPointsIn = MutableList(TOUCH_POINT_COUNT) { false }

    val touchPointsIn: List<Boolean>
        get() {
            return _touchPointsIn.toList()
        }

    fun setTouchPointsIn(touchPoints: List<Point>) {
        for (i in 0 until TOUCH_POINT_COUNT) {
            _touchPointsIn[i] = pointIn(touchPoints[i])
        }
    }

    val touchPointInCount: Int
        get() {
            return _touchPointsIn.count { it }
        }

    val fixed: Boolean
        get() {
            return if (rotation.isRotated) {
                false
            } else {
                isNearlyEqual(x, sourceX) && isNearlyEqual(y, sourceY)
            }
        }

    val closeForFixing: Boolean
        get() {
            return if (rotation.isRotated) {
                false
            } else {
                val offsetX = abs(x - sourceX)
                val offsetY = abs(y - sourceY)
                val offset = max(offsetX, offsetY)
                offset <= FIX_OFFSET
            }
        }

    fun fix() {
        val pos = getNextPos(x, y, sourceX, sourceY, FIX_STEP)
        x = pos.x
        y = pos.y
    }

    // Пересекаются ли прямоугольники элементов
    fun intersectElementRect(element: Element): Boolean {
        return rect.intersect(element.rect, INTERSECT_BORDER_SIZE)
    }

    var destX = 0.0
    var destY = 0.0

    val arrivedDest get() = isNearlyEqual(x, destX) && isNearlyEqual(y, destY)

    fun destMove() {
        val pos = getNextPos(x, y, destX, destY, DEST_STEP)
        x = pos.x
        y = pos.y
    }

    private fun loadTriangles() {
        clearPoints()
        triangles.clear()

        var polygon = Polygon(sourcePoints)

        while (polygon.isNotEmpty) {
            polygon = polygon.splitIntoTriangles(triangles)
        }
    }

    private fun clearPoints() {
        for (point in sourcePoints) {
            point.addedPoint = null
            point.nearPointIndex = -1
            point.isConnected = false
        }
    }

    private fun loadTriangleVertexes() {
        _triangleVertexes = Vertexes(triangles.size * TRIANGLE_POINT_COUNT)

        var index = 0

        for (triangle in triangles) {
            for (i in 0 until TRIANGLE_POINT_COUNT) {
                _triangleVertexes.values1[index++] = triangle.points[i].x.toFloat()
                _triangleVertexes.values1[index++] = triangle.points[i].y.toFloat()
            }
        }
    }

    private fun loadTextureVertexes() {
        _textureVertexes = Vertexes(triangles.size * TRIANGLE_POINT_COUNT)

        var index = 0

        val offsetX = sourceRect.left.toFloat()
        val offsetY = sourceRect.top.toFloat()

        for (triangle in triangles) {
            for (i in 0 until TRIANGLE_POINT_COUNT) {
                val x = (triangle.points[i].x.toFloat() - offsetX) * TEXTURE_SIZE
                val y = (triangle.points[i].y.toFloat() - offsetY) * TEXTURE_SIZE
                _textureVertexes.values2[index++] = x
                _textureVertexes.values2[index++] = y
            }
        }

        _textureVertexes.loadData()
    }

    private fun loadFrameVertexes() {
        _frameVertexes = Vertexes(sourcePoints.size)
        setFrameVertexes()
        _frameVertexes.loadData()
    }

    private fun loadStrokeVertexes() {
        _strokeVertexes = Vertexes(_points.size)
    }

    private fun setTriangleVertexes() {
        val center = sourceRect.center

        val offsetX = sourceRect.halfWidth
        val offsetY = sourceRect.halfHeight

        val angle = getAngleOfRotation(rotation)

        for (i in 0.._triangleVertexes.valueSize - 2 step 2) {
            // Координаты относительно центра элемента
            var x0 = _triangleVertexes.values1[i] - center.x
            var y0 = _triangleVertexes.values1[i + 1] - center.y

            // Повернутые координаты
            val rotatedX = rotatePointX(x0, y0, angle)
            val rotatedY = rotatePointY(x0, y0, angle)

            val valueX = (rotatedX + offsetX + x)
            val valueY = (rotatedY + offsetY + y)

            _triangleVertexes.values2[i] = valueX.toFloat()
            _triangleVertexes.values2[i + 1] = valueY.toFloat()
        }
    }

    private fun setTriangleVertexesForList() {
        for (i in 0.._triangleVertexes.valueSize - 2 step 2) {
            // Координаты относительно центра элемента
            var x0 = _triangleVertexes.values1[i]
            var y0 = _triangleVertexes.values1[i + 1]

            val valueX = x0 * itemSize + itemX
            val valueY = y0 * itemSize + itemY

            _triangleVertexes.values2[i] = valueX.toFloat()
            _triangleVertexes.values2[i + 1] = valueY.toFloat()
        }
    }

    private fun setFrameVertexes() {
        for ((i, p) in sourcePoints.withIndex()) {
            val xIndex = i * POINT_COORD_COUNT
            val yIndex = xIndex + 1
            _frameVertexes.values2[xIndex] = p.x.toFloat()
            _frameVertexes.values2[yIndex] = p.y.toFloat()
        }
    }

    private fun setStrokeVertexes() {
        val points = points

        for ((i, p) in points.withIndex()) {
            val xIndex = i * POINT_COORD_COUNT
            val yIndex = xIndex + 1
            _strokeVertexes.values2[xIndex] = p.x.toFloat()
            _strokeVertexes.values2[yIndex] = p.y.toFloat()
        }
    }

    fun clear() {
        x = sourceX
        y = sourceY
        z = 0
        rotation.set(Rotation())
    }

    override fun toString(): String {
        return "color = $color, x = $x, y = $y, z = $z, rotation = $rotation, triangles = $triangles, " +
                "sourcePoints = $sourcePoints"
    }

    fun toElementsEntity(mosaicId: Long, num: Int): ElementsEntity {
        return ElementsEntity(id, mosaicId, num, color, x, y, z, rotation.rotateAngle)
    }

    companion object : Parceler<Element> {
        override fun Element.write(parcel: Parcel, flags: Int) {
            parcel.writeInt(color)
            parcel.writeParcelableList(sourcePoints, 0)
            parcel.writeLong(id)
            parcel.writeDouble(x)
            parcel.writeDouble(y)
            parcel.writeInt(z)
            parcel.writeParcelable(rotation, 0)
            parcel.writeParcelableList(triangles, 0)
        }

        override fun create(parcel: Parcel): Element {
            val color = parcel.readInt()
            val sourcePoints = mutableListOf<Point>()
            parcel.readParcelableListData(sourcePoints)

            val id = parcel.readLong()

            val element = Element(color, sourcePoints, id)
            element.x = parcel.readDouble()
            element.y = parcel.readDouble()
            element.z = parcel.readInt()

            val rotation = parcel.readParcelableData<Rotation>()

            if (rotation != null) {
                element.rotation.set(rotation)
            }

            val triangles = mutableListOf<Triangle>()
            parcel.readParcelableListData(triangles)

            element.triangles.addAll(triangles)

            return element
        }
    }
}