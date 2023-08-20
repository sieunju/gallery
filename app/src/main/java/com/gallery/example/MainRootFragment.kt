package com.gallery.example

import android.Manifest
import android.graphics.Bitmap
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
import hmju.permissions.core.SPermissions


class MainRootFragment : Fragment(R.layout.f_main_root) {
    private val baseImagePath = "https://raw.githubusercontent.com/sieunju/gallery/develop/storage"
    private val requestManager: RequestManager by lazy { Glide.with(this) }
    private val imgEditFlexibleUrl = "${baseImagePath}/example_edit_flexible_image.webp"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivEditFlexible = view.findViewById<AppCompatImageView>(R.id.ivEditFlexible)

        val fitCenter: Transformation<Bitmap> = FitCenter()
        requestManager
            .load(imgEditFlexibleUrl)
            .optionalTransform(fitCenter)
            .optionalTransform(WebpDrawable::class.java, WebpDrawableTransformation(fitCenter))
            .into(ivEditFlexible)

        view.findViewById<CardView>(R.id.cvGallery).setOnClickListener {
            findNavController().navigate(R.id.action_root_to_gallery)
        }

        view.findViewById<CardView>(R.id.cvEditFlexible).setOnClickListener {
            findNavController().navigate(R.id.action_root_to_editFlexibleImage)
        }

        SPermissions(this)
            .requestPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            .build { b, strings -> }
    }
}