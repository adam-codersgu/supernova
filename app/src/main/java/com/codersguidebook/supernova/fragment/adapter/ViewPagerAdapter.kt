package com.codersguidebook.supernova.fragment.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.codersguidebook.supernova.ui.albums.AlbumsFragment
import com.codersguidebook.supernova.ui.artists.ArtistsFragment
import com.codersguidebook.supernova.ui.playlists.PlaylistsFragment
import com.codersguidebook.supernova.ui.songs.SongsFragment

class ViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PlaylistsFragment()
            1 -> ArtistsFragment()
            2 -> AlbumsFragment()
            else -> SongsFragment()
        }
    }
}