package com.gallery.ui.adapter

import android.content.res.Resources
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.gallery.model.BaseGalleryItem
import com.gallery.model.CameraOpenItem
import com.gallery.model.GalleryItem
import com.gallery.ui.R

/**
 * Description : Gallery RecyclerView 전용 Adapter Class
 *
 * Created by juhongmin on 2022/11/23
 */
class GalleryAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    /**
     * GalleryItem
     */
    private val dataList: MutableList<BaseGalleryItem> by lazy { mutableListOf() }

    /**
     * Selected Photo Map
     */
    private val selectedPhotoMap: MutableMap<String, GalleryItem> by lazy { mutableMapOf() }

    // [s] Attribute Set
    private var isShowCamera = false
    private var cameraDrawableRes: Int = R.drawable.ic_camera
    private var selectedSize: Int = 30.dp
    private var selectedBgDrawable: Drawable? = null
    @ColorInt
    private var selectedTxtColor: Int = Color.WHITE
    // [e] Attribute Set

    private var lastPos = -1
    private var size = 0
    private var photoCursor: Cursor? = null
    var maxPickerCnt: Int = 4 // 최대 선택 할 수 있는 사진 개수

    fun setCursor(newCursor: Cursor?) {
        if (newCursor == null) return
        dataList.clear()

        if (isShowCamera) {
            // fist Index Default Camera Item
            dataList.add(CameraOpenItem())
        }

        lastPos = -1
        size = newCursor.count
        // set Cursor
        val prevCursor = photoCursor
        if (prevCursor != null && !prevCursor.isClosed) {
            prevCursor.close()
        }
        photoCursor = newCursor
        notifyItemRangeChanged(0, 10)
    }

    /**
     * first index Camera Visible or Gone
     */
    fun setIsCameraShow(isShow: Boolean): GalleryAdapter {
        isShowCamera = isShow
        return this
    }

    /**
     * first index Camera Custom Drawable Id
     */
    fun setCameraDrawableResId(@DrawableRes resId: Int): GalleryAdapter {
        cameraDrawableRes = resId
        return this
    }

    /**
     * set Selected Max Count
     */
    fun setSelectedMaxCount(count: Int): GalleryAdapter {
        maxPickerCnt = count
        return this
    }

    /**
     * set Selected Width, Height DP
     * @param size Dimension 1:1 Ratio Value
     */
    fun setSelectedSize(size: Int): GalleryAdapter {
        selectedSize = size
        return this
    }

    /**
     * set Selected View background drawable
     * @param drawable Background Drawable
     */
    fun setSelectedDrawable(drawable: Drawable): GalleryAdapter {
        selectedBgDrawable = drawable
        return this
    }

    /**
     * set Selected Text Color
     * @param color Text Color
     */
    fun setSelectedTextColor(@ColorInt color: Int): GalleryAdapter {
        selectedTxtColor = color
        return this
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.vh_child_gallery -> GalleryViewHolder(parent)
            R.layout.vh_child_camera -> CameraOpenViewHolder(parent, cameraDrawableRes)
            else -> throw IllegalArgumentException("Invalid ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        try {
            if (holder is GalleryViewHolder) {
                val pos = holder.bindingAdapterPosition
                if (lastPos < pos) {
                    lastPos = pos
                    performCursorToGalleryItem()
                }

                if (itemCount > pos) {
                    holder.onBindView(dataList[pos])
                }
            }

            performCloseCursor()
        } catch (ex: Exception) {
            // ignore
        }
    }

    override fun getItemViewType(pos: Int): Int {
        return if (isShowCamera && pos == 0) {
            R.layout.vh_child_camera
        } else {
            R.layout.vh_child_gallery
        }
    }

    override fun getItemCount(): Int {
        return size
    }

    /**
     * Perform Converter Cursor To Gallery Item
     * And dataList Add GalleryItem
     * @see GalleryItem
     */
    private fun performCursorToGalleryItem() {
        photoCursor?.runCatching {
            if (moveToNext()) {
                val mediaId = try {
                    getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                } catch (ex: IllegalArgumentException) {
                    0
                }
                val contentId = getLong(mediaId)
                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentId.toString()
                )

                // 기존에 선택한 아이템이라면 주소값 넘김
                val selectGalleryItem = selectedPhotoMap[uri.toString()]
                if (selectGalleryItem != null) {
                    dataList.add(selectGalleryItem)
                } else {
                    dataList.add(GalleryItem(uri.toString()))
                }

                // initBindViewHolder Listener
            }
        }
    }

    /**
     * Perform Closet Cursor
     */
    private fun performCloseCursor() {
        if (lastPos == itemCount.minus(1)) {
            photoCursor?.close()
        }
    }

    private val Int.dp: Int
        get() = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            Resources.getSystem().displayMetrics
        ).toInt()

    inner class GalleryViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.vh_child_gallery, parent, false
        )
    ) {

        private val ivThumb: AppCompatImageView by lazy { itemView.findViewById(R.id.ivThumb) }
        private val vSelected: View by lazy { itemView.findViewById(R.id.vSelected) }
        private val clSelected: ConstraintLayout by lazy { itemView.findViewById(R.id.clSelected) }
        private val tvSelectNum: AppCompatTextView by lazy { itemView.findViewById(R.id.tvSelectNum) }
        private val placeHolder: ColorDrawable by lazy {
            ColorDrawable(Color.argb(50, 60, 63, 65))
        }

        private var lifecycleOwner: LifecycleOwner? = null

        private val crossFadeFactory: DrawableCrossFadeFactory by lazy {
            DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
        }

        private val crossFadeTransition: DrawableTransitionOptions by lazy {
            DrawableTransitionOptions.withCrossFade(crossFadeFactory)
        }

        init {
            itemView.doOnAttach { v ->
                lifecycleOwner = ViewTreeLifecycleOwner.get(v)
            }
            itemView.doOnDetach {
                lifecycleOwner = null
            }

            ivThumb.setOnClickListener {

            }
        }

        fun onBindView(item: BaseGalleryItem?) {
            if (item == null) return

            if (item is GalleryItem) {
                Glide.with(ivThumb)
                    .load(item.imagePath)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .thumbnail(0.1F)
                    .override(300, 300)
                    .placeholder(placeHolder)
                    .transition(crossFadeTransition)
                    .into(ivThumb)
            }
        }
    }

    inner class CameraOpenViewHolder(
        parent: ViewGroup,
        @DrawableRes drawableId: Int
    ) : RecyclerView.ViewHolder(
        LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.vh_child_camera,
                parent,
                false
            )
    ) {
        private val ivCamera: AppCompatImageView by lazy { itemView.findViewById(R.id.ivCamera) }

        init {
            ivCamera.setImageResource(drawableId)
            ivCamera.setOnClickListener {
                // Camera Open
            }
        }
    }
}