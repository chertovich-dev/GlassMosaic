package com.dikiyserge.glassmosaic.model.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dikiyserge.glassmosaic.model.room.dao.ElementsDao
import com.dikiyserge.glassmosaic.model.room.dao.MosaicsDao
import com.dikiyserge.glassmosaic.model.room.dao.PointsDao
import com.dikiyserge.glassmosaic.model.room.entities.ElementsEntity
import com.dikiyserge.glassmosaic.model.room.entities.MosaicsEntity
import com.dikiyserge.glassmosaic.model.room.entities.PointsEntity

const val DATABASE_NAME = "database.db"

@Database(
    version = 1,
    entities = [
        MosaicsEntity::class,
        ElementsEntity::class,
        PointsEntity::class
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun getMosaicsDao(): MosaicsDao
    abstract fun getElementsDao(): ElementsDao
    abstract fun getPointsDao(): PointsDao
}