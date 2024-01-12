package com.dikiyserge.glassmosaic.data

import kotlin.math.max
import kotlin.math.sqrt

class CoordRect(var left: Float = 0f, var top: Float = 0f, var right: Float = 0f, var bottom: Float = 0f) {
    val isZero get() = left == 0f && top == 0f && right == 0f && bottom == 0f

    fun set(left: Float, top: Float, right: Float, bottom: Float) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val size: Float get() = max(width, height)

    // Размер области, который необходим для вращения данной области без потери изображения.
    // Является диагональю квадрата, образованный по максимальному размеру стороны прямоугольника
    val rotateSize: Int get() = (size * sqrt(2.0)).toInt()

    val rotateOffsetX: Float get() = (rotateSize - width) / 2f
    val rotateOffsetY: Float get() = (rotateSize - height) / 2f
}