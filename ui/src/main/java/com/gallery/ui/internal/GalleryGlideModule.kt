package com.gallery.ui.internal

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.LibraryGlideModule

/**
 * Description : Library Glide Module
 *
 * Created by juhongmin on 2024. 8. 25.
 */
//@Suppress("unused")
//@GlideModule
////class GalleryGlideModule : LibraryGlideModule() {
//
//    override fun registerComponents(
//        context: Context,
//        glide: Glide,
//        registry: Registry
//    ) {
//        Log.d("JLOGGER", "GalleryGlide Module!!!!")
//        registry.prepend(
//            Uri::class.java,
//            Bitmap::class.java,
//            ThumbnailModelLoader.Factory(context)
//        )
//    }
//}