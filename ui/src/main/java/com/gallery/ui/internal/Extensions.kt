package com.gallery.ui.internal

import android.view.View
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory

internal fun View.changeVisible(changeVisible: Int) {
    if (visibility != changeVisible) {
        visibility = changeVisible
    }
}

internal val crossFadeFactory: DrawableCrossFadeFactory by lazy {
    DrawableCrossFadeFactory
        .Builder()
        .setCrossFadeEnabled(true)
        .build()
}

internal val crossFadeTransition: DrawableTransitionOptions by lazy {
    DrawableTransitionOptions
        .withCrossFade(crossFadeFactory)
}