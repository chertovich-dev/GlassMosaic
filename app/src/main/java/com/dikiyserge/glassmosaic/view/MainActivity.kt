package com.dikiyserge.glassmosaic.view

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.dikiyserge.glassmosaic.viewmodel.MainViewModel
import com.dikiyserge.glassmosaic.R
import com.dikiyserge.glassmosaic.TestActivity
import com.dikiyserge.glassmosaic.data.Element
import com.dikiyserge.glassmosaic.data.Mosaic
import com.dikiyserge.glassmosaic.data.Triangle
import com.dikiyserge.glassmosaic.databinding.ActivityMainBinding
import com.dikiyserge.glassmosaic.getParcelableData
import com.dikiyserge.glassmosaic.model.MainRepository
import com.dikiyserge.glassmosaic.viewmodel.MainToMosaicNav
import com.dikiyserge.glassmosaic.viewmodel.MainViewModelFactory
import com.dikiyserge.glassmosaic.viewmodel.Nav
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async

private const val TAG = "_mosaic"

const val KEY_MOSAIC = "mosaic"

fun log(text: String, tag: String = TAG) = Log.i(tag, text)

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
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

        val navController = findNavController(R.id.nav_host_fragment_content_main)

        viewModel.navLiveEvent.observe(this) { nav ->
            navigate(nav)
        }
    }
}