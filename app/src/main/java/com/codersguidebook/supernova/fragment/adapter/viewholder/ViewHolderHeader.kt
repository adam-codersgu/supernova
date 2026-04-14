package com.codersguidebook.supernova.fragment.adapter.viewholder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.R

open class ViewHolderHeader(itemView: View) : RecyclerView.ViewHolder(itemView) {

    internal var mArtwork: ImageView = itemView.findViewById(R.id.artwork)
    internal var mTitle: TextView = itemView.findViewById(R.id.title)
    internal var mSubtitle: TextView = itemView.findViewById(R.id.subtitle)
    internal var mSubtitle2: TextView = itemView.findViewById(R.id.subtitle2)
}