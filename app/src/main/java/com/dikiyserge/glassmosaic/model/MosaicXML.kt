package com.dikiyserge.glassmosaic.model

import android.content.Context
import android.graphics.Color
import com.dikiyserge.glassmosaic.R
import com.dikiyserge.glassmosaic.data.*
import com.dikiyserge.glassmosaic.data.struct.Group
import com.dikiyserge.glassmosaic.data.struct.MosaicFile
import com.dikiyserge.glassmosaic.data.struct.Structure
import org.xmlpull.v1.XmlPullParser
import java.lang.NumberFormatException

const val TAG_MOSAICS = "Mosaics"
const val TAG_GROUP = "Group"
const val TAG_MOSAIC = "Mosaic"
const val TAG_ELEMENT = "Element"
const val TAG_POINT = "Point"

const val ATTR_ID = "id"
const val ATTR_FILE = "file"
const val ATTR_X = "x"
const val ATTR_Y = "y"
const val ATTR_COLOR = "color"

class MosaicXML(private val context: Context) {
    private fun startTagEquals(xml: XmlPullParser, tag: String): Boolean {
        return xml.eventType == XmlPullParser.START_TAG && xml.name.equals(tag, true)
    }

    private fun endTagEquals(xml: XmlPullParser, tag: String): Boolean {
        return xml.eventType == XmlPullParser.END_TAG && xml.name.equals(tag, true)
    }

    private fun readMosaicsFile(xml: XmlPullParser): Structure {
        val groups = mutableListOf<Group>()
        readGroups(xml, groups)
        return Structure(groups)
    }

    private fun readGroups(xml: XmlPullParser, groups: MutableList<Group>) {
        while (xml.eventType != XmlPullParser.END_DOCUMENT) {
            if (startTagEquals(xml, TAG_GROUP)) {
                val group = readGroup(xml)

                if (group != null) {
                    groups.add(group)
                }
            }

            xml.next()

            if (endTagEquals(xml, TAG_MOSAICS)) {
                break
            }
        }
    }

    private fun readGroup(xml: XmlPullParser): Group? {
        val idValue = xml.getAttributeValue(null, ATTR_ID)

        val id = idValue.toIntOrNull()

        return if (id == null) {
            null
        } else {
            val mosaicFiles = readMosaicFiles(xml)
            return Group(id, mosaicFiles)
        }
    }

    private fun readMosaicFiles(xml: XmlPullParser): List<MosaicFile> {
        val mosaicFiles = mutableListOf<MosaicFile>()

        while (xml.eventType != XmlPullParser.END_DOCUMENT) {
            if (startTagEquals(xml, TAG_MOSAIC)) {
                val mosaicFile = readMosaicFile(xml)

                if (mosaicFile != null) {
                    mosaicFiles.add(mosaicFile)
                }
            }

            xml.next()

            if (endTagEquals(xml, TAG_GROUP)) {
                break
            }
        }

        return mosaicFiles
    }

    private fun readMosaicFile(xml: XmlPullParser): MosaicFile? {
        var mosaic: MosaicFile? = null

        val idValue: String? = xml.getAttributeValue(null, ATTR_ID)
        val fileValue: String? = xml.getAttributeValue(null, ATTR_FILE)

        if (idValue != null && fileValue != null) {
            try {
                val id = idValue.toLong()
                mosaic = MosaicFile(id, fileValue)
            } catch (e: NumberFormatException) {
                //
            }
        }

        return mosaic
    }

    private fun readElements(xml: XmlPullParser): List<Element> {
        val elements = mutableListOf<Element>()

        while (xml.eventType != XmlPullParser.END_DOCUMENT) {
            if (startTagEquals(xml, TAG_ELEMENT)) {
                val element = readElement(xml)
                elements.add(element)
            }

            xml.next()

            if (endTagEquals(xml, TAG_MOSAIC)) {
                break
            }
        }

        return elements
    }

    private fun readElement(xml: XmlPullParser): Element {
        val colorValue: String? = xml.getAttributeValue(null, ATTR_COLOR)

        var color = EMPTY_COLOR

        if (colorValue != null) {
            try {
                color = Color.parseColor(colorValue)
            } catch (e: NumberFormatException) {
                //
            }
        }

        val points = readPoints(xml)

        val element = Element(color, points)
        element.createTriangles()

        return element
    }

    private fun readPoints(xml: XmlPullParser): List<Point> {
        val points = mutableListOf<Point>()

        while (xml.eventType != XmlPullParser.END_DOCUMENT) {
            if (startTagEquals(xml, TAG_POINT)) {
                val point = readPoint(xml)

                if (point != null) {
                    points.add(point)
                }
            }

            xml.next()

            if (endTagEquals(xml, TAG_ELEMENT)) {
                break
            }
        }

        return points
    }

    private fun readPoint(xml: XmlPullParser): Point? {
        var point: Point? = null
        val xValue: String? = xml.getAttributeValue(null, ATTR_X)
        val yValue: String? = xml.getAttributeValue(null, ATTR_Y)

        if (xValue != null && yValue != null) {
            try {
                val x = xValue.toDouble()
                val y = yValue.toDouble()
                point = Point(x, y)
            } catch (e: NumberFormatException) {
                //
            }
        }

        return point
    }

    fun loadStructure(): Structure {
        var xml = context.resources.getXml(R.xml.mosaics)

        while (xml.eventType != XmlPullParser.END_DOCUMENT) {
            if (startTagEquals(xml, TAG_MOSAICS)) {
                return readMosaicsFile(xml)
            }

            xml.next()
        }

        return Structure(listOf())
    }

    fun loadMosaic(mosaicFile: MosaicFile): Mosaic {
        val id = context.resources.getIdentifier(mosaicFile.file, "xml", context.packageName)

        if (id != 0) {
            var xml = context.resources.getXml(id)

            while (xml.eventType != XmlPullParser.END_DOCUMENT) {
                if (startTagEquals(xml, TAG_MOSAIC)) {
                    val elements = readElements(xml)
                    return Mosaic(mosaicFile.id, elements, MosaicState.NEW)
                }

                xml.next()
            }
        }

        return Mosaic(0, listOf(), MosaicState.NEW)
    }
}