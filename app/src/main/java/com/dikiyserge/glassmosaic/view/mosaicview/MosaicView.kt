package com.dikiyserge.glassmosaic.view.mosaicview

import android.content.Context
import android.graphics.Rect
import android.opengl.GLSurfaceView
import android.os.*
import android.provider.Settings.Global
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.Scroller
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.dikiyserge.glassmosaic.R
import com.dikiyserge.glassmosaic.data.getAngleBetweenPoints
import com.dikiyserge.glassmosaic.data.getHypotenuse
import com.dikiyserge.glassmosaic.data.getRotateAngleOfAngle
import com.dikiyserge.glassmosaic.data.getRotateDirection
import com.dikiyserge.glassmosaic.data.isNearlyEqual
import com.dikiyserge.glassmosaic.data.isZero
import com.dikiyserge.glassmosaic.data.*
import com.dikiyserge.glassmosaic.data.struct.MosaicFile
import com.dikiyserge.glassmosaic.getParcelableData
import com.dikiyserge.glassmosaic.view.log
import com.dikiyserge.glassmosaic.view.mosaicview.OpenGL.MosaicRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Единица измерения координат точек. Точки мозаики задаются от 0.0 до 1.0
private const val UNIT = 1.0

private const val MOSAIC_START = 0.0
private const val MOSAIC_END = UNIT

/* Размер области для навигации. Область навигации содержит мозаику (область от 0.0 до 1.0) и такую же по размеру
   область вокруг нее.
   x x x
   x m x
   x x x */
private const val SIZE = UNIT * 3

private const val START = -UNIT
private const val END = UNIT * 2

const val CENTER_X = 0.5
const val CENTER_Y = 0.5

// Масштаб, при котором ширина мазики равна ширине области
private const val SCALE_UNIT = 1.0

// Минимальный масштаб. При нем в видна мозаика и области вокруг нее.
private const val SCALE_MIN = UNIT / SIZE

private const val SCALE_MAX = 4.0

private const val START_SCALE = 0.5

// Значения ограничения скроллинга вне зависимости, выполняется ли изменения масштаба или нет
private const val SCROLL_X_MIN = -4.0
private const val SCROLL_X_MAX = 2.0
private const val SCROLL_Y_MIN = -6.0
private const val SCROLL_Y_MAX = 2.0

private const val SCROLL_STEP = 0.075

// Смещение окружающего контура от основной точки, для более точного определения выбранного элемента
private const val TOUCH_OFFSET = 0.025

private const val TOUCH_SIZE = 5
private const val TOUCH_INTERVAL_COUNT = TOUCH_SIZE - 1

private const val HANDLE_DELAY = 20L

private const val DEST_POS_OFFSET = 0.5
private const val DEST_POS_END = MOSAIC_END + DEST_POS_OFFSET

private const val DEST_MOVING_PAUSE = 2000

private const val ACTIVE_MOVING_STEP = 0.15

// Дополнительное смещение активного элемента, используется, чтобы были видны более мелкие элементы при активации
private const val ACTIVE_MOVING_OFFSET = 0.06

private const val ACTIVE_MOVING_POINT_SIZE = 0.16

private const val VELOCITY_COEFF = 0.06
private const val SCROLL_INT_COEFF = 1000000
private const val SCROLL_DELAY = 10L

private const val KEY_SUPER_STATE = "superState"
private const val KEY_STARTED = "started"
private const val KEY_MOSAIC = "mosaic"
private const val KEY_SCALE = "scale"
private const val KEY_SCROLL_X = "scrollX"
private const val KEY_SCROLL_Y = "scrollY"

private const val COL_COUNT = 2

// Количество дополнительно загруженных мозаик с начала и конца зоны отображения мозаик
private const val ADDITIONAL_LOAD_MOSAIC_COUNT = 4

enum class MosaicMode {
    MOSAIC, LIST
}

interface OnMosaicViewListListener {
    fun onLoadMosaic(mosaicFile: MosaicFile)
    fun onSelectMosaic(mosaic: Mosaic)
}

interface OnMosaicViewListener {
    fun onSaveMosaic(mosaic: Mosaic)
}

