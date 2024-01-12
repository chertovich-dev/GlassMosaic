package com.dikiyserge.glassmosaic.data

data class Coord(var x: Float = 0f, var y: Float = 0f) {
    fun set(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    fun set(coord: Coord) {
        set(coord.x, coord.y)
    }

    override fun toString() = "x = $x, y = $y"
}
