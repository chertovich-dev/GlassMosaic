package com.dikiyserge.glassmosaic.data

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Vertexes(val size: Int = 0) {
    val valueSize = size * POINT_COORD_COUNT

    // Исходные значения
    val values1 = FloatArray(valueSize)

    // Итоговые значения после преобразований для помещения в буфер
    val values2 = FloatArray(valueSize)

    private val _data = ByteBuffer.allocateDirect(valueSize * Float.SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer()

    val data: FloatBuffer
        get() {
            _data.position(0)
            return _data
        }

    fun loadData() {
        _data.position(0)
        _data.put(values2)
    }
}