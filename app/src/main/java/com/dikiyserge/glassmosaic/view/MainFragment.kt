package com.dikiyserge.glassmosaic.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.dikiyserge.glassmosaic.data.Element
import com.dikiyserge.glassmosaic.data.Point
import com.dikiyserge.glassmosaic.viewmodel.MainViewModel
import com.dikiyserge.glassmosaic.databinding.FragmentMainBinding
import com.dikiyserge.glassmosaic.model.MainRepository
import com.dikiyserge.glassmosaic.view.adapters.MosaicFragmentStateAdapter
import com.dikiyserge.glassmosaic.viewmodel.MainViewModelFactory
import com.google.android.material.tabs.TabLayoutMediator

private const val GROUP = "group"

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels {
        MainViewModelFactory(MainRepository(requireActivity().applicationContext))
    }

    private fun getGroupName(id: Int): String {
        val name = GROUP + id
        val packageName = requireActivity().packageName
        val resId = resources.getIdentifier(name, "string", packageName)
        return getString(resId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.structureLiveData.observe(viewLifecycleOwner)  {
            val adapter = MosaicFragmentStateAdapter(this, it)
            binding.viewPager.adapter = adapter
            binding.viewPager.isUserInputEnabled = false

            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tab.text = getGroupName(it.groups[position].id)
            }.attach()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}