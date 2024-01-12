package com.dikiyserge.glassmosaic.data

import android.graphics.Color.*

const val EMPTY_COLOR = -1
const val ALFA_MAX = 1f

private const val COLOR_MAX_VALUE = 255f

fun r(color: Int) = red(color) / COLOR_MAX_VALUE
fun g(color: Int) = green(color) / COLOR_MAX_VALUE
fun b(color: Int) = blue(color) / COLOR_MAX_VALUE
