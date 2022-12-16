package com.codersguidebook.supernova.ui.currentlyPlaying

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentWithRecyclerViewBinding
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_TYPE
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_URI
import com.codersguidebook.supernova.recyclerview.BaseRecyclerViewFragment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.FileNotFoundException
import java.io.IOException

class CustomAnimationFragment : BaseRecyclerViewFragment() {

    override var _binding: ViewBinding? = null
        get() = field as FragmentWithRecyclerViewBinding?
    override val binding: FragmentWithRecyclerViewBinding
        get() = _binding!! as FragmentWithRecyclerViewBinding
    private var position: Int? = null
    override lateinit var adapter: AnimationAdapter
    private lateinit var sharedPreferences: SharedPreferences

    private val registerResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            try {
                result.data?.data?.let { selectedImageUri ->
                    if (position == null) adapter.setImage(selectedImageUri.toString())
                    else adapter.setImage(selectedImageUri.toString(), position!!)
                    sharedPreferences.edit().apply {
                        putString(ANIMATION_TYPE, getString(R.string.custom_image))
                        apply()
                    }
                    saveChanges()
                }
            } catch (_: FileNotFoundException) {
            } catch (_: IOException) { }
        }
        position = null
    }

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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.adapter = adapter

        sharedPreferences.getString(ANIMATION_URI, null)?.let {
            val listType = object : TypeToken<MutableList<String>>() {}.type
            val imageStrings: List<String> = Gson().fromJson(it, listType)
            adapter.imageStrings.addAll(imageStrings)
            adapter.notifyItemRangeInserted(0, imageStrings.size)
        }
    }

    /**
     * Prompt the user to select an image from their device.
     *
     * @param position - The position in the adapter at which the image should be displayed.
     * Default = Null (the image will be displayed at the next available position)
     */
    fun getPhoto(position: Int? = null) {
        this.position = position
        registerResult.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI))
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
            if (adapter.imageStrings.isEmpty()) putString(ANIMATION_URI, null)
            else {
                val imagesJson = GsonBuilder().setPrettyPrinting().create()
                    .toJson(adapter.imageStrings)
                putString(ANIMATION_URI, imagesJson)
            }
            apply()
        }
    }

    override fun initialiseAdapter() {
        adapter = AnimationAdapter(this)
    }

    override fun requestNewData() {
        TODO("Not yet implemented")
    }
}