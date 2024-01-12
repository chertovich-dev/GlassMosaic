package com.dikiyserge.glassmosaic.data

class Coords {
    private val _values = mutableListOf<Coord>()
    val values: List<Coord> get() = _values.toList()

    val rect = CoordRect()

    fun add(coord: Coord) {
        _values.add(coord)
        setRect(coord)
    }

    private fun setRect(coord: Coord) {
        if (rect.isZero) {
            rect.set(coord.x, coord.y, coord.x, coord.y)
        } else {
            if (coord.x < rect.left) {
                rect.left = coord.x
            }

            if (coord.x > rect.right) {
                rect.right = coord.x
            }

            if (coord.y < rect.top) {
                rect.top = coord.y
            }

            if (coord.y > rect.bottom) {
                rect.bottom = coord.y
            }
        }
    }
}