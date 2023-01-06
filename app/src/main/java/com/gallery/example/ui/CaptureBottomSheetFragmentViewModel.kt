package com.gallery.example.ui

import androidx.lifecycle.ViewModel
import com.gallery.core.GalleryProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class CaptureBottomSheetFragmentViewModel @Inject constructor(
    private val galleryProvider: GalleryProvider
) : ViewModel() {

}
