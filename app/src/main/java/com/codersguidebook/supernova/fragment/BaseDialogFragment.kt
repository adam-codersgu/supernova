package com.codersguidebook.supernova.fragment

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.codersguidebook.supernova.MainActivity
import com.codersguidebook.supernova.MusicLibraryViewModel
import com.codersguidebook.supernova.databinding.OptionsLayoutBinding

@Suppress("PropertyName")
abstract class BaseDialogFragment: DialogFragment() {

    var _binding: OptionsLayoutBinding? = null
    val binding get() = _binding!!
    lateinit var mainActivity: MainActivity
    lateinit var musicLibraryViewModel: MusicLibraryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = activity as MainActivity
        musicLibraryViewModel = ViewModelProvider(mainActivity)[MusicLibraryViewModel::class.java]
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