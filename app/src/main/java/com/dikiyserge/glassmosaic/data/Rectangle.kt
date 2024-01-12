package com.dikiyserge.glassmosaic.data

class Rectangle(private val leftTopPoint: Point, private val rightBottomPoint: Point) {
    private val points = mutableListOf<Point>()
    private val triangles = mutableListOf<Triangle>()

    private var _triangleVertexes = Vertexes(0)

    val triangleVertexes
        get() = _triangleVertexes

    private var _textureVertexes = Vertexes(0)

    val textureVertexes
        get() = _textureVertexes

    init {
        loadPoints()
        loadTriangles()
        loadTriangleVertexes()
        loadTextureVertexes()
    }

    private fun loadPoints() {
        points.add(leftTopPoint)
        points.add(Point(rightBottomPoint.x, leftTopPoint.y))
        points.add(rightBottomPoint)
        points.add(Point(leftTopPoint.x, rightBottomPoint.y))
    }

    private fun loadTriangles() {
        var polygon = Polygon(points)

        while (polygon.isNotEmpty) {
            polygon = polygon.splitIntoTriangles(triangles)
        }
    }

    private fun loadTriangleVertexes() {
        _triangleVertexes = Vertexes(triangles.size * TRIANGLE_POINT_COUNT)

        var index = 0

        for (triangle in triangles) {
            for (i in 0 until TRIANGLE_POINT_COUNT) {
                _triangleVertexes.values2[index++] = triangle.points[i].x.toFloat()
                _triangleVertexes.values2[index++] = triangle.points[i].y.toFloat()
            }
        }

        _triangleVertexes.loadData()
    }

    private fun loadTextureVertexes() {
        _textureVertexes = Vertexes(triangles.size * TRIANGLE_POINT_COUNT)

        var index = 0

        for (triangle in triangles) {
            for (i in 0 until TRIANGLE_POINT_COUNT) {
                _textureVertexes.values2[index++] = triangle.points[i].x.toFloat()
                _textureVertexes.values2[index++] = triangle.points[i].y.toFloat()
            }
        }

        _textureVertexes.loadData()
    }
}