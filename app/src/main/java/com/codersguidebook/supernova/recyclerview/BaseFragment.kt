package com.codersguidebook.supernova.recyclerview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.codersguidebook.supernova.MainActivity

@Suppress("PropertyName")
abstract class BaseFragment: Fragment() {

    abstract var _binding: ViewBinding?
    val binding get() = _binding!!
    lateinit var mainActivity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        mainActivity = activity as MainActivity
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}