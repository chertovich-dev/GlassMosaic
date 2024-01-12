package com.dikiyserge.glassmosaic.data

import com.dikiyserge.glassmosaic.view.mosaicview.PointIndex

private const val MIN_DISTANCE = 0.0075
private const val CROSS_DISTANCE_COEFF = 0.5

// Максимальное расстояние до точки, чтобы считать точку близкой
private const val NEAR_POINT_DISTANCE_COEFF = 1.2

class Polygon() {
    constructor(points: Collection<Point>) : this() {
        addPoints(points)
    }

    private var _points = mutableListOf<Point>()

    val points: List<Point>
        get() = _points.toList()

    fun addPoint(point: Point) {
        _points.add(point)
    }

    fun addPoints(points: Collection<Point>) {
        _points.addAll(points)
    }

    val isNotEmpty: Boolean
        get() = _points.size > 0

    fun splitIntoTriangles(triangles: MutableCollection<Triangle>): Polygon {
        val polygon = Polygon()

        if (_points.size < TRIANGLE_POINT_COUNT) {
            return polygon
        }

        val pointIndex = PointIndex(_points)

        if (_points.size == TRIANGLE_POINT_COUNT) {
            val triangle = getTriangle(pointIndex)
            triangles.add(triangle)
        } else {
            setAddedPoints(pointIndex)
            setConnectedPoints(pointIndex)
            triangles.addAll(getTriangles(pointIndex))
            loadNewPolygon(polygon, pointIndex)
        }

        return polygon
    }

    private fun getTriangle(pointIndex: PointIndex): Triangle {
        val triangle = Triangle()

        pointIndex.first()

        for (i in 0 until TRIANGLE_POINT_COUNT) {
            triangle.points[i] = pointIndex.point
            pointIndex.prev()
        }

        return triangle
    }

    private fun setAddedPoints(pointIndex: PointIndex) {
        pointIndex.first()

        while (pointIndex.mainPointIterator.hasNext()) {
            setAddedPoint(pointIndex)

            pointIndex.mainPointIterator.next()
        }
    }

    private fun setAddedPoint(pointIndex: PointIndex) {
        val concave = isConcavePoint(pointIndex.prevPoint, pointIndex.point, pointIndex.nextPoint)

        val rayPoints = getRayPoints(pointIndex, concave)

        val middlePoint = rayPoints.first
        val startRayPoint = rayPoints.second
        val endRayPoint = rayPoints.third

        var distance = getDistance(pointIndex, middlePoint, concave)

        if (distance < MIN_DISTANCE) {
            distance = MIN_DISTANCE
        }

        val crossPoint = getNearCrossPoint(pointIndex.point, endRayPoint, pointIndex.prevPoint, pointIndex.point)

        if (crossPoint != null) {
            val crossDistance = getDistanceBetweenPoints(pointIndex.point, crossPoint)
            val crossDistanceCoeff = crossDistance * CROSS_DISTANCE_COEFF

            if (distance > crossDistanceCoeff) {
                distance = crossDistanceCoeff
            }
        }

        val point = extendLine(startRayPoint, pointIndex.point, distance)
        val nearPointIndex = getNearPointIndex(pointIndex.point, distance)

        pointIndex.point.addedPoint = if (nearPointIndex == -1) point else pointIndex.points[nearPointIndex].addedPoint
        pointIndex.point.nearPointIndex = nearPointIndex
    }

    private fun getRayPoints(pointIndex: PointIndex, concave: Boolean): Triple<Point, Point, Point> {
        val middlePoint = getMiddlePoint(pointIndex.prevPoint, pointIndex.point, pointIndex.nextPoint)

        val startRayPoint = if (concave) {
            middlePoint
        } else {
            val middleDistance = getDistanceBetweenPoints(pointIndex.point, middlePoint)

            // Переносим точку начала луча от Point в противоположную сторону на MiddleDistance
            extendLine(middlePoint, pointIndex.point, middleDistance)
        }

        val endRayPoint = extendLine(startRayPoint, pointIndex.point, 1.0)

        return Triple(middlePoint, startRayPoint, endRayPoint)
    }

    private fun getDistance(pointIndex: PointIndex, middlePoint: Point, concave: Boolean): Double {
        val distance = getDistanceBetweenPoints(pointIndex.point, middlePoint)
        return if (concave) distance / 2 else distance
    }

    private fun getNearCrossPoint(point1: Point, point2: Point, exceptPoint1: Point, exceptPoint2: Point): Point? {
        val pointIndex = PointIndex(_points)

        var nearCrossPoint: Point? = null

        while (pointIndex.pointIterator.hasNext()) {
            if (pointIndex.point != exceptPoint1 && pointIndex.point != exceptPoint2) {
                val crossPoint = getCrossPointOfLines(point1, point2, pointIndex.point, pointIndex.nextPoint)

                if (crossPoint != null) {
                    if (nearCrossPoint == null) {
                        nearCrossPoint = crossPoint
                    } else {
                        if (getDistanceBetweenPoints(point1, crossPoint) < getDistanceBetweenPoints(point1, nearCrossPoint)) {
                            nearCrossPoint = crossPoint
                        }
                    }
                }

            }

            pointIndex.pointIterator.next()
        }

        return nearCrossPoint
    }

