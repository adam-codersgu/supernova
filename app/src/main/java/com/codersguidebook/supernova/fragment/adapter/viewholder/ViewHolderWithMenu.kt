package com.codersguidebook.supernova.fragment.adapter.viewholder

import android.view.View
import android.widget.ImageButton
import com.codersguidebook.supernova.R

abstract class ViewHolderWithMenu(itemView: View) : ViewHolder(itemView) {

    internal var mMenu: ImageButton = itemView.findViewById(R.id.menu)

    init {
        mMenu.setOnClickListener {
            openDialog()
        }
    }
}