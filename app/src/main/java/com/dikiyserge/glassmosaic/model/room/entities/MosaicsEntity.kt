package com.dikiyserge.glassmosaic.model.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mosaics")
data class MosaicsEntity(
    @PrimaryKey @ColumnInfo(name = "mosaic_id") val mosaicId: Long,
    val state: Int
)