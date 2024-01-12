package com.dikiyserge.glassmosaic.model

import android.content.Context
import androidx.room.Room
import com.dikiyserge.glassmosaic.data.Element
import com.dikiyserge.glassmosaic.data.Mosaic
import com.dikiyserge.glassmosaic.data.MosaicState
import com.dikiyserge.glassmosaic.data.Point
import com.dikiyserge.glassmosaic.data.struct.MosaicFile
import com.dikiyserge.glassmosaic.data.struct.Structure
import com.dikiyserge.glassmosaic.model.room.AppDatabase
import com.dikiyserge.glassmosaic.model.room.DATABASE_NAME
import com.dikiyserge.glassmosaic.model.room.entities.MosaicsEntity
import com.dikiyserge.glassmosaic.view.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainRepository(applicationContext: Context) : Repository {
    private val mosaicXML = MosaicXML(applicationContext)
    private val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, DATABASE_NAME).build()

    private fun loadMosaicFromDB(mosaicsEntity: MosaicsEntity): Mosaic {
        val elementsDao = db.getElementsDao()
        val pointsDao = db.getPointsDao()

        val elementsEntityList = elementsDao.loadElements(mosaicsEntity.mosaicId)

        val elements = mutableListOf<Element>()

        for (elementsEntity in elementsEntityList) {
            val pointsEntityList = pointsDao.loadPoints(elementsEntity.elementId)

            val points = mutableListOf<Point>()

            for (pointEntity in pointsEntityList) {
                val point = Point(pointEntity.x, pointEntity.y, pointEntity.pointId)
                points.add(point)
            }

            val element = Element(elementsEntity.color, points, elementsEntity.elementId)
            element.x = elementsEntity.x
            element.y = elementsEntity.y
            element.z = elementsEntity.z

            element.rotation.rotateAngle = elementsEntity.rotation
            element.createTriangles()

            elements.add(element)
        }

        val state = MosaicState.getValue(mosaicsEntity.state)

        return Mosaic(mosaicsEntity.mosaicId, elements, state)
    }

    override fun getStructure(): Structure {
        return mosaicXML.loadStructure()
    }

    override suspend fun getMosaic(mosaicFile: MosaicFile) = withContext(Dispatchers.IO) {
        val mosaicsDao = db.getMosaicsDao()

        val mosaicsEntity = mosaicsDao.findMosaic(mosaicFile.id)

        return@withContext if (mosaicsEntity == null) {
           mosaicXML.loadMosaic(mosaicFile)
        } else {
            loadMosaicFromDB(mosaicsEntity)
        }
    }

    override suspend fun saveMosaic(mosaic: Mosaic) {
        withContext(Dispatchers.IO) {
            val mosaicsDao = db.getMosaicsDao()
            val elementsDao = db.getElementsDao()
            val pointsDao = db.getPointsDao()

            val mosaicsEntity = mosaic.toMosaicsEntity()
            val savedMosaicsEntity = mosaicsDao.findMosaic(mosaic.id)

            if (savedMosaicsEntity == null) {
                val mosaicId = mosaicsDao.insertMosaic(mosaicsEntity)

                for ((index, element) in mosaic.elements.withIndex()) {
                    val elementsEntity = element.toElementsEntity(mosaicId, index)
                    val elementId = elementsDao.insertElement(elementsEntity)
                    element.id = elementId

                    for ((pointIndex, point) in element.sourcePoints.withIndex()) {
                        val pointsEntity = point.toPointsEntity(elementId, pointIndex)
                        val pointId = pointsDao.insertPoint(pointsEntity)
                        point.id = pointId
                    }
                }
            } else {
                mosaicsDao.updateMosaic(mosaicsEntity)

                for ((index, element) in mosaic.elements.withIndex()) {
                    val elementsEntity = element.toElementsEntity(mosaic.id, index)
                    elementsDao.updateElement(elementsEntity)
                }
            }
        }
    }
}