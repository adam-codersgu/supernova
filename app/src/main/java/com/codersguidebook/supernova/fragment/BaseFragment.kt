package com.codersguidebook.supernova.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.MainActivityFragments
import com.codersguidebook.supernova.MusicLibraryViewModel

@Suppress("PropertyName")
abstract class BaseFragment: Fragment() {

    abstract var _binding: ViewBinding?
    abstract val binding: ViewBinding

    protected val mainActivity: MainActivityFragments?
        get() = activity as? MainActivityFragments

    protected open val viewModelOwner: ViewModelStoreOwner
        get() = requireActivity()

    lateinit var musicLibraryViewModel: MusicLibraryViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        musicLibraryViewModel = ViewModelProvider(
            viewModelOwner,
            MusicLibraryViewModel.Factory
        )[MusicLibraryViewModel::class.java]

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Convenience method that returns a RecyclerView item animator which has animations for
     * item updates disabled.
     *
     * @return A DefaultItemAnimator instance with its supportsChangeAnimations attribute set to false.
     */
    fun getItemAnimatorWithNoChangeAnimation(): DefaultItemAnimator {
        return DefaultItemAnimator().apply {
            changeDuration = 0L
        }
    }
}