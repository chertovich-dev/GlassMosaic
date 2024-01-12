package com.dikiyserge.glassmosaic.view.mosaicview

import android.view.MotionEvent
import com.dikiyserge.glassmosaic.data.Coord
import java.util.*

// Время, после которого происходит перемещение элемента
const val MOVING_TIME = 70

interface OnMovingGestureListener {
    fun onSelect(coord: Coord): Boolean
    fun onActivate()
    fun onDeactivate()
    fun onMove(coord: Coord)
    fun onStartRotation(coord: Coord, rotateCoord: Coord)
    fun onRotate(coord: Coord, rotateCoord: Coord)
}

class MovingGesture(onMovingGestureListener: OnMovingGestureListener) {
    private val listener: OnMovingGestureListener = onMovingGestureListener

    private var _selected = false
    private var _selectTime = 0L

    val selected
        get() = _selected

    val selectTime
        get() = _selectTime

    private var pointerId = -1

    private val coord = Coord()
    private val lastMoveCoord = Coord()
    private val lastRotateCoord = Coord()

    var active = false
        set(value) {
            val event = field != value

            field = value

            if (event) {
                if (value) {
                    listener.onActivate()
                } else {
                    listener.onDeactivate()
                }
            }
        }

    fun onTouchEvent(motionEvent: MotionEvent?): Boolean {
        if (motionEvent != null) {
            val action = motionEvent.actionMasked
            coord.set(motionEvent.x, motionEvent.y)

            if (action == MotionEvent.ACTION_DOWN) {
                _selected = listener.onSelect(coord)

                if (_selected) {
                    _selectTime = Date().time
                }
            }

            if (action == MotionEvent.ACTION_MOVE) {
                if (_selected && !active) {
                    val passedTime = Date().time - _selectTime

                    if (passedTime > MOVING_TIME) {
                        active = true
                    } else {
                        _selected = false
                    }
                }

                if (active) {
                    if (pointerId == -1) {
                        listener.onMove(coord)
                    } else {
                        if (coord != lastMoveCoord) {
                            listener.onMove(coord)
                        }

                        if (pointerId >= 0 && pointerId < motionEvent.pointerCount) {
                            val x = motionEvent.getX(pointerId)
                            val y = motionEvent.getY(pointerId)
                            val rotateCoord = Coord(x, y)

                            if (rotateCoord != lastRotateCoord) {
                                listener.onRotate(coord, rotateCoord)
                                lastRotateCoord.set(rotateCoord)
                            }
                        }
                    }

                    lastMoveCoord.set(coord)
                    return true
                }
            }

            if (action == MotionEvent.ACTION_POINTER_DOWN) {
                if (active) {
                    if (pointerId == -1) {
                        pointerId = motionEvent.getPointerId(motionEvent.actionIndex)
                        val x = motionEvent.getX(pointerId)
                        val y = motionEvent.getY(pointerId)
                        val rotateCoord = Coord(x, y)
                        listener.onStartRotation(coord, rotateCoord)
                        lastRotateCoord.set(rotateCoord)
                    }

                    return true
                }
            }

            if (action == MotionEvent.ACTION_POINTER_UP) {
                if (active) {
                    val id = motionEvent.getPointerId(motionEvent.actionIndex)

                    if (id == 0) {
                        _selected = false
                        active = false
                        pointerId = -1
                    }

                    if (id == pointerId) {
                        pointerId = -1
                    }

                    return true
                }
            }

            if (action == MotionEvent.ACTION_UP) {
                _selected = false
                active = false
                pointerId = -1
            }
        }

        return false
    }
}