class MosaicView : GLSurfaceView,  GestureDetector.OnGestureListener, ScaleGestureDetector.OnScaleGestureListener,
    OnMovingGestureListener {

    private var _mode: MosaicMode = MosaicMode.MOSAIC

    val mode: MosaicMode
        get() = _mode

    val isModeMosaic
        get() = _mode == MosaicMode.MOSAIC

    val isModeList
        get() = _mode == MosaicMode.LIST

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MosaicView)
            val value = typedArray.getInt(R.styleable.MosaicView_mode, MosaicMode.MOSAIC.ordinal)
            _mode = MosaicMode.values()[value]
        }

        setEGLContextClientVersion(3)
        setEGLConfigChooser(renderer)

        setRenderer(renderer)
    }

    // Устанавливаем в true, когда получаем размеры области для вывода изображения
    private var initialized = false

    // Устанавливается в true, после выполнения инициализации мозайки. Выполняется один раз.
    private var started = false

    private val renderer = MosaicRenderer(context, this)

    private val gestureDetector = GestureDetector(context, this)
    private val scaleGestureDetector = ScaleGestureDetector(context, this)
    private val movingGesture = MovingGesture(this)

    private val scroller = Scroller(context)

    private var _mosaics = Mosaics(listOf())

    val mosaics
        get() = _mosaics

    private var rowCount = 0

    // Размер мозаки при списке
    private var itemSize: Double = 1.0

    private val startRowIndex: Int
        get() = getStartRowIndex(scrollY)

    private val endRowIndex: Int
        get() = getEndRowIndex(scrollY + height)

    val startMosaicIndex: Int
        get() = startRowIndex * COL_COUNT

    val endMosaicIndex: Int
        get() = endRowIndex * COL_COUNT + 1

    private var onMosaicViewListListener: OnMosaicViewListListener? = null
    private var onMosaicViewListener: OnMosaicViewListener? = null

    private val emptyMosaic = Mosaic(0, listOf(), MosaicState.NEW)

    var mosaic = emptyMosaic
        set(value) {
            field = Mosaic(value)
        }

    private val rect = Rect()

    /* Значение, при помощи которого выполняется перевод значений точек (от 0.0 до 1.0), в координаты экрана.
       Значением является ширина области */
    private val size: Int
        get() = rect.width()

    private val ratio: Double
        get() {
            return if (rect.width() == 0) {
                1.0
            } else {
                rect.height() / rect.width().toDouble()
            }
        }

    private var scrollX = 0.0
        set(value) {
            val minScrollX = getMinScrollX()
            val maxScrollX = getMaxScrollX()

            field = if (value < minScrollX) {
                minScrollX
            } else {
                if (value > maxScrollX) {
                    maxScrollX
                } else {
                    value
                }
            }

            setProjectionMatrix()
        }

    private var scrollY = 0.0
        set(value) {
            val minScrollY = getMinScrollY()
            val maxScrollY = getMaxScrollY()

            field = if (value < minScrollY) {
                minScrollY
            } else {
                if (value > maxScrollY) {
                    maxScrollY
                } else {
                    value
                }
            }

            if (isModeList) {
                getMosaicList()
            }

            setProjectionMatrix()
        }

    private val minScrollX2
        get() = START

    private val maxScrollX2
        get() = END - UNIT / scale

    // Высота области просмотра
    private val height
        get() = UNIT * ratio / scale

    private val minScrollY2: Double
        get() {
            val h = height

            return if (h > SIZE) {
                START - (h - SIZE)
            } else {
                START
            }
        }

    private val maxScrollY2: Double
        get() {
            val h = height

            return if (h > SIZE) {
                START
            } else {
                START + (SIZE - h)
            }
        }

    private var scale = SCALE_UNIT
        set(value) {
            var result = value

            if (isModeMosaic) {
                if (result < SCALE_MIN) {
                    result = SCALE_MIN
                }

                if (result > SCALE_MAX) {
                    result = SCALE_MAX
                }
            }

            field = result

            setProjectionMatrix()
        }

    private var scaling = false

    // Скроллинг, если он выполняется пользователем
    private var scrolling = false

    private var selectedElement: Element? = null

    private var selectOffsetX = 0.0
    private var selectOffsetY = 0.0

    private var activeElement: Element? = null
        set(value) {
            field = value
            renderer.activeElement = value
        }

    private var activeMoving = false

    // Необходимое смещение элемента при его активации, чтобы элемент был выше пальца
    private var activeMovingOffset = 0.0

    // Шаг при перемещении активного элемента
    private var activeMovingStep = 0.0

    private var prevRotatePoint = Point()

    private fun setProjectionMatrix() {
        renderer.setProjectionMatrix(ratio.toFloat(), scrollX.toFloat(), scrollY.toFloat(), scale.toFloat())
    }

    private fun getPointSize(coordSize: Float): Double {
        return if (size == 0 || scale == 0.0) {
            0.0
        } else {
            coordSize / size / scale
        }
    }

    private fun getPoint(coord: Coord): Point {
        val rectX = coord.x - rect.left
        val rectY = coord.y - rect.top
        val pointX = rectX / size / scale + scrollX
        val pointY = rectY / size / scale + scrollY

        return Point(pointX, pointY)
    }

    private fun getMinScrollX() = when (mode) {
        MosaicMode.MOSAIC -> SCROLL_X_MIN
        MosaicMode.LIST -> MOSAIC_START
    }

    private fun getMaxScrollX() = when (mode) {
        MosaicMode.MOSAIC -> SCROLL_X_MAX
        MosaicMode.LIST -> MOSAIC_START
    }

    private fun getMinScrollY() = when (mode) {
        MosaicMode.MOSAIC -> SCROLL_Y_MIN
        MosaicMode.LIST -> 0.0
    }

    private fun getMaxScrollY() = when (mode) {
        MosaicMode.MOSAIC -> SCROLL_Y_MAX
        MosaicMode.LIST -> max(UNIT - height, 0.0)
    }

    private fun scaleInPosition(point: Point, scaleFactor: Float) {
        val startScale = scale
        var scaling = false

        if (scaleFactor > SCALE_UNIT) {
            if (scale < SCALE_MAX) {
                scaling = true

                if (scale * scaleFactor > SCALE_MAX) {
                    scale = SCALE_MAX
                } else {
                    scale *= scaleFactor
                }
            }
        }

        if (scaleFactor < SCALE_UNIT) {
            if (scale > SCALE_MIN) {
                scaling = true

                if (scale * scaleFactor < SCALE_MIN) {
                    scale = SCALE_MIN
                } else {
                    scale *= scaleFactor
                }
            }
        }

        if (scaling) {
            val x = point.x - scrollX
            val y = point.y - scrollY

            val x1 = x * startScale
            val y1 = y * startScale
            val x2 = x * scale
            val y2 = y * scale

            val offsetX = (x2 - x1) / scale
            val offsetY = (y2 - y1) / scale

            scrollX += offsetX
            scrollY += offsetY
        }
    }

    private fun correctScrolling() {
        if (!scrolling && !scaling) {
            if (scrollX < minScrollX2 || scrollX > maxScrollX2 || scrollY < minScrollY2 || scrollY > maxScrollY2) {
                val endX = if (scrollX < minScrollX2) {
                    minScrollX2
                } else {
                    if (scrollX > maxScrollX2) {
                        maxScrollX2
                    } else {
                        scrollX
                    }
                }

                val endY = if (scrollY < minScrollY2) {
                    minScrollY2
                } else {
                    if (scrollY > maxScrollY2) {
                        maxScrollY2
                    } else {
                        scrollY
                    }
                }

                val point = getNextPos(scrollX, scrollY, endX, endY, SCROLL_STEP)
                scrollX = point.x
                scrollY = point.y
            }
        }
    }

    private fun getNextPos(startX: Double, startY: Double, endX: Double, endY: Double, step: Double): Point {
        val offsetX = startX - endX
        val offsetY = startY - endY

        val h = getHypotenuse(offsetX, offsetY)
        val ratio = h / step

        var x = if (isZero(h)) {
            endX
        } else {
            startX - offsetX / ratio
        }

        var y = if (isZero(h)) {
            endY
        } else {
            startY - offsetY / ratio
        }

        if (!isNearlyEqual(x, endX)) {
            val direction = endX >= startX

            if (direction) {
                if (x > endX) {
                    x = endX
                }
            } else {
                if (x < endX) {
                    x = endX
                }
            }
        }

        if (!isNearlyEqual(y, endY)) {
            val direction = endY >= startY

            if (direction) {
                if (y > endY) {
                    y = endY
                }
            } else {
                if (y < endY) {
                    y = endY
                }
            }
        }

        return Point(x, y)
    }

    // Возможна ли навигация пользователем
    private val canNavigate: Boolean
        get()  {
            return when (mode) {
                MosaicMode.MOSAIC -> {
                    mosaic.state in MosaicState.SOLVING..MosaicState.SOLVED
                }

                MosaicMode.LIST -> true
            }
        }

    private var destMovingTime = 0L

    private val mainCoroutineScope = CoroutineScope(Dispatchers.Main)

    private var handlerJob: Job? = null

    private val lifecycleScope: LifecycleCoroutineScope?
        get() = findViewTreeLifecycleOwner()?.lifecycleScope

    private fun startHandler() {
        handlerJob = lifecycleScope?.launch {
            while (isActive) {
                if (initialized) {
                    val time = Date().time

                    val start = if (started) {
                        false
                    } else {
                        started = true
                        true
                    }

                    handleMosaic(time, start)
                }

                delay(HANDLE_DELAY)
            }
        }
    }

    private fun stopHandler() {
        handlerJob?.cancel()
    }

    private fun handleMosaic(time: Long, start: Boolean) {
        if (start) {
            when (mosaic.state) {
                MosaicState.NEW -> {
                    destMovingTime = time + DEST_MOVING_PAUSE
                    scale = START_SCALE
                    center()

                    setDestPos()
                    mosaic.state = MosaicState.DEST_MOVING_PAUSE
                }

                MosaicState.SOLVING -> {
                    scale = START_SCALE
                    center()
                }

                MosaicState.SOLVED -> {
                    scale = SCALE_UNIT
                    center()
                }

                else -> {
                    //
                }
            }
        } else {
            when (mosaic.state) {
                MosaicState.DEST_MOVING_PAUSE -> {
                    if (time > destMovingTime) {
                        mosaic.state = MosaicState.DEST_MOVING
                    }
                }

                MosaicState.DEST_MOVING -> {
                    if (!mosaic.destMove()) {
                        mosaic.state = MosaicState.SOLVING
                        saveMosaic()
                    }
                }

                MosaicState.SOLVING -> {
                    activateElement(time)
                    activeElementMove()
                    correctRotation()
                    fix()
                    correctScrolling()

                    if (isSolved()) {
                        mosaic.state = MosaicState.SOLVED
                        scale = SCALE_UNIT
                        center()
                        saveMosaic()
                    }
                }

                MosaicState.SOLVED -> {
                    //
                }

                else -> {
                    //
                }
            }
        }
    }

    private fun handleTest(time: Long, start: Boolean) {
        if (start) {
            center()
        } else {
            activateElement(time)
            activeElementMove()
            correctRotation()
            fix()
        }
    }

    private fun getElementByPoint(point: Point, withoutFixed: Boolean): Element? {
        val touchPoints = getTouchPoints(point)

        for (element in mosaic.elements) {
            element.setTouchPointsIn(touchPoints)
        }

        val elements = if (withoutFixed) {
            mosaic.elements.filter { !it.fixed }
        } else {
            mosaic.elements
        }

        val element = elements.maxByOrNull { it.touchPointInCount }
        
        if (element != null) {
            val count = element.touchPointInCount

            if (count > 0) {
                val elements = elements.filter { it.touchPointInCount == count }

                if (elements.isNotEmpty()) {
                    return if (elements.size == 1) {
                        elements[0]
                    } else {
                        val elementsWithFirstTouchPointIn = elements.filter { it.touchPointsIn[0] }

                        if (elementsWithFirstTouchPointIn.isEmpty()) {
                            val elementsZ = elements.sortedByDescending { it.z }
                            elementsZ[0]
                        } else {
                            if (elementsWithFirstTouchPointIn.size == 1) {
                                elementsWithFirstTouchPointIn[0]
                            } else {
                                val elementsZ = elementsWithFirstTouchPointIn.sortedByDescending { it.z }
                                return elementsZ[0]
                            }
                        }
                    }
                }
            }
        }

        return null
    }

    // Получение точек, которые окружают указанную. Необходимо для более точного определения выбранного пользователем
    // элемента. Точки представляю собой квадрат без угловых точек:
    // o x x x o
    // x x x x x
    // x x p x x
    // x x x x x
    // o x x x o
    private fun getTouchPoints(point: Point): List<Point> {
        val touchOffset = TOUCH_OFFSET / scale

        val points = MutableList(TOUCH_POINT_COUNT) { Point() }
        val step = touchOffset * 2 / TOUCH_INTERVAL_COUNT

        val startPoint = Point(point)
        val offset = step * (TOUCH_INTERVAL_COUNT / 2)
        startPoint.offset(-offset, -offset)

        var index = 0

        for (row in 0 until TOUCH_SIZE) {
            for (col in 0 until TOUCH_SIZE) {
                val leftPoint = col == 0
                val rightPoint = col == TOUCH_SIZE - 1
                val topPoint = row == 0
                val bottomPoint = row == TOUCH_SIZE - 1

                val cornerPoint = (leftPoint && topPoint) || (rightPoint && topPoint) || (rightPoint && bottomPoint)
                        || (leftPoint && bottomPoint)

                if (!cornerPoint) {
                    points[index].x = startPoint.x + col * step
                    points[index].y = startPoint.y + row * step

                    index++
                }
            }
        }

        return points
    }

    private fun MotionEvent.isPointer1Up() = actionMasked == MotionEvent.ACTION_POINTER_UP
    private fun MotionEvent.isUp() = action == MotionEvent.ACTION_UP

    private fun activateElement(time: Long) {
        if (movingGesture.selected && !movingGesture.active) {
            val passedTime = time - movingGesture.selectTime

            if (passedTime > MOVING_TIME) {
                movingGesture.active = true
            }
        }
    }

    private fun activeElementMove() {
        if (activeMoving) {
            val element = activeElement

            if (element != null) {
                var step = if (activeMovingOffset - activeMovingStep < 0) {
                    activeMoving = false
                    activeMovingOffset
                } else {
                    activeMovingStep
                }

                element.y -= step
                activeMovingOffset -= step
            }
        }
    }

    private fun correctRotation() {
        for (element in mosaic.elements) {
            if (element != activeElement) {
                if (element.rotation.isNeededRotateCorrection) {
                    element.rotation.correctRotation()
                }
            }
        }
    }

    private fun fix() {
        for (element in mosaic.elements) {
            if (activeElement != element) {
                if (!element.fixed && element.closeForFixing) {
                    element.fix()
                }
            }
        }
    }

    private fun setDestPos() {
        val random = Random()
        val movedElements = mutableListOf<Element>()

        for (element in mosaic.elementsSize) {
            setElementDestPos(mosaic, element, random, movedElements)
        }
    }

    private fun setElementDestPos(mosaic: Mosaic, element: Element, random: Random,
                                  movedElements: MutableList<Element>) {
        for (i in 1..100) {
            val movedMosaic = Mosaic(0, movedElements, MosaicState.NEW)

            val x = getDestValue(random)
            val y = getDestValue(random)
            val rotateAngle = element.rotation.getRandomRotateAngle(random)

            val movedElement = Element(element)
            movedElement.x = x
            movedElement.y = y
            movedElement.rotation.rotateAngle = rotateAngle

            val correctPos = movedElement.right <= DEST_POS_END && movedElement.bottom <= DEST_POS_END
            val intersect = mosaic.intersectElementRect(movedElement)
            val intersectMoved = movedMosaic.intersectElementRect(movedElement)

            if (correctPos && !intersect && !intersectMoved) {
                element.destX = x
                element.destY = y

                element.rotation.destRotateAngle = rotateAngle

                movedElements.add(movedElement)

                return
            }
        }

        element.destX = 0.0
        element.destY = 0.0
        element.rotation.destRotateAngle = 0.0
    }

    private fun getDestValue(random: Random): Double {
        val value = DEST_POS_OFFSET * 2 + UNIT
        return -DEST_POS_OFFSET + value * random.nextDouble()
    }

    private fun isSolved(): Boolean {
        return if (mosaic.elements.isEmpty()) {
            false
        } else {
            mosaic.elements.all { it.fixed }
        }
    }

    private fun center() {
        val width = UNIT / scale
        val offsetX = -(width - UNIT) / 2
        val height = ratio / scale
        val offsetY = -(height - UNIT) / 2
        scrollX = offsetX
        scrollY = offsetY
    }

    private fun saveMosaic() {
        val mosaic = Mosaic(mosaic)

        if (mosaic.state in setOf(MosaicState.DEST_MOVING, MosaicState.DEST_MOVING_PAUSE)) {
            mosaic.state = MosaicState.NEW

            for (element in mosaic.elements) {
                element.clear()
            }
        }

        onMosaicViewListener?.onSaveMosaic(mosaic)
    }

    private fun setListParams() {
        rowCount = getRowCount()
        itemSize = getItemSize()

        // Вычисление масштаба в зависимости от itemSize
        scale = getListScale()

        getMosaicList()
    }

    private fun getRowCount(): Int {
        val count = _mosaics.count.toFloat()
        val value = count / COL_COUNT
        return value.roundToInt()
    }

    private fun getItemSize(): Double {
        return if (rowCount < 3) {
            // Если меньше 3-х рядов, то можно вместить до 4-х мозаик в область от 0.0 до 1.0
            UNIT / 2
        } else {
            // Вмещаем ряды мозаик в область от 0.0 до 1.0
            UNIT / rowCount
        }
    }

    private fun getMosaicList() {
        // Добавляем для загрузки предыдущий и последующий ряд
        val startIndex = startMosaicIndex - ADDITIONAL_LOAD_MOSAIC_COUNT
        val endIndex = endMosaicIndex + ADDITIONAL_LOAD_MOSAIC_COUNT

        val indexes = mutableListOf<Int>()

        for (index in startIndex..endIndex) {
            indexes.add(index)
        }

        val listener = onMosaicViewListListener

        if (listener != null) {
            for (index in indexes) {
                if (index in 0 until _mosaics.count) {
                    val mosaic = _mosaics[index]

                    if (mosaic == null) {
                        val mosaicFile = _mosaics.mosaicFiles[index]

                        if (!_mosaics.isLoadingMosaicId(mosaicFile.id)) {
                            _mosaics.addLoadingMosaicId(mosaicFile.id)
                            listener.onLoadMosaic(mosaicFile)
                        }
                    }
                }
            }
        }
    }

    private fun getStartRowIndex(startPos: Double): Int {
        val rowIndex = getRowIndex(startPos)

        return if (rowIndex < 0) {
            0
        } else {
            rowIndex
        }
    }

    private fun getEndRowIndex(endPos: Double): Int {
        val rowIndex = getRowIndex(endPos)

        return if (rowIndex >= rowCount) {
            rowCount - 1
        } else {
            rowIndex
        }
    }

    private fun getRowIndex(verticalPos: Double): Int {
        val value = verticalPos / itemSize
        return value.toInt()
    }

    private fun getColIndex(horizontalPos: Double): Int {
        val value = horizontalPos / itemSize
        return min(value.toInt(), COL_COUNT)
    }

    private fun setMosaicItemParams(mosaic: Mosaic, index: Int) {
        val col = getItemCol(index)
        val row = getItemRow(index)

        val x = itemSize * col
        val y = itemSize * row
        val size = itemSize

        mosaic.setItemParams(x, y, size)
    }

    private fun getItemCol(index: Int): Int {
        return index % COL_COUNT
    }

    private fun getItemRow(index: Int): Int {
        return index / COL_COUNT
    }

    private fun getMosaicIndex(rowIndex: Int, colIndex: Int): Int {
        return rowIndex * COL_COUNT + colIndex
    }

    private fun getListScale(): Double {
        val width = itemSize * COL_COUNT

        return if (width != 0.0) {
            UNIT / width
        } else {
            1.0
        }
    }

    private fun getScrollInt(scroll: Double): Int {
        val value = scroll * SCROLL_INT_COEFF
        return value.toInt()
    }

    private fun getScrollOfScrollInt(scrollInt: Int): Double {
        return scrollInt / SCROLL_INT_COEFF.toDouble()
    }

    fun setOnMosaicViewListListener(listener: OnMosaicViewListListener?) {
        onMosaicViewListListener = listener
    }

    fun setOnMosaicViewListener(listener: OnMosaicViewListener?) {
        onMosaicViewListener = listener
    }

    fun setMosaicFiles(mosaicFileList: List<MosaicFile>) {
        _mosaics = Mosaics(mosaicFileList)
        setListParams()
    }

    fun setMosaicList(mosaicList: MosaicList) {
        for (m in mosaicList) {
            val index = mosaics.getIndex(m.id)

            if (index != -1) {
                val value = mosaics[index]

                val set = if (value == null) {
                    true
                } else {
                    value.id == mosaicList.reloadMosaicId
                }

                if (set) {
                    val mosaic = Mosaic(m)
                    setMosaicItemParams(mosaic, index)
                    mosaics[index] = mosaic
                }
            }
        }
    }

    /* MosaicRenderListener */
    fun onSize(width: Int, height: Int) {
        rect.set(0, 0, width, height)

        if (isModeList && initialized) {
            setListParams()
        }

        initialized = true

        setProjectionMatrix()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable(KEY_SUPER_STATE, super.onSaveInstanceState())
        bundle.putBoolean(KEY_STARTED, started)

        if (mosaic.state == MosaicState.DEST_MOVING) {
            for (element in mosaic.elements) {
                element.x = element.destX

                log("element.x = ${element.x}")
                element.y = element.destY
                element.rotation.rotateAngle = element.rotation.destRotateAngle
            }

            mosaic.state = MosaicState.SOLVING
        }

        bundle.putParcelable(KEY_MOSAIC, mosaic)


        bundle.putDouble(KEY_SCALE, scale)
        bundle.putDouble(KEY_SCROLL_X, scrollX)
        bundle.putDouble(KEY_SCROLL_Y, scrollY)

        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getParcelableData(KEY_SUPER_STATE))
            started = state.getBoolean(KEY_STARTED, false)

            val mosaicValue = state.getParcelableData<Mosaic>(KEY_MOSAIC)

            if (mosaicValue != null) {
                mosaic = mosaicValue
            }

            scale = state.getDouble(KEY_SCALE, SCALE_UNIT)
            scrollX = state.getDouble(KEY_SCROLL_X)
            scrollY = state.getDouble(KEY_SCROLL_Y)
        }
    }

    override fun onTouchEvent(motionEvent: MotionEvent?): Boolean {
        super.onTouchEvent(motionEvent)

        return if (motionEvent != null) {
            when (mode) {
                MosaicMode.MOSAIC -> onTouchEventMosaic(motionEvent)
                MosaicMode.LIST -> gestureDetector.onTouchEvent(motionEvent)
            }
        } else {
            false
        }
    }

    private fun onTouchEventMosaic(motionEvent: MotionEvent): Boolean {
        if (movingGesture.onTouchEvent(motionEvent)) {
            //
        } else {
            scaleGestureDetector.onTouchEvent(motionEvent)

            if (scaleGestureDetector.isInProgress) {
                scaling = true
                scrolling = false
            }

            var gesture = true

            if (scaling) {
                if (motionEvent.isPointer1Up()) {
                    scaling = false
                    gesture = true
                } else {
                    gesture = false
                }
            }

            if (gesture) {
                if (motionEvent.isUp()) {
                    scrolling = false
                }

                return gestureDetector.onTouchEvent(motionEvent)
            }
        }

        return true
    }

    override fun onResume() {
        super.onResume()

        if (isModeMosaic) {
            startHandler()
        }
    }


    override fun onPause() {
        super.onPause()

        if (isModeMosaic) {
            stopHandler()
            saveMosaic()
        }
    }

    /* GestureDetector.OnGestureListener */
    override fun onDown(motionEvent: MotionEvent): Boolean {
        return true
    }

    override fun onShowPress(motionEvent: MotionEvent) {
        //
    }

    override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
        if (isModeList) {
            val coord = Coord(motionEvent.x, motionEvent.y)
            val point = getPoint(coord)

            val rowIndex = getRowIndex(point.y)
            val colIndex = getColIndex(point.x)

            val mosaicIndex = getMosaicIndex(rowIndex, colIndex)

            if (mosaicIndex in 0 until mosaics.count) {
                val mosaic = mosaics[mosaicIndex]

                if (mosaic != null) {
                    onMosaicViewListListener?.onSelectMosaic(mosaic)
                }
            }
        }

        return false
    }

    override fun onScroll(start: MotionEvent?, end: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (canNavigate) {
            if (start != null) {
                scrolling = true

                val pointDistanceX = getPointSize(distanceX)
                val pointDistanceY = getPointSize(distanceY)

                scrollX += pointDistanceX
                scrollY += pointDistanceY
                return true
            }
        }

        return false
    }

    override fun onLongPress(motionEvent: MotionEvent) {
        //
    }

    override fun onFling(
        motionEvent1: MotionEvent?, motionEvent2: MotionEvent, velocityX: Float, velocityY: Float
    ): Boolean {
        when (mode) {
            MosaicMode.MOSAIC -> return false

            MosaicMode.LIST -> {
                val velocity = -getPointSize(velocityY)
                val distance = velocity * VELOCITY_COEFF

                val scroll = getScrollInt(scrollY)
                val scrollDistance = getScrollInt(distance)

                scroller.startScroll(0, scroll, 0, scrollDistance)

                mainCoroutineScope.launch {
                    while (scroller.computeScrollOffset()) {
                        scrollY = getScrollOfScrollInt(scroller.currY)
                        delay(SCROLL_DELAY)
                    }
                }

                return true
            }
        }
    }

    /* ScaleGestureDetector.OnScaleGestureListener */
    override fun onScale(detector: ScaleGestureDetector): Boolean {
        if (canNavigate) {
            if (detector != null) {
                val point = getPoint(Coord(detector.focusX, detector.focusY))
                scaleInPosition(point, scaleGestureDetector.scaleFactor)
            }
        }

        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        return true
    }

    override fun onScaleEnd(p0: ScaleGestureDetector) {
        //
    }

    /* OnMovingGestureListener */
    override fun onSelect(coord: Coord): Boolean {
        val point = getPoint(coord)

        val withoutFixed = true //mode != MosaicMode.TEST

        val element = getElementByPoint(point, withoutFixed)

        selectedElement = element

        if (element != null) {
            val size = element.minSize * scale

            if (size < ACTIVE_MOVING_POINT_SIZE) {
                activeMovingOffset = element.rect.bottom - point.y + ACTIVE_MOVING_OFFSET / scale
                activeMovingStep = activeMovingOffset * ACTIVE_MOVING_STEP
            } else {
                activeMovingOffset = 0.0
                activeMovingStep = 0.0
            }

            selectOffsetX = point.x - element.x
            selectOffsetY = point.y - element.y + activeMovingOffset

            return true
        }

        return false
    }

    override fun onActivate() {
        val element = selectedElement

        if (element != null) {
            activeElement = element
            mosaic.setTopZ(element)

            activeMoving = true
        }
    }

    override fun onDeactivate() {
        activeElement = null
        activeMoving = false
    }

    override fun onMove(coord: Coord) {
        val element = activeElement

        if (element != null) {
            val point = getPoint(coord)

            element.x = point.x - selectOffsetX
            element.y = point.y - selectOffsetY

            activeMoving = false
        }
    }

    override fun onStartRotation(coord: Coord, rotateCoord: Coord) {
        val rotatePoint = getPoint(rotateCoord)
        prevRotatePoint.set(rotatePoint)
    }

    override fun onRotate(coord: Coord, rotateCoord: Coord) {
        val element = activeElement

        if (element != null) {
            val point = getPoint(coord)
            val rotatePoint = getPoint(rotateCoord)

            val direction = getRotateDirection(point, prevRotatePoint, rotatePoint)
            val angle = getAngleBetweenPoints(point,  prevRotatePoint, rotatePoint)
            val rotateAngle = getRotateAngleOfAngle(angle)

            val value = if (direction) rotateAngle else -rotateAngle
            element.rotation.inc(value)
            
            prevRotatePoint.set(rotatePoint)
        }
    }
}