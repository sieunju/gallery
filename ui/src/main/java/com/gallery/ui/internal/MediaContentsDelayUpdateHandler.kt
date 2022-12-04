package com.gallery.ui.internal

import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.lifecycle.Lifecycle
import com.gallery.ui.GalleryRecyclerView
import com.gallery.ui.GalleryAdapter
import timber.log.Timber

/**
 * Description : MediaContents Delay Update Handler
 *
 * Created by juhongmin on 2022/11/27
 */
internal class MediaContentsDelayUpdateHandler(
    private val view: GalleryRecyclerView,
    private val adapter: GalleryAdapter
) : Handler(Looper.getMainLooper()) {

    companion object {
        const val UPDATE_TYPE = 1
    }

    override fun handleMessage(msg: Message) {
        if (msg.what == UPDATE_TYPE) {
            Timber.d("HandleMessage ${msg.obj}")
            val obj = msg.obj
            if (obj is String) {
                if (view.lifecycleStatus != Lifecycle.Event.ON_DESTROY) {
                    adapter.setTakePictureItemUpdate(obj)
                }
            }
        }
    }
}
