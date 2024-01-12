
package com.dikiyserge.glassmosaic.data

import com.dikiyserge.glassmosaic.view.mosaicview.Rotation
import kotlin.math.*

const val QUADRILLION = 1000000000000000

const val PARALLEL_PREC = 0.0001

// Получение угла в радианах из rotation, который имеет значение от 0.0 до 1.0
fun getAngleOfRotation(rotation: Rotation) = rotation.rotateAngle * 2 * PI

fun getRotateAngleOfAngle(angle: Double) = angle / (2 * PI)

// Вращение координаты x по часовой стрелке относительно точки (0.0, 0.0)
fun rotatePointX(x: Double, y: Double, angle: Double) = x * cos(angle) - y * sin(angle)

// Вращение координаты y по часовой стрелке относительно точки (0.0, 0.0)
fun rotatePointY(x: Double, y: Double, angle: Double) = x * sin(angle) + y * cos(angle)

fun rotatePoint(point: Point, angle: Double): Point {
    val p = Point()
    p.x = rotatePointX(point.x, point.y, angle)
    p.y = rotatePointY(point.x, point.y, angle)
    return p
}

fun getHypotenuse(side1: Double, side2: Double): Double = sqrt(side1.pow(2) + side2.pow(2))

fun getDistanceBetweenPoints(point1: Point, point2: Point): Double {
    val dx = abs(point2.x - point1.x)
    val dy = abs(point2.y - point1.y)
    return getHypotenuse(dx, dy)
}

// Получение направления поворота. true - если поворот происходит по часовой стрелке, false - против
fun getRotateDirection(centerPoint: Point, firstPoint: Point, secondPoint: Point): Boolean {
    val offsetX = -centerPoint.x
    val offsetY = -centerPoint.y

    val point1 = Point(firstPoint)
    point1.offset(offsetX, offsetY)

    val point2 = Point(secondPoint)
    point2.offset(offsetX, offsetY)

    val quarter1 = getQuarterOfPoint(point1)
    val quarter2 = getQuarterOfPoint(point2)

    val minQuarter = minQuarter(quarter1, quarter2)

    // Центр окружности
    val point0 = Point()

    // Начало отсчета угла
    val startPoint = getStartPointOfQuarter(minQuarter)

    val angle1 = getAngleBetweenPoints(point0, startPoint, point1)
    val angle2 = getAngleBetweenPoints(point0, startPoint, point2)

    return angle2 >= angle1
}

// Получение четверти, к которой относится point, относительно координаты 0.0
private fun getQuarterOfPoint(point: Point): Int {
    if (point.x >= 0 && point.y < 0) {
        return 0
    }

    if (point.x >= 0 && point.y >= 0) {
        return 1
    }

    if (point.x < 0 && point.y >= 0) {
        return 2
    }

    if (point.x < 0 && point.y < 0) {
        return 3
    }

    return -1
}

// Получение минимальной четвери (та, с которой выполняется поворот по часовой стрелке)
private fun minQuarter(quarter1: Int, quarter2: Int): Int {
    val threeAndZero = (quarter1 == 3 && quarter2 == 0) || (quarter1 == 0 && quarter2 == 3)

    return if (threeAndZero) {
        0
    } else {
        min(quarter1, quarter2)
    }
}

private fun getStartPointOfQuarter(quarter: Int): Point {
    return when (quarter) {
        0 -> Point(0.0, -1.0)
        1 -> Point(1.0, 0.0)
        2 -> Point(0.0, 1.0)
        3 -> Point(-1.0, 0.0)
        else -> Point()
    }
}

// Получение угла между сторонами треугольника b и c
fun getAngleOfTriangle(a: Double, b: Double, c: Double): Double {
    val value1 = a.pow(2) + b.pow(2) - c.pow(2)
    val value2 = 2 * a * b

    if (value2 != 0.0) {
        return acos(value1 / value2)
    }

    return 0.0
}

fun getAngleBetweenPoints(centerPoint: Point, point1: Point, point2: Point): Double {
    val a = getDistanceBetweenPoints(centerPoint, point1)
    val b = getDistanceBetweenPoints(centerPoint, point2)
    val c = getDistanceBetweenPoints(point1, point2)

    return getAngleOfTriangle(a, b, c)
}

fun isNearlyEqual(value1: Double, value2: Double): Boolean {
    val product1 = value1 * QUADRILLION
    val product2 = value2 * QUADRILLION
    return product1.toLong() == product2.toLong()
}

fun isZero(value: Double) = isNearlyEqual(value, 0.0)

