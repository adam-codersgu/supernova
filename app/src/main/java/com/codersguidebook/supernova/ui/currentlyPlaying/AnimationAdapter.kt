package com.codersguidebook.supernova.ui.currentlyPlaying

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.codersguidebook.supernova.R

class AnimationAdapter(private val fragment: CustomAnimationFragment):
    RecyclerView.Adapter<ViewHolder>() {

    val imageStrings = mutableListOf<String>()

    class ViewHolderAnimation(itemView: View) : ViewHolder(itemView) {
        init {
            itemView.isClickable = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderAnimation {
        return ViewHolderAnimation(LayoutInflater.from(parent.context)
            .inflate(R.layout.image_view, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when {
            imageStrings.size < 6 && position == imageStrings.size -> {
                Glide.with(fragment)
                    .load(R.drawable.ic_photo)
                    .into(holder.itemView as ImageView)

                holder.itemView.setOnClickListener {
                    fragment.getPhoto(position)
                }
            }
            else -> {
                val current = imageStrings[position]

                holder.itemView.setOnClickListener {
                    fragment.showPopup(it, position)
                }

                try {
                    val uri = Uri.parse(current)
                    Glide.with(fragment)
                        .load(uri)
                        .centerCrop()
                        .into(holder.itemView as ImageView)
                } catch (_: Exception) {
                    removeItem(position)
                }
            }
        }
    }

    fun removeItem(position: Int) {
        imageStrings.removeAt(position)
        notifyItemRemoved(position)
        notifyItemChanged(imageStrings.size + 1)
        fragment.saveChanges()
    }

    /**
     * Set the image that should be displayed at a given position in the adapter.
     *
     * @param uriString - A String representation of the image URI.
     * @param position - The RecyclerView position at which the image should be displayed.
     * Default value - The next available index in the imageStrings list
     */
    fun setImage(uriString: String, position: Int = imageStrings.size) {
        when {
            position < imageStrings.size -> {
                imageStrings[position] = uriString
                notifyItemChanged(position)
            }
            position >= 5 && imageStrings.size >= 6 -> {
                imageStrings[5] = uriString
                notifyItemChanged(5)
            }
            else -> {
                imageStrings.add(uriString)
                notifyItemInserted(imageStrings.size - 1)
                if (imageStrings.size >= 6) {
                    // If there are six images then remove the option to add new ones
                    notifyItemRemoved(7)
                }
            }
        }
    }

    override fun getItemCount() = if (imageStrings.size < 6) imageStrings.size + 1
    else 6
}