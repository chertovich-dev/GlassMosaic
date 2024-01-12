package com.dikiyserge.glassmosaic.view

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.activity.viewModels
import com.dikiyserge.glassmosaic.viewmodel.MainViewModel
import com.dikiyserge.glassmosaic.R
import com.dikiyserge.glassmosaic.databinding.ActivityMainBinding
import com.dikiyserge.glassmosaic.model.MainRepository
import com.dikiyserge.glassmosaic.viewmodel.MainToMosaicNav
import com.dikiyserge.glassmosaic.viewmodel.MainViewModelFactory
import com.dikiyserge.glassmosaic.viewmodel.Nav

private const val TAG = "_mosaic"

const val KEY_MOSAIC = "mosaic"

fun log(text: String, tag: String = TAG) = Log.i(tag, text)

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels { MainViewModelFactory(MainRepository(applicationContext)) }

    private fun navigate(nav: Nav) {
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        val bundle = Bundle()

        when (nav) {
            is MainToMosaicNav -> {
                bundle.putParcelable(KEY_MOSAIC, nav.mosaic)
            }
        }

        navController.navigate(nav.action, bundle)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.navLiveEvent.observe(this) { nav ->
            navigate(nav)
        }
    }
}