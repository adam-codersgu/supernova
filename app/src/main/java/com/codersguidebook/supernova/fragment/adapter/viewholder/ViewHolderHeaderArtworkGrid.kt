package com.codersguidebook.supernova.fragment.adapter.viewholder

import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import com.codersguidebook.supernova.R

class ViewHolderHeaderArtworkGrid(itemView: View) : ViewHolderHeader(itemView) {

    internal var mArtworkGrid: GridLayout = itemView.findViewById(R.id.imageGrid)
    internal var mArtwork1: ImageView = itemView.findViewById(R.id.artwork1)
    internal var mArtwork2: ImageView = itemView.findViewById(R.id.artwork2)
    internal var mArtwork3: ImageView = itemView.findViewById(R.id.artwork3)
    internal var mArtwork4: ImageView = itemView.findViewById(R.id.artwork4)
}