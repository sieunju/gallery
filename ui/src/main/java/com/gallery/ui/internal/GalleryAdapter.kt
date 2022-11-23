package com.gallery.ui.internal

import android.database.Cursor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.gallery.model.BaseGalleryItem
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
    // [e] Attribute Set

    private var lastPos = -1
    private var size = 0
    private var photoCursor: Cursor? = null
    var maxPickerCnt: Int = 4 // 최대 선택 할 수 있는 사진 개수

    fun setCursor(newCursor: Cursor?) {
        if (newCursor == null) return
        dataList.clear()

        lastPos = -1
        size = newCursor.count
        // set Cursor
        val prevCursor = photoCursor
        if (prevCursor != null && !prevCursor.isClosed) {
            prevCursor.close()
        }
        photoCursor = newCursor
        notifyItemRangeChanged(0, itemCount)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.vh_child_gallery -> GalleryViewHolder(parent)
            R.layout.vh_child_camera -> CameraOpenViewHolder(parent, cameraDrawableRes)
            else -> throw IllegalArgumentException("Invalid ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, pos: Int) {
        
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

    inner class GalleryViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.vh_child_gallery, parent, false
        )
    ) {

        fun onBindView(item: GalleryItem?) {
            if (item == null) return

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