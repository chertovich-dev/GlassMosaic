package com.dikiyserge.glassmosaic.data.struct

data class Group(val id: Int, val mosaicFiles: List<MosaicFile>) {
    fun getMosaicFileIndex(mosaicId: Long): Int {
        for ((index, mosaicFile) in mosaicFiles.withIndex()) {
            if (mosaicFile.id == mosaicId) {
                return index
            }
        }

        return -1
    }
}