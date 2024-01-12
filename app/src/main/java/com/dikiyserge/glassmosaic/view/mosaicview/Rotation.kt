package com.dikiyserge.glassmosaic.view.mosaicview

import android.os.Parcel
import android.os.Parcelable
import com.dikiyserge.glassmosaic.data.isNearlyEqual
import kotlinx.android.parcel.Parcelize
import kotlinx.parcelize.Parceler
import java.util.*
import kotlin.math.abs

const val MIN_ROTATE_ANGLE = 0.0
const val MAX_ROTATE_ANGLE = 1.0
const val HALF_ROTATE_ANGLE = 0.5

const val ROTATE_VALUE_COUNT = 8
const val ROTATE_CORRECTION_STEP = 0.007
const val DEST_ROTATE_STEP = 0.02

@Parcelize
class Rotation(rotAngle: Double = 0.0) : Parcelable {
    var rotateAngle = 0.0
        set(value) {
            field = if (value > MAX_ROTATE_ANGLE) {
                MAX_ROTATE_ANGLE
            } else {
                if (value < MIN_ROTATE_ANGLE) {
                    MIN_ROTATE_ANGLE
                } else {
                    value
                }
            }
        }

    private val _rotateValues = mutableListOf<Double>()

    private val rotateValues: List<Double>
        get() {
            return _rotateValues.toList()
        }

    init {
        rotateAngle = rotAngle
        addRotateValues()
    }

    private fun addRotateValues() {
        val step = MAX_ROTATE_ANGLE / ROTATE_VALUE_COUNT
        var value = MIN_ROTATE_ANGLE

        for (i in 1..ROTATE_VALUE_COUNT) {
            _rotateValues.add(value)
            value += step
        }

        _rotateValues.add(MAX_ROTATE_ANGLE)
    }

    val isNeededRotateCorrection: Boolean
        get() = !_rotateValues.contains(rotateAngle)

    fun correctRotation() {
        val nearestRotateValue = getNearestRotateValue()

        if (nearestRotateValue != rotateAngle) {
            val direction = nearestRotateValue >= rotateAngle

            rotateAngle = if (direction) {
                val value = rotateAngle + ROTATE_CORRECTION_STEP
                if (value > nearestRotateValue) nearestRotateValue else value
            } else {
                val value = rotateAngle - ROTATE_CORRECTION_STEP
                if (value < nearestRotateValue) nearestRotateValue else value
            }
        }
    }

    private fun getNearestRotateValue(): Double {
        for (i in 0 until ROTATE_VALUE_COUNT) {
            val rotate1 = _rotateValues[i]
            val rotate2 = _rotateValues[i + 1]

            if (rotateAngle in rotate1..rotate2) {
                val distance1 = abs(rotateAngle - rotate1)
                val distance2 = abs(rotateAngle - rotate2)

                return if (distance2 >= distance1) rotate1 else rotate2
            }
        }

        return 0.0
    }

    val isRotated: Boolean
        get() {
            return rotateAngle != 0.0 && rotateAngle != 1.0
        }

    fun inc(value: Double) {
        rotateAngle = getNextValue(value)
    }

    private fun getNextValue(incValue: Double): Double {
        val newValue = rotateAngle + incValue

        return if (incValue >= 0) {
            if (newValue > MAX_ROTATE_ANGLE) {
                newValue % MAX_ROTATE_ANGLE
            } else {
                newValue
            }
        } else {
            if (newValue < MIN_ROTATE_ANGLE) {
                MAX_ROTATE_ANGLE - abs(newValue % MAX_ROTATE_ANGLE)
            } else {
                newValue
            }
        }
    }

    fun getRandomRotateAngle(random: Random): Double {
        val index = 1 + random.nextInt(ROTATE_VALUE_COUNT - 1)
        return _rotateValues[index]
    }

    var destRotateAngle = 0.0

    private val destRotateDirection get() = destRotateAngle >= HALF_ROTATE_ANGLE

    val arrivedDest get() = isNearlyEqual(rotateAngle, destRotateAngle)

    fun destRotate() {
        if (!isNearlyEqual(rotateAngle, destRotateAngle)) {
            val direction = destRotateDirection
            val incValue = if (direction) DEST_ROTATE_STEP else -DEST_ROTATE_STEP
            val nextValue = getNextValue(incValue)

            val newValue = if (direction) {
                if (nextValue > destRotateAngle) {
                    destRotateAngle
                } else {
                    nextValue
                }
            } else {
                if (nextValue < destRotateAngle) {
                    destRotateAngle
                } else {
                    nextValue
                }
            }

            rotateAngle = newValue
        }
    }

    fun set(rotation: Rotation) {
        rotateAngle = rotation.rotateAngle
    }

    override fun toString(): String {
        return "rotateAngle = $rotateAngle"
    }

    companion object : Parceler<Rotation> {
        override fun Rotation.write(parcel: Parcel, flags: Int) {
            parcel.writeDouble(rotateAngle)
        }

        override fun create(parcel: Parcel): Rotation {
            val rotateAngle = parcel.readDouble()
            return Rotation(rotateAngle)
        }
    }
}
