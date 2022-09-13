package com.gallery.core

import android.content.Context
import com.gallery.core.impl.GalleryProviderImpl

/**
 * Gallery Provider Builder Class
 */
class Builder {
    companion object {

        /**
         * Do not Activity Context!
         * @param context Application Context
         */
        fun build(context: Context): GalleryProvider {
            return GalleryProviderImpl(context)
        }
    }
}
