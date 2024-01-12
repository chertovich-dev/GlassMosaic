package com.dikiyserge.glassmosaic.model.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "elements",
    foreignKeys = [
        ForeignKey(
            entity = MosaicsEntity::class,
            parentColumns = ["mosaic_id"],
            childColumns = ["mosaic_id"]
        )
    ]
)
data class ElementsEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "element_id") val elementId: Long,
    @ColumnInfo(name = "mosaic_id") val mosaicId: Long,
    val num: Int,
    val color: Int,
    val x: Double,
    val y: Double,
    val z: Int,
    val rotation: Double
)