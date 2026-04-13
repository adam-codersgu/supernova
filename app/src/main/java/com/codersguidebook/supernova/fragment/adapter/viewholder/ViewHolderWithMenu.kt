package com.codersguidebook.supernova.fragment.adapter.viewholder

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.fragment.BaseDialogFragment

abstract class ViewHolderWithMenu(itemView: View) : RecyclerView.ViewHolder(itemView) {

    internal var mArtwork: ImageView = itemView.findViewById(R.id.artwork)
    internal var mTitle: TextView = itemView.findViewById(R.id.title)
    internal var mSubtitle: TextView = itemView.findViewById(R.id.subtitle)
    internal var mMenu: ImageButton = itemView.findViewById(R.id.menu)

    init {
        itemView.rootView.isClickable = true
        itemView.rootView.setOnClickListener {
            rootViewAction()
        }

        itemView.rootView.setOnLongClickListener{
            openDialog()
            return@setOnLongClickListener true
        }

        mMenu.setOnClickListener {
            openDialog()
        }
    }

    internal fun openDialog() {
        getActivity().openDialog(getOptionsDialog())
    }

    abstract fun getActivity(): MainActivity

    abstract fun rootViewAction()

    abstract fun getOptionsDialog(): BaseDialogFragment
}