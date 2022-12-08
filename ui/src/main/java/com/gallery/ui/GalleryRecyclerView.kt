package com.gallery.ui

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.provider.MediaStore
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.gallery.ui.internal.MediaContentsDelayUpdateHandler
import com.gallery.ui.internal.dp

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
) : RecyclerView(context, attrs, defStyleAttr), LifecycleEventObserver {

    private val adapter: GalleryAdapter by lazy { GalleryAdapter() }
    var lifecycleStatus: Lifecycle.Event = Lifecycle.Event.ON_ANY

    private val mediaContentsUpdateHandler: MediaContentsDelayUpdateHandler by lazy {
        MediaContentsDelayUpdateHandler(this, adapter)
    }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.GalleryRecyclerView).run {
            try {
                setCameraShow(
                    getBoolean(
                        R.styleable.GalleryRecyclerView_galleryCameraVisible,
                        false
                    )
                )
                setCameraResourceId(
                    getInteger(
                        R.styleable.GalleryRecyclerView_galleryCameraDrawableId,
                        R.drawable.ic_camera
                    )
                )
                setSelectedMaxCount(
                    getInt(
                        R.styleable.GalleryRecyclerView_gallerySelectedMaxCount,
                        3
                    )
                )

                setSelectedSize(
                    getDimensionPixelSize(
                        R.styleable.GalleryRecyclerView_gallerySelectedSize,
                        30.dp
                    )
                )
                val bgColor = getColor(
                    R.styleable.GalleryRecyclerView_gallerySelectedBgColor,
                    Color.rgb(51, 153, 255)
                )
                val strokeWidth = getDimensionPixelSize(
                    R.styleable.GalleryRecyclerView_gallerySelectedStroke,
                    0.dp
                )
                val strokeColor = getColor(
                    R.styleable.GalleryRecyclerView_gallerySelectedStrokeColor,
                    Color.WHITE
                )
                val corner = getDimensionPixelSize(
                    R.styleable.GalleryRecyclerView_gallerySelectedCorner,
                    15.dp
                )
                val drawable = GradientDrawable(
                    GradientDrawable.Orientation.BL_TR,
                    intArrayOf(bgColor, bgColor)
                )
                drawable.cornerRadius = corner.toFloat()
                if (strokeWidth != 0) {
                    drawable.setStroke(strokeWidth, strokeColor)
                }
                setSelectedDrawable(drawable)

                setSelectedTxtColor(
                    getColor(
                        R.styleable.GalleryRecyclerView_gallerySelectedTxtColor,
                        Color.WHITE
                    )
                )

                setSelectGravity(
                    getInt(
                        R.styleable.GalleryRecyclerView_gallerySelectedGravity,
                        0x50 shl 0x05
                    )
                )

                setAdapter(adapter)
            } catch (ex: Exception) {
            }
            recycle()
        }

        isMotionEventSplittingEnabled = false
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        lifecycleStatus = event
    }

    /**
     * setLifecycle Observer
     */
    fun setLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    fun setLifecycle(fragmentActivity: FragmentActivity) {
        fragmentActivity.lifecycle.addObserver(this)
    }

    fun setLifecycle(fragment: Fragment) {
        fragment.lifecycle.addObserver(this)
    }

    /**
     * setCursor
     */
    fun setCursor(cursor: Cursor?) {
        if (cursor == null) return

        adapter.setCursor(cursor)
        context.contentResolver.unregisterContentObserver(contentsObserver)
        context.contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentsObserver
        )
    }

    private val contentsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            if (uri != null) {
                mediaContentsUpdateHandler.removeMessages(MediaContentsDelayUpdateHandler.UPDATE_TYPE)
                val message = Message().apply {
                    what = MediaContentsDelayUpdateHandler.UPDATE_TYPE
                    obj = uri.toString()
                }
                mediaContentsUpdateHandler.sendMessageDelayed(message, 500)
            }
        }
    }

    /**
     * First Camera Show
     * @param isShow true Camera Show, false Camera Hidden
     */
    fun setCameraShow(isShow: Boolean): GalleryAdapter {
        return adapter.setIsCameraShow(isShow)
    }

    /**
     * First Camera Resource Id
     */
    fun setCameraResourceId(@DrawableRes id: Int): GalleryAdapter {
        return adapter.setCameraDrawableResId(id)
    }

    /**
     * set Selected Camera Max Count
     */
    fun setSelectedMaxCount(count: Int): GalleryAdapter {
        return adapter.setSelectedMaxCount(count)
    }

    /**
     * set Selected UI Width / Height
     * @param size Ratio 1:1 Width
     */
    fun setSelectedSize(size: Int): GalleryAdapter {
        return adapter.setSelectedSize(size)
    }

    /**
     * set Selected Drawable
     */
    fun setSelectedDrawable(drawable: Drawable): GalleryAdapter {
        return adapter.setSelectedDrawable(drawable)
    }

    /**
     * set Selected Text Color
     */
    fun setSelectedTxtColor(@ColorInt color: Int): GalleryAdapter {
        return adapter.setSelectedTextColor(color)
    }

    /**
     * Glide Request Manager
     */
    fun setRequestManager(manager: RequestManager): GalleryAdapter {
        return adapter.setRequestManager(manager)
    }

    /**
     * Selected UI Gravity
     */
    fun setSelectGravity(gravity: Int): GalleryAdapter {
        return adapter.setSelectGravity(gravity)
    }

    /**
     * Request Adapter.ViewHolder Click Function
     * @param pos Request Position
     */
    fun requestViewHolderClick(pos: Int) {
        adapter.requestViewHolderClick(pos)
    }

    /**
     * Gallery Listener
     */
    fun setListener(listener: GalleryListener): GalleryAdapter {
        adapter.listener = listener
        return adapter
    }
}
