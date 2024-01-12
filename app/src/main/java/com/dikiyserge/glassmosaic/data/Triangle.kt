package com.dikiyserge.glassmosaic.data

import android.os.Parcel
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.parcelize.Parceler

import com.dikiyserge.glassmosaic.readParcelableArrayData
import com.dikiyserge.glassmosaic.view.log

const val TRIANGLE_POINT_COUNT = 3

private const val POINT_INDEX1 = 0
private const val POINT_INDEX2 = 1
private const val POINT_INDEX3 = 2

@Parcelize
class Triangle : Parcelable {
    val points = Array(TRIANGLE_POINT_COUNT) { Point() }

    var point1: Point
        get() {
            return points[POINT_INDEX1]
        }

        set(value) {
            points[POINT_INDEX1] = value
        }

    var point2: Point
        get() {
            return points[POINT_INDEX2]
        }

        set(value) {
            points[POINT_INDEX2] = value
        }

    var point3: Point
        get() {
            return points[POINT_INDEX3]
        }

        set(value) {
            points[POINT_INDEX3] = value
        }

    override fun toString() = "$point1; $point2; $point3"

    companion object : Parceler<Triangle> {
        override fun Triangle.write(parcel: Parcel, flags: Int) {
            parcel.writeParcelableArray(points, 0)
        }

        override fun create(source: Parcel): Triangle {
            val triangle = Triangle()

            val array = source.readParcelableArrayData<Point>()

            for (i in 0 until TRIANGLE_POINT_COUNT) {
                val point = array?.get(i)

                if (point is Point) {
                    triangle.points[i] = point
                }
            }

            return triangle
        }
    }
}