package com.codersguidebook.supernova.recyclerview

import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.entities.Song

abstract class SongAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val songs = mutableListOf<Song>()
}