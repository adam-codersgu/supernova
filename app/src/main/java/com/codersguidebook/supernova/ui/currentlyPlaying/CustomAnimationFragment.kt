package com.codersguidebook.supernova.ui.currentlyPlaying

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentWithRecyclerViewBinding
import com.codersguidebook.supernova.entities.Song
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_TYPE
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_URI
import com.codersguidebook.supernova.recyclerview.RecyclerViewFragment
import com.codersguidebook.supernova.recyclerview.adapter.AlbumsAdapter
import com.codersguidebook.supernova.ui.album.AlbumFragmentDirections
import com.codersguidebook.supernova.ui.artists.ArtistsFragmentDirections
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.FileNotFoundException
import java.io.IOException

class CustomAnimationFragment : RecyclerViewFragment() {

    var imageStrings = mutableListOf<String>()
    override lateinit var adapter: AnimationAdapter
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWithRecyclerViewBinding.inflate(inflater, container, false)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        setupMenu()

        binding.root.layoutManager = GridLayoutManager(context, 3)
        binding.root.itemAnimator = DefaultItemAnimator()

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.adapter = adapter

        sharedPreferences.getString(ANIMATION_URI, null)?.let {
            val listType = object : TypeToken<MutableList<String>>() {}.type
            imageStrings = Gson().fromJson(it, listType)
            adapter.imageStringList = imageStrings
            adapter.notifyItemRangeInserted(0, imageStrings.size)
        }
    }

    fun getPhoto(position: Int) {
        startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), position)
    }

    override fun onActivityResult(reqCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            try {
                val selectedImageUri = data!!.data
                if (selectedImageUri != null) {
                    val uriString = selectedImageUri.toString()

                    if (reqCode >= imageStrings.size) {
                        imageStrings.add(reqCode, uriString)
                        animationAdapter.imageStringList = imageStrings
                        animationAdapter.notifyItemInserted(reqCode)
                        if (animationAdapter.imageStringList.size == 6) animationAdapter.notifyItemRangeChanged(reqCode, 6)
                        else animationAdapter.notifyItemRangeChanged(reqCode, animationAdapter.imageStringList.size + 1)
                    } else {
                        imageStrings.removeAt(reqCode)
                        imageStrings.add(reqCode, uriString)
                        animationAdapter.imageStringList = imageStrings
                        animationAdapter.notifyItemChanged(reqCode)
                    }
                    val editor = sharedPreferences.edit()
                    editor.putString(ANIMATION_TYPE, getString(R.string.custom_image))
                    editor.apply()
                    saveChanges()
                }
            } catch (_: FileNotFoundException) { }
            catch (_: IOException) {  }
        }

        super.onActivityResult(reqCode, resultCode, data)
    }

    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.search)?.isVisible = false
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) { }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return findNavController().popBackStack()
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    fun showPopup(view: View, position: Int) {
        PopupMenu(requireContext(), view).apply {
            inflate(R.menu.animation_menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_change -> getPhoto(position)
                    else -> adapter.removeItem(position)
                }
                true
            }
            show()
        }
    }

    fun saveChanges() {
        sharedPreferences.edit().apply {
            if (imageStrings.isEmpty()) putString(ANIMATION_URI, null)
            else {
                val gPretty = GsonBuilder().setPrettyPrinting().create().toJson(imageStrings)
                putString(ANIMATION_URI, gPretty)
            }
            apply()
        }
    }

    override fun initialiseAdapter() {
        adapter = AnimationAdapter(this)
    }
}