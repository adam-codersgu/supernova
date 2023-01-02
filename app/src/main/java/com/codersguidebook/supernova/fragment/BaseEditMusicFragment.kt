package com.codersguidebook.supernova.fragment

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import com.codersguidebook.supernova.R
import java.io.FileNotFoundException
import java.io.IOException

abstract class BaseEditMusicFragment: BaseFragment() {

    var newArtwork: Bitmap? = null
    private val registerResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            try {
                result.data?.data?.let { uri ->
                    newArtwork = ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(requireActivity().contentResolver, uri)
                    )
                    furtherUriProcessing(uri)
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
        setupMenu()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    /** Configure the menu items and responses for the fragment. */
    private fun setupMenu() {
        (requireActivity() as MenuHost).addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.search)?.isVisible = false
                menu.findItem(R.id.save)?.isVisible = true
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) { }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return menuItemSelected(menuItem)
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    /**
     * Handle fragment-specific responses to menu item selections.
     *
     * @param menuItem The selected MenuItem option.
     * @return A Boolean indicating whether the method sufficiently handled the item selection. A value
     * of false will propagate the item selection further, which is necessary to handle actions such as
     * back navigation and other conventional application processes.
     */
    abstract fun menuItemSelected(menuItem: MenuItem): Boolean

    /** Prompt the user to select an image */
    fun getImage() = registerResult.launch(Intent(Intent.ACTION_PICK,
        MediaStore.Images.Media.INTERNAL_CONTENT_URI))

    /**
     * Optional method that allows further fragment-specific processing of the URI that is returned
     * after the user selects an image using the ActivityResultLauncher.
     *
     * @param uri The uri of the selected image.
     */
    open fun furtherUriProcessing(uri: Uri) {}
}