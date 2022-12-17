package com.codersguidebook.supernova.ui.currentlyPlaying

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.ImageDecoder.createSource
import android.graphics.ImageDecoder.decodeBitmap
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
import com.codersguidebook.supernova.params.SharedPreferencesConstants.Companion.CUSTOM_ANIMATION_IMAGE_IDS
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
    private var imageIdToUse = "0"
    override lateinit var adapter: AnimationAdapter
    private lateinit var sharedPreferences: SharedPreferences

    private val registerResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            try {
                result.data?.data?.let { uri ->
                    val bitmap = decodeBitmap(createSource(requireActivity().contentResolver, uri))
                    mainActivity.saveImageByResourceId("customAnimation", bitmap, imageIdToUse)
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

        return super.onCreateView(inflater, container, savedInstanceState)
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
    fun getPhoto(imageId: Int) {
        this.imageIdToUse = imageId.toString()
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
    fun showPopup(view: View, imageId: Int) {
        PopupMenu(requireContext(), view).apply {
            inflate(R.menu.animation_menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_change -> getPhoto(imageId)
                    else -> adapter.removeItemByImageId(imageId.toString())
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

    override fun initialiseAdapter() {
        adapter = AnimationAdapter(this, mainActivity)
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