package com.dikiyserge.glassmosaic.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dikiyserge.glassmosaic.R
import com.dikiyserge.glassmosaic.data.Mosaic
import com.dikiyserge.glassmosaic.data.MosaicState
import com.dikiyserge.glassmosaic.data.struct.Group
import com.dikiyserge.glassmosaic.data.struct.MosaicFile
import com.dikiyserge.glassmosaic.databinding.FragmentGroupBinding
import com.dikiyserge.glassmosaic.databinding.FragmentMainBinding
import com.dikiyserge.glassmosaic.model.MainRepository
import com.dikiyserge.glassmosaic.view.mosaicview.MosaicView
import com.dikiyserge.glassmosaic.view.mosaicview.OnMosaicViewListListener
import com.dikiyserge.glassmosaic.viewmodel.MainViewModel
import com.dikiyserge.glassmosaic.viewmodel.MainViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val KEY_GROUP_INDEX = "groupIndex"

class GroupFragment : Fragment(), OnMosaicViewListListener {
    private var _binding: FragmentGroupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(MainRepository(requireActivity().applicationContext))
    }

    private var groupIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            groupIndex = it.getInt(KEY_GROUP_INDEX)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.structureLiveData.observe(viewLifecycleOwner) { structure ->
            val mosaicView = view.findViewById<MosaicView>(R.id.mosaicView)
            mosaicView.setOnMosaicViewListListener(this)

            val group = structure.groups[groupIndex]

            mosaicView.setMosaicFiles(group.mosaicFiles)

            val mosaicsLiveData = viewModel.mosaicsLiveData[groupIndex]

            mosaicsLiveData.observe(viewLifecycleOwner) { mosaicList ->
                mosaicView.setMosaicList(mosaicList)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.mosaicView.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.mosaicView.onResume()
    }

    companion object {
        @JvmStatic
        fun newInstance(groupIndex: Int) = GroupFragment().apply {
                arguments = Bundle().apply {
                    putInt(KEY_GROUP_INDEX, groupIndex)
                }
            }
    }

    /** OnMosaicViewListListener */
    override fun onLoadMosaic(mosaicFile: MosaicFile) {
        viewModel.loadMosaic(groupIndex, mosaicFile)
    }

    override fun onSelectMosaic(mosaic: Mosaic) {
        lifecycleScope.launch {
            // Если первое открытие мозаики, то сохраняем ее в БД, чтобы получить id
            if (mosaic.state == MosaicState.NEW) {
                viewModel.saveMosaicAndGetIds(mosaic)
            }

            viewModel.showMosaic(mosaic)
        }
    }

}