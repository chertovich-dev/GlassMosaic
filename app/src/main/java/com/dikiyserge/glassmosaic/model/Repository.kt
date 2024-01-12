package com.dikiyserge.glassmosaic.model

import com.dikiyserge.glassmosaic.data.Mosaic
import com.dikiyserge.glassmosaic.data.struct.MosaicFile
import com.dikiyserge.glassmosaic.data.struct.Structure

interface Repository {
    fun getStructure(): Structure
    suspend fun getMosaic(mosaicFile: MosaicFile): Mosaic
    suspend fun saveMosaic(mosaic: Mosaic)
}