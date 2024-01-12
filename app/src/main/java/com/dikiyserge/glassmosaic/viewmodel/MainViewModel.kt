package com.dikiyserge.glassmosaic.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dikiyserge.glassmosaic.data.Mosaic
import com.dikiyserge.glassmosaic.data.MosaicList
import com.dikiyserge.glassmosaic.data.MosaicState
import com.dikiyserge.glassmosaic.data.struct.MosaicFile
import com.dikiyserge.glassmosaic.model.Repository
import com.dikiyserge.glassmosaic.view.log
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class MainViewModel(private val repository: Repository) : ViewModel() {
    private val structure = repository.getStructure()
    val structureLiveData = MutableLiveData(structure)

    private val mosaics = ArrayList<MosaicList>()

    val mosaicsLiveData = ArrayList<MutableLiveData<MosaicList>>()

    init {
        for (i in 1..structure.groups.size) {
            val mosaicList = MosaicList()
            mosaics.add(mosaicList)

            val mutableLiveData = MutableLiveData(mosaicList)
            mosaicsLiveData.add(mutableLiveData)
        }
    }

    val navLiveEvent = SingleLiveEvent<Nav>()


    private fun setMosaic(mosaic: Mosaic) {
        val groupIndex = structure.getGroupIndex(mosaic.id)

        if (groupIndex != -1) {
            val mosaicList = mosaics[groupIndex]

            val mosaicIndex = mosaicList.getMosaicIndex(mosaic.id)

            if (mosaicIndex != -1) {
                mosaicList[mosaicIndex] = mosaic
                setMosaicsLiveData(groupIndex, mosaic.id)
            }
        }
    }

    private fun setMosaicsLiveData(groupIndex: Int, reloadMosaicId: Long = -1) {
        mosaics[groupIndex].reloadMosaicId = reloadMosaicId
        mosaicsLiveData[groupIndex].value = mosaics[groupIndex]
    }

    fun loadMosaic(groupIndex: Int, mosaicFile: MosaicFile)  {
        viewModelScope.launch {
            val mosaicList = mosaics[groupIndex]

            if (!mosaicList.contains(mosaicFile.id)) {
                val mosaic = repository.getMosaic(mosaicFile)

                if (!mosaicList.contains(mosaicFile.id)) {
                    mosaicList.add(mosaic)
                    setMosaicsLiveData(groupIndex)
                }
            }
        }
    }

    fun showMosaic(mosaic: Mosaic) {
        val nav = MainToMosaicNav(mosaic)
        navLiveEvent.value = nav
    }

    private suspend fun saveMosaicAndSet(mosaic: Mosaic) {
        repository.saveMosaic(mosaic)
        setMosaic(mosaic)
    }

    fun saveMosaic(mosaic: Mosaic) {
        viewModelScope.launch {
            saveMosaicAndSet(mosaic)
        }
    }

    suspend fun saveMosaicAndGetIds(mosaic: Mosaic) {
        saveMosaicAndSet(mosaic)
    }
}