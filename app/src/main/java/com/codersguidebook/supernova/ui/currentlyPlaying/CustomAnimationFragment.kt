package com.codersguidebook.supernova.ui.currentlyPlaying

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.ImageDecoder.createSource
import android.graphics.ImageDecoder.decodeBitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.ImageView
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.signature.ObjectKey
import com.codersguidebook.supernova.R
import com.codersguidebook.supernova.databinding.FragmentWithRecyclerViewBinding
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.ANIMATION_TYPE
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.CUSTOM_ANIMATION_IMAGE_IDS
import com.codersguidebook.supernova.recyclerview.BaseRecyclerViewFragment
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class CustomAnimationFragment : BaseRecyclerViewFragment() {

    override var _binding: ViewBinding? = null
        get() = field as FragmentWithRecyclerViewBinding?
    override val binding: FragmentWithRecyclerViewBinding
        get() = _binding!! as FragmentWithRecyclerViewBinding
    private var imageIdToUse = "0"
    override lateinit var adapter: AnimationAdapter
    private lateinit var sharedPreferences: SharedPreferences

    private val registerResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            try {
                result.data?.data?.let { uri ->
                    val bitmap = decodeBitmap(createSource(requireActivity().contentResolver, uri))
                    saveImageByResourceId(bitmap, imageIdToUse)
                    adapter.loadImageId(imageIdToUse)
                    sharedPreferences.edit().apply {
                        putString(ANIMATION_TYPE, getString(R.string.custom_image))
                        apply()
                    }
                    saveCustomAnimationImageIds()
                }
            } catch (_: FileNotFoundException) {
            } catch (_: IOException) { }
        }
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

        requestNewData()
    }

    /**
     * Prompt the user to select an image from their device.
     *
     * @param imageId - The ID that the image should have.
     */
    fun getPhoto(imageId: String) {
        this.imageIdToUse = imageId
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

    /**
     * Show a popup options menu when the user selects and image.
     *
     * @param view - The ImageView that the popup menu should appear over.
     * @param imageId - The ID of the selected image.
     */
    fun showPopup(view: View, imageId: String) {
        PopupMenu(requireContext(), view).apply {
            inflate(R.menu.animation_menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_change -> getPhoto(imageId)
                    else -> adapter.removeItemByImageId(imageId)
                }
                true
            }
            show()
        }
    }

    /** Convert the list of custom animation image IDs to a JSON String and save them on a device */
    fun saveCustomAnimationImageIds() {
        sharedPreferences.edit().apply {
            if (adapter.customAnimationImageIds.isEmpty()) putString(CUSTOM_ANIMATION_IMAGE_IDS, null)
            else {
                val imagesJson = GsonBuilder().create().toJson(adapter.customAnimationImageIds)
                putString(CUSTOM_ANIMATION_IMAGE_IDS, imagesJson)
            }
            apply()
        }
    }

    /**
     * Create a File object for the image file associated with a given resource ID
     * and save the image to a target directory.
     *
     * @param image - A Bitmap representation of the image to be saved.
     * @param resourceId - The ID of the resource that an image should be loaded for.
     */
    // TODO: Ultimately image saving resource operations should be moved to a utils class that references application context
    //      This is also needed for operations currently handled by MainActivity
    private fun saveImageByResourceId(image: Bitmap, resourceId: String) {
        val directory = ContextWrapper(requireActivity().application).getDir("customAnimation", Context.MODE_PRIVATE)
        val path = File(directory, "$resourceId.jpg")
        FileOutputStream(path).use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    // TODO: Ultimately image rendering operations should be moved to a utils class that references application context
    //      This is also needed for operations currently handled by MainActivity
    /**
     * Create a File object for the image file associated with a given resource ID
     * and load the image into a user interface View.
     *
     * @param resourceId - The ID of the resource that an image should be loaded for.
     * @param view - The user interface View that the artwork should be displayed in.
     */
    fun loadImage(resourceId: String?, view: ImageView) {
        var file: File? = null
        if (resourceId != null) {
            val directory = ContextWrapper(requireActivity().application)
                .getDir("customAnimation", Context.MODE_PRIVATE)
            file = File(directory, "$resourceId.jpg")
        }
        Glide.with(this)
            .load(file ?: R.drawable.no_album_artwork)
            .transition(DrawableTransitionOptions.withCrossFade())
            .centerCrop()
            .signature(ObjectKey(file?.path + file?.lastModified()))
            .override(600, 600)
            .error(R.drawable.no_album_artwork)
            .into(view)
    }

    override fun initialiseAdapter() {
        adapter = AnimationAdapter(this)
    }

    override fun requestNewData() {
        if (adapter.customAnimationImageIds.isNotEmpty()) {
            val numberOfItemsToRemove = adapter.customAnimationImageIds.size
            adapter.customAnimationImageIds.clear()
            adapter.notifyItemRangeRemoved(0, numberOfItemsToRemove)
        }
        sharedPreferences.getString(CUSTOM_ANIMATION_IMAGE_IDS, null)?.let {
            val listType = object : TypeToken<List<String>>() {}.type
            val imageIds: List<String> = Gson().fromJson(it, listType)
            adapter.customAnimationImageIds.addAll(imageIds)
            adapter.notifyItemRangeInserted(0, imageIds.size)
        }
    }
}