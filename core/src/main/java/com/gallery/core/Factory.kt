package com.gallery.core

import android.content.Context
import com.gallery.core.impl.GalleryProviderImpl

/**
 * Gallery Provider Factory Class
 */
class Factory {
    companion object {

        /**
         * Do not Activity Context!
         * @param context Application Context
         */
        fun create(context: Context): GalleryProvider {
            return GalleryProviderImpl(context)
        }
    }
}
