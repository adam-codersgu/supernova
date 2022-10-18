package com.codersguidebook.supernova.ui.currentlyPlaying

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentWithRecyclerViewBinding
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_TYPE
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_URI
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.FileNotFoundException
import java.io.IOException

class CustomAnimationFragment : Fragment() {

    private var _binding: FragmentWithRecyclerViewBinding? = null
    private val binding get() = _binding!!
    var imageStrings = mutableListOf<String>()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var animationAdapter: AnimationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWithRecyclerViewBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        animationAdapter = AnimationAdapter(this)
        val layoutManager = GridLayoutManager(context, 3)
        binding.root.layoutManager = layoutManager
        binding.root.itemAnimator = DefaultItemAnimator()
        binding.root.adapter = animationAdapter

        val customDrawableString = sharedPreferences.getString(ANIMATION_URI, null)
        if (customDrawableString != null) {
            val listType = object : TypeToken<MutableList<String>>() {}.type
            imageStrings = Gson().fromJson(customDrawableString, listType)
            animationAdapter.imageStringList = imageStrings
            animationAdapter.notifyDataSetChanged()
        }

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val search: MenuItem? = menu.findItem(R.id.search)
        search?.let { it.isVisible = false }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = findNavController().popBackStack()

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

    fun showPopup(view: View, position: Int) {
        val popup = PopupMenu(requireContext(), view)
        popup.inflate(R.menu.animation_menu)

        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_change -> {
                    getPhoto(position)
                    true
                }
                else -> {
                    animationAdapter.removeItem(position)
                    true
                }
            }
        }

        popup.show()
    }

    fun saveChanges() {
        val editor = sharedPreferences.edit()
        if (imageStrings.isEmpty()) editor.putString(ANIMATION_URI, null)
        else {
            val gPretty = GsonBuilder().setPrettyPrinting().create().toJson(imageStrings)
            editor.putString(ANIMATION_URI, gPretty)
        }
        editor.apply()
    }
}