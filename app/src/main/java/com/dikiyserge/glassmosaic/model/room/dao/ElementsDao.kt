package com.dikiyserge.glassmosaic.model.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dikiyserge.glassmosaic.model.room.entities.ElementsEntity

@Dao
interface ElementsDao {
    @Insert
    fun insertElement(elementsEntity: ElementsEntity): Long

    @Query("SELECT * FROM elements WHERE mosaic_id = :mosaicId ORDER BY num")
    fun loadElements(mosaicId: Long): List<ElementsEntity>

    @Update
    fun updateElement(elementsEntity: ElementsEntity)
}