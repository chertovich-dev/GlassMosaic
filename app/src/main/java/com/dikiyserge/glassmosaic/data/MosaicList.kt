package com.dikiyserge.glassmosaic.data

class MosaicList : ArrayList<Mosaic>() {
    var reloadMosaicId: Long = -1

    fun contains(mosaicId: Long): Boolean {
        val mosaic = find { it.id == mosaicId }
        return mosaic != null
    }

    fun getMosaicIndex(mosaicId: Long): Int {
        return indexOfFirst { it.id == mosaicId }
    }
}