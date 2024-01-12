package com.dikiyserge.glassmosaic.data

import android.os.Parcel
import android.os.Parcelable
import com.dikiyserge.glassmosaic.model.room.entities.MosaicsEntity
import com.dikiyserge.glassmosaic.readParcelableListData
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

enum class MosaicState {
    NEW, DEST_MOVING_PAUSE, DEST_MOVING, SOLVING, SOLVED;

    companion object {
        fun getValue(ordinal: Int): MosaicState {
            val values = MosaicState.values()

            return if (ordinal in values.indices) {
                values[ordinal]
            } else {
                NEW
            }
        }
    }
}

@Parcelize
class Mosaic(val id: Long, val elements: List<Element>, var state: MosaicState) : Parcelable {
    constructor(mosaic: Mosaic) : this(mosaic.id, mosaic.elements.map { Element(it) }, mosaic.state)

    val elementsZ: List<Element>
        get() {
            return elements.sortedBy { it.z }
        }

    // Элементы отсортированные по размеру, от большего к меньшему
    val elementsSize: List<Element>
        get() {
            return elements.sortedByDescending { it.size }
        }

    fun setTopZ(element: Element) {
        val other = elements.filter { it != element }
        val maxZ = other.maxByOrNull { it.z }?.z ?: 0

        if (element.z <= maxZ) {
            element.z = maxZ + 1
        }
    }

    fun intersectElementRect(element: Element): Boolean {
        return elements.any { it.intersectElementRect(element) }
    }

    fun destMove(): Boolean {
        var move = false

        for (element in elements) {
            if (!element.arrivedDest) {
                element.destMove()
                move = true
            }

            if (!element.rotation.arrivedDest) {
                element.rotation.destRotate()
                move = true
            }
        }

        return move
    }

    override fun toString(): String {
        return "id = $id, elements.size = ${elements.size}"
    }

    fun setItemParams(itemX: Double, itemY: Double, itemSize: Double) {
        for (element in elements) {
            element.itemX = itemX
            element.itemY = itemY
            element.itemSize = itemSize
        }
    }

    fun toMosaicsEntity(): MosaicsEntity {
        return MosaicsEntity(id, state.ordinal)
    }

    companion object : Parceler<Mosaic> {
        override fun Mosaic.write(parcel: Parcel, flags: Int) {
            parcel.writeLong(id)
            parcel.writeParcelableList(elements, 0)
            parcel.writeInt(state.ordinal)
        }

        override fun create(parcel: Parcel): Mosaic {
            val id = parcel.readLong()

            val elements = mutableListOf<Element>()
            parcel.readParcelableListData(elements)

            val stateOrdinal = parcel.readInt()
            val state = MosaicState.getValue(stateOrdinal)

            return Mosaic(id, elements, state)
        }
    }
}