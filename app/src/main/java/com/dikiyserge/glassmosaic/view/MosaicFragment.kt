package com.dikiyserge.glassmosaic.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.dikiyserge.glassmosaic.R
import com.dikiyserge.glassmosaic.data.Mosaic
import com.dikiyserge.glassmosaic.data.struct.MosaicFile
import com.dikiyserge.glassmosaic.databinding.FragmentMainBinding
import com.dikiyserge.glassmosaic.databinding.FragmentMosaicBinding
import com.dikiyserge.glassmosaic.getParcelableData
import com.dikiyserge.glassmosaic.model.MainRepository
import com.dikiyserge.glassmosaic.view.mosaicview.MosaicView
import com.dikiyserge.glassmosaic.view.mosaicview.OnMosaicViewListener
import com.dikiyserge.glassmosaic.viewmodel.MainViewModel
import com.dikiyserge.glassmosaic.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch

class MosaicFragment : Fragment(), OnMosaicViewListener {
    private var _binding: FragmentMosaicBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(MainRepository(requireActivity().applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMosaicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mosaicView = view.findViewById<MosaicView>(R.id.mosaicView)

        mosaicView.setOnMosaicViewListener(this)

        val bundle = arguments

        if (bundle != null && savedInstanceState == null) {
            val mosaic = bundle.getParcelableData<Mosaic>(KEY_MOSAIC)

            if (mosaic != null) {
                mosaicView.mosaic = mosaic
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mosaicView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mosaicView.onPause()
    }

    /** onMosaicViewListener */
    override fun onSaveMosaic(mosaic: Mosaic) {
        viewModel.saveMosaic(mosaic)
    }
}