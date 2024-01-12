package com.dikiyserge.glassmosaic.data

import com.dikiyserge.glassmosaic.data.struct.MosaicFile
import com.dikiyserge.glassmosaic.view.log
import com.dikiyserge.glassmosaic.view.mosaicview.OpenGL.MosaicRenderer

class Mosaics(val mosaicFiles: List<MosaicFile>) {
    private val values = MutableList<Mosaic?>(mosaicFiles.size) { null }

    private val loadingMosaicsId = mutableListOf<Long>()

    val count = mosaicFiles.size

    operator fun get(index: Int) = values[index]

    operator fun set(index: Int, value: Mosaic) {
        values[index] = value
        loadingMosaicsId.remove(value.id)
    }

    fun getIndex(mosaicId: Long) = mosaicFiles.indexOfFirst { it.id == mosaicId }

    fun addLoadingMosaicId(mosaicId: Long) {
        loadingMosaicsId.add(mosaicId)
    }

    fun isLoadingMosaicId(mosaicId: Long) = loadingMosaicsId.contains(mosaicId)
}