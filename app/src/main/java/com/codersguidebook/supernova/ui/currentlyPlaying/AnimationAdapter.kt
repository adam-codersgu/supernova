package com.codersguidebook.supernova.ui.currentlyPlaying

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.utils.ImageHandlingHelper
import com.google.android.material.color.MaterialColors

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
                val context = fragment.requireActivity()
                val onSurfaceColour = MaterialColors.getColor(context, R.attr.colorOnSurface, Color.LTGRAY)
                // 60% Alpha
                val onSurfaceColour60 = MaterialColors.compositeARGBWithAlpha(onSurfaceColour, 153)
                val cameraDrawable = ContextCompat.getDrawable(context, R.drawable.ic_camera)
                    ?.setTint(onSurfaceColour60)

                Glide.with(fragment)
                    .load(cameraDrawable)
                    .into(holder.itemView as ImageView)

                holder.itemView.setOnClickListener {
                    if (customAnimationImageIds.size < 6) {
                        var imageIdToUse = 6
                        for (i in 1..6) {
                            if (!customAnimationImageIds.contains(i.toString())) {
                                imageIdToUse = i
                                break
                            }
                        }
                        fragment.getPhoto(imageIdToUse.toString())
                    } else Toast.makeText(fragment.context,
                        fragment.getString(R.string.error_custom_animation_image_limit_reached),
                        Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                val current = customAnimationImageIds[position]

                holder.itemView.setOnClickListener {
                    fragment.showPopup(it, current)
                }

                ImageHandlingHelper.loadImageByCustomAnimationImageId(
                    fragment.requireActivity().application, current, holder.itemView as ImageView
                )
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