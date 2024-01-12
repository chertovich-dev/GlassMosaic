package com.dikiyserge.glassmosaic.model.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "points",
    foreignKeys = [
        ForeignKey(
            entity = ElementsEntity::class,
            parentColumns = ["element_id"],
            childColumns = ["element_id"]
        )
    ]
)
data class PointsEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "points") val pointId: Long,
    @ColumnInfo(name = "element_id") val elementId: Long,
    val num: Int,
    val x: Double,
    val y: Double
)
