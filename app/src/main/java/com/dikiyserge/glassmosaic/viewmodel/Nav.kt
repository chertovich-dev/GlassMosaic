package com.dikiyserge.glassmosaic.viewmodel

import com.dikiyserge.glassmosaic.R
import com.dikiyserge.glassmosaic.data.Mosaic

sealed class Nav(val action: Int)

class MainToMosaicNav(val mosaic: Mosaic) : Nav(R.id.action_MainFragment_to_MosaicFragment)