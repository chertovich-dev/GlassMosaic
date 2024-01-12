package com.dikiyserge.glassmosaic.view.adapters

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dikiyserge.glassmosaic.data.struct.Structure
import com.dikiyserge.glassmosaic.view.GroupFragment
import com.dikiyserge.glassmosaic.view.MosaicFragment

class MosaicFragmentStateAdapter(fragment: Fragment, private val structure: Structure) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int {
        return structure.groups.size
    }

    override fun createFragment(position: Int): Fragment {
        return GroupFragment.newInstance(position)
    }
}