package com.gallery.ui

import android.content.Context
import android.database.Cursor
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import com.gallery.ui.adapter.GalleryAdapter

/**
 * Description : Gallery RecyclerView
 *
 * Created by juhongmin on 2022/11/23
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class GalleryRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val adapter: GalleryAdapter by lazy { GalleryAdapter() }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.GalleryRecyclerView).run {
            try {
                adapter.setIsCameraShow(
                    getBoolean(
                        R.styleable.GalleryRecyclerView_galleryAdapterCameraVisible,
                        false
                    )
                )
                adapter.setCameraDrawableResId(
                    getInteger(
                        R.styleable.GalleryRecyclerView_galleryAdapterCameraDrawableRes,
                        R.drawable.ic_camera
                    )
                )
                adapter.setSelectedMaxCount(
                    getInt(
                        R.styleable.GalleryRecyclerView_galleryMaxSelectedCount,
                        3
                    )
                )
                setAdapter(adapter)
            } catch (ex: Exception) {
            }
            recycle()
        }
    }

    /**
     * setCursor
     */
    fun setCursor(cursor: Cursor?) {
        adapter.setCursor(cursor)
    }
}
