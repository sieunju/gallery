package com.gallery.example

import android.Manifest
import android.os.Bundle
import android.view.View
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import hmju.permissions.core.SPermissions

class MainRootFragment : Fragment(R.layout.f_main_root) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<CardView>(R.id.cvGallery).setOnClickListener {
            findNavController().navigate(R.id.action_root_to_gallery)
        }

        view.findViewById<CardView>(R.id.cvEditFlexible).setOnClickListener {
            findNavController().navigate(R.id.action_root_to_editFlexibleImage)
        }

        SPermissions(this)
            .requestPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)
            .build { b, strings -> }
    }
}