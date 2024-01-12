package com.dikiyserge.glassmosaic.model.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dikiyserge.glassmosaic.model.room.entities.PointsEntity

@Dao
interface PointsDao {
    @Insert
    fun insertPoint(pointsEntity: PointsEntity): Long

    @Query("SELECT * FROM points WHERE element_id = :elementId ORDER BY num")
    fun loadPoints(elementId: Long): List<PointsEntity>
}