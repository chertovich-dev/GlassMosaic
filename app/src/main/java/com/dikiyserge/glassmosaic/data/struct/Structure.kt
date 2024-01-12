package com.dikiyserge.glassmosaic.data.struct

data class Structure(val groups: List<Group>) {
    fun getGroupIndex(mosaicId: Long): Int {
        for ((index, group) in groups.withIndex()) {
            val mosaicIndex = group.getMosaicFileIndex(mosaicId)

            if (mosaicIndex != -1) {
                return index
            }
        }

        return -1
    }
}