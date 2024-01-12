package com.dikiyserge.glassmosaic

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dikiyserge.glassmosaic.data.Element
import com.dikiyserge.glassmosaic.data.Mosaic
import com.dikiyserge.glassmosaic.data.Point
import com.dikiyserge.glassmosaic.data.Triangle
import com.dikiyserge.glassmosaic.view.log

class TestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        val data = intent.getParcelableExtraData<Mosaic>("data")

        if (data != null) {
            log("data2 = ${data}")
        }
    }
}