fun getNextPos(currentX: Double, currentY: Double, destX: Double, destY: Double, step: Double): Point {
    val offsetX = currentX - destX
    val offsetY = currentY - destY

    val h = getHypotenuse(offsetX, offsetY)
    val ratio = h / step

    var x = if (isZero(h)) {
        destX
    } else {
        currentX - offsetX / ratio
    }

    var y = if (isZero(h)) {
        destY
    } else {
        currentY - offsetY / ratio
    }

    if (!isNearlyEqual(x, destX)) {
        val direction = destX >= currentX

        if (direction) {
            if (x > destX) {
                x = destX
            }
        } else {
            if (x < destX) {
                x = destX
            }
        }
    }

    if (!isNearlyEqual(y, destY)) {
        val direction = destY >= currentY

        if (direction) {
            if (y > destY) {
                y = destY
            }
        } else {
            if (y < destY) {
                y = destY
            }
        }
    }

    return Point(x, y)
}

fun isEven(value: Int) = value % 2 == 0

// Если второй отрезок, присоединяется к первому по часовой стрелке
fun isClockwiseLine(point1: Point, point2: Point, point3: Point): Boolean {
    val p1 = Point(point1)
    val p2 = Point(point2)
    val p3 = Point(point3)

    val offsetX = -p2.x
    val offsetY = -p2.y

    // Выполняем смещение так, чтобы центральная точка находилась в координатах (0.0; 0.0)
    p1.offset(offsetX, offsetY)
    p2.offset(offsetX, offsetY)
    p3.offset(offsetX, offsetY)

    val quarter = getQuarterOfPoint(p1)
    val startPoint = getStartPointOfQuarter(quarter)
    val angle = -getAngleBetweenPoints(p2, startPoint, p1)

    val rotatedPoint = rotatePoint(p3, angle)

    return isClockwisePoint(rotatedPoint, quarter)
}

fun isClockwisePoint(point: Point, quarter: Int): Boolean {
    return when (quarter) {
        0 -> point.x <= 0
        1 -> point.y <= 0
        2 -> point.x >= 0
        3 -> point.y >= 0
        else -> false
    }
}

fun isConcavePoint(point1: Point, point2: Point, point3: Point) = !isClockwiseLine(point1, point2, point3)

fun getMiddlePoint(prevPoint: Point, point: Point, nextPoint: Point): Point {
    val distance1 = getDistanceBetweenPoints(prevPoint, point)
    val distance2 = getDistanceBetweenPoints(point, nextPoint)
    val sum = distance1 + distance2

    val ratio = if (sum == 0.0) 1.0 else distance1 / sum

    return getPointOnLine(prevPoint, nextPoint, ratio);
}

fun getPointOnLine(point1: Point, point2: Point, ratio: Double): Point {
    val offsetX = point1.x
    val offsetY = point1.y

    val p = Point(point2)
    p.offset(-offsetX, -offsetY)

    val point = Point(p.x * ratio, p.y * ratio)
    point.offset(offsetX, offsetY)

    return point
}

// Продление отрезка на определенное расстояние
fun extendLine(point1: Point, point2: Point, distance: Double): Point {
    val offsetX = point1.x
    val offsetY = point1.y

    val p1 = Point()

    val p2 = Point(point2)
    p2.offset(-offsetX, -offsetY)

    val d = getDistanceBetweenPoints(p1, p2)

    val coeff = if (d == 0.0) 1.0 else 1.0 + distance / d

    val point = Point(p2.x * coeff, p2.y * coeff)
    point.offset(offsetX, offsetY)

    return point
}

// Определяет точку пересечения двух линий, продленных до пересечения
fun getCrossPointOfLines(point1: Point, point2: Point, point3: Point, point4: Point): Point? {
    val a1 = getAOfLine(point1, point2)
    val b1 = getBOfLine(point1, point2)
    val c1 = getCOfLine(point1, point2)

    val a2 = getAOfLine(point3, point4)
    val b2 = getBOfLine(point3, point4)
    val c2 = getCOfLine(point3, point4)

    val ab = a1 * b2 - a2 * b1

    if (abs(ab) > PARALLEL_PREC) {
        val x = -(c1 * b2 - c2 * b1) / ab
        val y = -(a1 * c2 - a2 * c1) / ab

        val point = Point(x, y)

        if (pointInLine(point, point1, point2) && pointInLine(point, point3, point4)) {
            return point
        }
    }

    return null
}

private fun getAOfLine(point1: Point, point2: Point) = point2.y - point1.y
private fun getBOfLine(point1: Point, point2: Point) = point1.x - point2.x
private fun getCOfLine(point1: Point, point2: Point) = point1.y * point2.x - point1.x * point2.y

fun pointInLine(point: Point, point1: Point, point2: Point): Boolean {
    val left = min(point1.x, point2.x)
    val right = max(point1.x, point2.x)
    val top = min(point1.y, point2.y)
    val bottom = max(point1.y, point2.y)
    return point.x >= left && point.x <= right && point.y >= top && point.y <= bottom
}
