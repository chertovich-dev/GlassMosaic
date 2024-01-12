package com.dikiyserge.glassmosaic.view.mosaicview

import com.dikiyserge.glassmosaic.data.isEven
import com.dikiyserge.glassmosaic.data.Point

const val POINT_INDEX_EOF = -1

class PointIndex(points: List<Point>) {
    val points = points
    private var _index = 0
    val index get() = _index

    private fun getNextIndexOfIndex(index: Int): Int {
        var nextIndex = index + 1

        if (nextIndex >= points.size) {
            nextIndex = 0
        }

        return nextIndex
    }

    private fun getPrevIndexOfIndex(index: Int): Int {
        var prevIndex = index - 1

        if (prevIndex < 0) {
            prevIndex = points.size - 1
        }

        return prevIndex
    }

    private fun isMainIndex(value: Int): Boolean = isEven(value)

    fun next(cycle: Boolean = true) {
        if (!isEof()) {
            if (!cycle && isLast()) {
                setEof()
            } else {
                _index = getNextIndexOfIndex(index)
            }
        }
    }

    fun prev() {
        _index = getPrevIndexOfIndex(index)
    }

    val prevPoint: Point
        get() = points[getPrevIndexOfIndex(index)]

    val point: Point
        get() = points[index]

    val nextPoint: Point
        get() = points[getNextIndexOfIndex(index)]

    fun first() {
        _index = 0
    }

    fun isFirst() = _index == 0

    fun isLast() = _index == points.size - 1

    fun isEof() = index == POINT_INDEX_EOF

    private fun setEof() {
        _index = POINT_INDEX_EOF
    }

    val nextMainPoint: Point
        get() {
            var index = getNextIndexOfIndex(_index)

            if (!isMainIndex(index)) {
                index = getNextIndexOfIndex(index)
            }

            return points[index]
        }

    val nextIndex get() = getNextIndexOfIndex(_index)
    val prevIndex get() = getPrevIndexOfIndex(_index)

    val pointIterator = PointIterator(this)
    val mainPointIterator = MainPointIterator(this)

    class PointIterator(private val pointIndex: PointIndex) : Iterator<Point> {
        override fun hasNext(): Boolean {
            return pointIndex.index >= 0 && pointIndex.index < pointIndex.points.size
        }

        override fun next(): Point {
            val point = pointIndex.point
            pointIndex.next(false)
            return point
        }
    }

    class MainPointIterator(private val pointIndex: PointIndex) : Iterator<Point> {
        override fun hasNext(): Boolean {
            return pointIndex.index >= 0 && pointIndex.index < pointIndex.points.size
                    && pointIndex.isMainIndex(pointIndex.index)
        }

        override fun next(): Point {
            val point = pointIndex.point

            pointIndex.next(false)
            pointIndex.next(false)

            return point
        }
    }

}