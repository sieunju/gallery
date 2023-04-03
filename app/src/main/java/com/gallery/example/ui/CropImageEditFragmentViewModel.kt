package com.gallery.example.ui

import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.core.GalleryProvider
import com.gallery.core_coroutines.deleteCacheDirectoryCo
import com.gallery.core_coroutines.fetchGalleryCo
import com.gallery.core_coroutines.pathToBitmapCo
import com.gallery.core_coroutines.saveGalleryPictureCo
import com.gallery.example.SingleLiveEvent
import com.gallery.ui.model.GalleryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class CropImageEditFragmentViewModel @Inject constructor(
    private val galleryProvider: GalleryProvider
) : ViewModel() {

    private val _cursor: MutableLiveData<Cursor> by lazy { MutableLiveData() }
    val cursor: LiveData<Cursor> get() = _cursor

    val startCameraOpenEvent: SingleLiveEvent<Intent> by lazy { SingleLiveEvent() }
    val startSnackBarEvent: SingleLiveEvent<String> by lazy { SingleLiveEvent() }
    val startViewHolderClickEvent: SingleLiveEvent<Int> by lazy { SingleLiveEvent() }

    private val _selectPhotoBitmap: MutableLiveData<Bitmap> by lazy { MutableLiveData() }
    val selectPhotoBitmap: LiveData<Bitmap> get() = _selectPhotoBitmap

    private val _startRotateEvent: MutableLiveData<Int> by lazy { MutableLiveData(0) }
    val startRotateEvent: LiveData<Int> get() = _startRotateEvent

    private var prevImagePath: String = ""
    private var takePictureUrl: Uri? = null

    fun start() {
        viewModelScope.launch(Dispatchers.Main) {
            _cursor.value = galleryProvider.fetchGalleryCo().getOrNull()
            delay(200)
            startViewHolderClickEvent.value = 1
        }
    }

    private fun changeBitmap(imagePath: String) {
        viewModelScope.launch(Dispatchers.Main) {
            galleryProvider.pathToBitmapCo(imagePath)
                .onSuccess { _selectPhotoBitmap.value = it }
        }
    }

    fun onSuccessSavePicture() {
        val url = takePictureUrl
        if (url != null) {
            viewModelScope.launch(Dispatchers.IO) {
                galleryProvider.saveGalleryPictureCo(
                    url.toString(),
                    "gallery_${System.currentTimeMillis()}"
                )
                galleryProvider.deleteCacheDirectoryCo()
            }
        }
    }

    fun onRotate() {
        var rotate = startRotateEvent.value ?: 0
        rotate = rotate.plus(90)
        if (rotate >= 360) {
            rotate = 0
        }
        _startRotateEvent.value = rotate
    }

    fun onCameraOpen() {
        takePictureUrl = galleryProvider.createGalleryPhotoUri("com.gallery.example.provider")
        startCameraOpenEvent.value = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, takePictureUrl)
            // 1 은 전면 카메라, 0 은 후면 카메라?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                putExtra("android.intent.extras.LENS_FACING_FRONT", 0)
            } else {
                putExtra("android.intent.extras.CAMERA_FACING", 0)
            }
        }
    }

    fun onPhotoClick(item: GalleryItem, isAdd: Boolean) {
        if (isAdd) {
            changeBitmap(item.imagePath)
        }
    }

    fun isSamePhoto(clickItem: GalleryItem): Boolean {
        return prevImagePath == clickItem.imagePath
    }

    fun onMaxPhotoClick() {
        startSnackBarEvent.value = "Max Picker Count"
    }
}
