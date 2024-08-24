package com.gallery.example

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.gallery.ui.PhotoPickerBottomSheet
import com.hmju.permission.SPermission
import timber.log.Timber


class MainRootFragment : Fragment(R.layout.f_main_root) {

    private val baseImagePath = "https://raw.githubusercontent.com/sieunju/gallery/develop/storage"
    private val requestManager: RequestManager by lazy { Glide.with(this) }
    private val imgEditFlexibleUrl = "${baseImagePath}/example_edit_flexible_image.webp"
    private val imgEditCropUrl = "${baseImagePath}/example_edit_crop_image.webp"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initThumb(view)

        view.findViewById<CardView>(R.id.cvGallery).setOnClickListener {
            findNavController().navigate(R.id.action_root_to_gallery)
        }

        view.findViewById<CardView>(R.id.cvEditFlexible).setOnClickListener {
            findNavController().navigate(R.id.action_root_to_editFlexibleImage)
        }

        view.findViewById<CardView>(R.id.cvEditCrop).setOnClickListener {
            findNavController().navigate(R.id.action_root_to_editCropImage)
        }

        view.findViewById<CardView>(R.id.cvPhotoPickerBottomSheet).setOnClickListener {
            PhotoPickerBottomSheet()
                .setMaxCount(4)
                .setSubmitListener {
                    Timber.d("Selected $it")
                }
                .setCancelListener { Timber.d("Cancel") }
                .simpleShow(childFragmentManager)
        }

//        val permissions = mutableListOf<String>()
//        permissions.add(Manifest.permission.CAMERA)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
//            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
//            permissions.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
//            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
//        } else {
//            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
//        }
//        SPermission(this)
//            .addPermissions(permissions)
//            .build { b, map -> }
    }

    private fun initThumb(view: View) {
        val ivEditFlexible = view.findViewById<AppCompatImageView>(R.id.ivEditFlexible)
        val ivEditCrop = view.findViewById<AppCompatImageView>(R.id.ivEditCrop)
        requestManager
            .load(imgEditFlexibleUrl)
            .optionalTransform(FitCenter())
            .optionalTransform(WebpDrawable::class.java, WebpDrawableTransformation(FitCenter()))
            .into(ivEditFlexible)
        requestManager
            .load(imgEditCropUrl)
            .optionalTransform(FitCenter())
            .optionalTransform(WebpDrawable::class.java, WebpDrawableTransformation(FitCenter()))
            .into(ivEditCrop)

    }
}