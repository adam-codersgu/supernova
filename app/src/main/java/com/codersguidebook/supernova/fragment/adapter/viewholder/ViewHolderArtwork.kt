package com.codersguidebook.supernova.fragment.adapter.viewholder

import android.view.View
import android.widget.ImageView
import com.codersguidebook.supernova.R

abstract class ViewHolderArtwork(itemView: View) : ViewHolder(itemView) {

    internal var mArtwork: ImageView = itemView.findViewById(R.id.artwork)
}