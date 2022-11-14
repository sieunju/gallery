package com.gallery.example.edit

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.gallery.edit.CropImageEditView
import com.gallery.example.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class CropEditFragment : Fragment(R.layout.f_crop_edit) {

    private lateinit var ivCropEdit: CropImageEditView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ivCropEdit = view.findViewById(R.id.ivEdit)
    }
}
