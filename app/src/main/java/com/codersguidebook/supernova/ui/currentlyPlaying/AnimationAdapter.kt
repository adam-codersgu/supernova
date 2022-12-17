package com.codersguidebook.supernova.ui.currentlyPlaying

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.codersguidebook.supernova.R

class AnimationAdapter(private val fragment: CustomAnimationFragment): RecyclerView.Adapter<ViewHolder>() {

    val customAnimationImageIds = mutableListOf<String>()

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
            customAnimationImageIds.size < 6 && position == customAnimationImageIds.size -> {
                Glide.with(fragment)
                    .load(R.drawable.ic_photo)
                    .into(holder.itemView as ImageView)

                holder.itemView.setOnClickListener {
                    if (customAnimationImageIds.size < 6) {
                        fragment.getPhoto(customAnimationImageIds.size)
                    } else Toast.makeText(fragment.context,
                        fragment.getString(R.string.error_custom_animation_image_limit_reached),
                        Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                val current = customAnimationImageIds[position]

                holder.itemView.setOnClickListener {
                    fragment.showPopup(it, position)
                }

                fragment.loadImage(current, holder.itemView as ImageView)
            }
        }
    }

    /**
     * Remove a given image from the adapter based on its ID.
     *
     * @param imageId - The ID of the image.
     */
    fun removeItemByImageId(imageId: String) {
        val indexOfImage =  customAnimationImageIds.indexOfFirst { it == imageId }
        if (indexOfImage == -1) return
        customAnimationImageIds.removeAt(indexOfImage)
        notifyItemRemoved(indexOfImage)
        notifyItemChanged(customAnimationImageIds.size)
        fragment.saveCustomAnimationImageIds()
    }

    /**
     * Load an image into the adapter based on its ID. If the ID already exists in the adapter, then
     * that image will be replaced. Otherwise, a new image will be added.
     *
     * @param imageId - The ID of the image to be displayed.
     */
    fun loadImageId(imageId: String) {
        val indexOfImage =  customAnimationImageIds.indexOfFirst { it == imageId }
        if (indexOfImage == -1) {
            customAnimationImageIds.add(0, imageId)
            notifyItemInserted(0)
            if (customAnimationImageIds.size >= 6) {
                // If there are six images then remove the option to add new ones
                notifyItemRemoved(7)
            }
        } else {
            customAnimationImageIds[indexOfImage] = imageId
            notifyItemChanged(indexOfImage)
        }
    }

    override fun getItemCount() = if (customAnimationImageIds.size < 6) customAnimationImageIds.size + 1
    else 6
}