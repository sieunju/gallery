package com.gallery.ui

import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
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
     * Gallery Listener
     */
    fun setListener(listener: GalleryListener): GalleryAdapter {
        adapter.listener = listener
        return adapter
    }

    private val Int.dp: Int
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            Resources.getSystem().displayMetrics
        ).toInt()
}
