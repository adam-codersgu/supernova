package com.codersguidebook.supernova.ui.currentlyPlaying

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.codersguidebook.supernova.R
import java.io.FileNotFoundException
import java.lang.reflect.InvocationTargetException

class AnimationAdapter(private val fragment: CustomAnimationFragment):
    RecyclerView.Adapter<AnimationAdapter.AnimationViewHolder>() {

    var imageStringList = mutableListOf<String>()

    class AnimationViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        internal var mArtwork = itemView.findViewById<View>(R.id.smallSongArtwork) as ImageView
        internal var mPlaylistName = itemView.findViewById<View>(R.id.smallSongTitle) as TextView
        internal var mPlaylistSongCount = itemView.findViewById<View>(R.id.smallSongArtistOrCount) as TextView

        init {
            itemView.isClickable = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimationViewHolder {
        return AnimationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.small_recycler_grid_preview, parent, false))
    }

    override fun onBindViewHolder(holder: AnimationViewHolder, position: Int) {
        holder.mPlaylistName.isGone = true
        holder.mPlaylistSongCount.isGone = true

        when {
            imageStringList.size < 6 && position == imageStringList.size -> {
                Glide.with(fragment)
                    .load(R.drawable.ic_photo)
                    .into(holder.mArtwork)

                holder.itemView.setOnClickListener {
                    fragment.getPhoto(position)
                }
            }
            else -> {
                val current = imageStringList[position]

                holder.itemView.setOnClickListener {
                    fragment.showPopup(it, position)
                }

                try {
                    val uri = Uri.parse(current)
                    Glide.with(fragment)
                        .load(uri)
                        .centerCrop()
                        .into(holder.mArtwork)
                } catch (e: InvocationTargetException) {
                    removeItem(position)
                } catch (e: FileNotFoundException) {
                    removeItem(position)
                } catch (e: SecurityException) {
                    removeItem(position)
                }
            }
        }
    }

    fun removeItem(position: Int) {
        fragment.imageStrings.removeAt(position)
        fragment.saveChanges()
        imageStringList = fragment.imageStrings
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, imageStringList.size + 1)
    }

    override fun getItemCount() = if (imageStringList.size < 6) imageStringList.size + 1
    else 6
}