    private fun getNearPointIndex(point: Point, distance: Double): Int {
        var index = -1
        var minDistance = Double.MAX_VALUE

        var d = distance * NEAR_POINT_DISTANCE_COEFF

        for ((i, p) in _points.withIndex()) {
            val addedPoint = p.addedPoint

            if (addedPoint != null) {
                val pointDistance = getDistanceBetweenPoints(point, addedPoint)

                if (pointDistance <= d && pointDistance < minDistance) {
                    minDistance = pointDistance
                    index = i
                }
            }
        }

        return index
    }

    private fun setConnectedPoints(pointIndex: PointIndex) {
        pointIndex.first()

        while (pointIndex.pointIterator.hasNext()) {
            pointIndex.point.isConnected = false

            val point1 = pointIndex.point.addedPoint

            val nextMainPoint = pointIndex.nextMainPoint
            val point2 = nextMainPoint.addedPoint

            if (point1 != null && point2 != null) {
                pointIndex.point.isConnected = isConnectedPoint(point1, point2)
            }

            pointIndex.pointIterator.next()
        }
    }

    private fun isConnectedPoint(point1: Point, point2: Point): Boolean {
        val pointIndex = PointIndex(_points)

        while (pointIndex.mainPointIterator.hasNext()) {
            val addedPoint = pointIndex.point.addedPoint

            if (addedPoint != null) {
                if (addedPoint != point1 && addedPoint != point2) {
                    val crossPoint = getCrossPointOfLines(point1, point2, pointIndex.point, addedPoint)

                    if (crossPoint != null) {
                        return false
                    }
                }
            }

            pointIndex.mainPointIterator.next()
        }

        pointIndex.first()

        while (pointIndex.pointIterator.hasNext()) {
            val crossPoint = getCrossPointOfLines(point1, point2, pointIndex.point, pointIndex.nextPoint)

            if (crossPoint != null) {
                return false
            }

            pointIndex.pointIterator.next()
        }

        return true
    }

    private fun getTriangles(pointIndex: PointIndex): MutableList<Triangle> {
        val triangles = mutableListOf<Triangle>()

        pointIndex.first()

        while (pointIndex.pointIterator.hasNext()) {
            val prevPoint = pointIndex.prevPoint
            val point = pointIndex.point
            val nextPoint = pointIndex.nextPoint

            val addedPoint = pointIndex.point.addedPoint

            val prevAddedPoint = prevPoint.addedPoint
            val nextAddedPoint = nextPoint.addedPoint

            if (addedPoint == null) {
                if (prevPoint.isConnected && nextPoint.nearPointIndex != pointIndex.prevIndex
                    && prevPoint.nearPointIndex != pointIndex.nextIndex) {

                    if (prevAddedPoint != null && nextAddedPoint != null) {
                        val triangle = Triangle()
                        triangle.point1 = point
                        triangle.point2 = prevAddedPoint
                        triangle.point3 = nextAddedPoint
                        triangles.add(triangle)
                    }
                }

                if (nextAddedPoint != null) {
                    val triangle = Triangle()
                    triangle.point1 = point
                    triangle.point2 = nextAddedPoint
                    triangle.point3 = nextPoint
                    triangles.add(triangle)
                }
            } else {
                val triangle = Triangle()
                triangle.point1 = point
                triangle.point2 = addedPoint
                triangle.point3 = nextPoint
                triangles.add(triangle)

                if (pointIndex.isLast() && point.nearPointIndex != pointIndex.nextIndex) {
                    if (nextAddedPoint != null) {
                        val triangle = Triangle()
                        triangle.point1 = addedPoint
                        triangle.point2 = nextAddedPoint
                        triangle.point3 = nextPoint
                        triangles.add(triangle)
                    }
                }
            }

            pointIndex.pointIterator.next()
        }

        return triangles
    }

    private fun loadNewPolygon(polygon: Polygon, pointIndex: PointIndex) {
        pointIndex.first()

        while (pointIndex.mainPointIterator.hasNext()) {
            val addedPoint = pointIndex.point.addedPoint

            if (addedPoint != null) {
                // Не добавляем точку, если она уже добавлена. Используется, если к главным точкам добавляется одна и та же точка
                if (polygon._points.indexOf(addedPoint) == -1) {
                    polygon.addPoint(addedPoint)
                }
            }

            if (!pointIndex.point.isConnected) {
                polygon.addPoint(pointIndex.nextPoint)
            }

            pointIndex.mainPointIterator.next()
        }
    }
}