package com.codersguidebook.supernova.ui.library

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.ViewPagerAdapter
import com.codersguidebook.supernova.databinding.FragmentLibraryBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class LibraryFragment : Fragment() {
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter
        binding.viewPager.currentItem = viewPagerPosition ?: 0

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

        val namesArray = arrayOf("Playlists","Artists","Albums","Songs")
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = namesArray[position]
        }.attach()
        binding.tabLayout.tabGravity = TabLayout.GRAVITY_FILL
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
