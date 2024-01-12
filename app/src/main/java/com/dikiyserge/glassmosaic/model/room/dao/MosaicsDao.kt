package com.dikiyserge.glassmosaic.model.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dikiyserge.glassmosaic.model.room.entities.MosaicsEntity

@Dao
interface MosaicsDao {
    @Query("SELECT * FROM mosaics")
    fun getAll(): List<MosaicsEntity>

    @Query("SELECT * FROM mosaics WHERE mosaic_id = :mosaicId")
    fun findMosaic(mosaicId: Long): MosaicsEntity?

    @Insert
    fun insertMosaic(mosaicsEntity: MosaicsEntity): Long

    @Update
    fun updateMosaic(mosaicsEntity: MosaicsEntity)
}