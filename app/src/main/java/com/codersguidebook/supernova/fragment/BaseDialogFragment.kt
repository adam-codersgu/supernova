package com.codersguidebook.supernova.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.MusicLibraryViewModel

@Suppress("PropertyName")
abstract class BaseDialogFragment: DialogFragment() {

    abstract var _binding: ViewBinding?
    abstract val binding: ViewBinding
    lateinit var inflater: LayoutInflater
    lateinit var mainActivity: MainActivity
    lateinit var musicLibraryViewModel: MusicLibraryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = activity as MainActivity
        musicLibraryViewModel = ViewModelProvider(mainActivity)[MusicLibraryViewModel::class.java]
        inflater = mainActivity.layoutInflater
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(mainActivity)
            .setView(binding.root)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}