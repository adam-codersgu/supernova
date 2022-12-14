package com.codersguidebook.supernova.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.widget.ViewPager2
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.ViewPagerAdapter
import com.codersguidebook.supernova.databinding.FragmentLibraryBinding
import com.codersguidebook.supernova.recyclerview.BaseFragment
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class LibraryFragment : BaseFragment() {
    override var _binding: ViewBinding? = null
        get() = field as FragmentLibraryBinding?
    override val binding: FragmentLibraryBinding
        get() = _binding!! as FragmentLibraryBinding
    private var viewPagerPosition: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            val safeArgs = LibraryFragmentArgs.fromBundle(it)
            viewPagerPosition = safeArgs.position
        }

        _binding = FragmentLibraryBinding.inflate(inflater, container, false)

        binding.viewPager.adapter = ViewPagerAdapter(this)
        binding.viewPager.currentItem = viewPagerPosition ?: 0

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navView: NavigationView = requireActivity().findViewById(R.id.nav_view)
        val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> navView.setCheckedItem(R.id.nav_playlists)
                    1 -> navView.setCheckedItem(R.id.nav_artists)
                    2 -> navView.setCheckedItem(R.id.nav_albums)
                    3 -> navView.setCheckedItem(R.id.nav_songs)
                }
            }
        }
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback)

        val namesArray = arrayOf("Playlists", "Artists", "Albums", "Songs")
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = namesArray[position]
        }.attach()
        binding.tabLayout.tabGravity = TabLayout.GRAVITY_FILL
    }
}
