package com.dikiyserge.glassmosaic.data

import android.os.Parcelable
import com.dikiyserge.glassmosaic.model.room.entities.PointsEntity
import kotlinx.android.parcel.Parcelize

const val POINT_COORD_COUNT = 2

@Parcelize
class Point(var x: Double = 0.0, var y: Double = 0.0, var id: Long = 0) : Parcelable {
    constructor(point: Point) : this(point.x, point.y, point.id)

    fun set(point: Point) {
        x = point.x
        y = point.y
    }

    fun offset(dx: Double, dy: Double) {
        x += dx
        y += dy
    }

    override fun toString(): String {
        return "x = $x, y = $y"
    }

    fun toPointsEntity(elementId: Long, num: Int): PointsEntity {
        return PointsEntity(id, elementId, num, x, y)
    }

    // Добавленная точка для триангуляции, используется для главных (четных) точек
    var addedPoint: Point? = null

    // Индекс ближайшей точки, если выполняется добавление уже добавленной точки
    var nearPointIndex = -1

    // Можно ли соединить главные точки
    var isConnected = false
}
