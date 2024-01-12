package com.dikiyserge.glassmosaic.data

import kotlin.math.max

data class PointRect(var left: Double = 0.0, var top: Double = 0.0, var right: Double = 0.0, var bottom: Double = 0.0) {
    constructor(pointRect: PointRect) : this(pointRect.left, pointRect.top, pointRect.right, pointRect.bottom)

    val width get() = right - left
    val height get() = bottom - top
    val halfWidth get() = width / 2
    val halfHeight get() = height / 2
    val size: Double get() = max(width, height)

    val center: Point
        get() {
            val x = left + halfWidth
            val y = top + halfHeight
            return Point(x, y)
        }

    val isZero get() = left == 0.0 && top == 0.0 && right == 0.0 && bottom == 0.0

    fun set(left: Double, top: Double, right: Double, bottom: Double) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    fun set(pointRect: PointRect) {
        this.left = pointRect.left
        this.top = pointRect.top
        this.right = pointRect.right
        this.bottom = pointRect.bottom
    }

    // Входит ли точка в прямоугольник
    fun pointIn(point: Point) = point.x in left..right && point.y in top..bottom

    // Прересекается ли с другим прямоугольником
    fun intersect(rect: PointRect, borderSize: Double = 0.0): Boolean {
        val left1 = left - borderSize
        val top1 = top - borderSize
        val right1 = right + borderSize
        val bottom1 = bottom + borderSize

        val left2 = rect.left - borderSize
        val top2 = rect.top - borderSize
        val right2 = rect.right + borderSize
        val bottom2 = rect.bottom + borderSize

        return left1 <= right2 && right1 >= left2 && top1 <= bottom2 && bottom1 >= top2
    }

    fun offset(dx: Double, dy: Double) {
        left += dx
        top += dy
        right += dx
        bottom += dy
    }

    override fun toString(): String {
        return "left = $left, top = $top, right = $right, bottom = $bottom"
    }
}