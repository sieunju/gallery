package com.gallery.ui

import android.content.res.Resources
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.gallery.model.BaseGalleryItem
import com.gallery.model.CameraOpenItem
import com.gallery.model.GalleryItem
import com.gallery.ui.internal.changeVisible
import com.gallery.ui.internal.crossFadeTransition
import com.gallery.ui.internal.isAndOperatorTrue

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
    private val pickerMap: MutableMap<String, GalleryItem> by lazy { mutableMapOf() }

    /**
     * Selected Listener
     */
    var listener: GalleryListener? = null

    // [s] Attribute Set
    private var isShowCamera = false
    private var cameraDrawableRes: Int = R.drawable.ic_camera
    private var selectedSize: Int = 30.dp
    private var selectedBgDrawable: Drawable? = null

    @ColorInt
    private var selectedTxtColor: Int = Color.WHITE
    private var requestManager: RequestManager? = null
    private var selectedGravity: Int = 0x50 shl 0x05
    private val SELECT_TOP = 0x30
    private val SELECT_BOTTOM = 0x50
    private val SELECT_LEFT = 0x03
    private val SELECT_RIGHT = 0x05
    // [e] Attribute Set

    private var lastPos = -1
    private var size = 0
    private var photoCursor: Cursor? = null
    var maxPickerCnt: Int = 4 // 최대 선택 할 수 있는 사진 개수

    /**
     * setCursor
     * @param newCursor Set NewCursor
     */
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
     * 카메라를 찍은 이후 이미지를 개신 처리하기위한 함수
     * @param imagePath TakePicture Camera Contents path
     */
    fun setTakePictureItemUpdate(imagePath: String) {
        val searchPos = if (isShowCamera) {
            1
        } else {
            0
        }
        val item = dataList[searchPos]
        if (item is GalleryItem) {
            if (item.imagePath != imagePath) {
                dataList.add(searchPos, GalleryItem(imagePath))
                notifyItemInserted(searchPos)
            } else {
                notifyItemChanged(searchPos)
            }
        }
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

    /**
     * set Glide RequestManager
     * @param manager RequestManager
     */
    fun setRequestManager(manager: RequestManager): GalleryAdapter {
        requestManager = manager
        return this
    }

    /**
     * set Selected Ui Gravity
     * @param gravity
     */
    fun setSelectGravity(gravity: Int): GalleryAdapter {
        // top 0x30, bottom 0x50, left 0x03, right 0x05
        selectedGravity = gravity
        return this
    }

    fun requestViewHolderClick(pos: Int) {
        notifyItemChanged(pos, true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            R.layout.vh_child_gallery -> GalleryViewHolder(parent)
            R.layout.vh_child_camera -> CameraOpenViewHolder(parent)
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

    /**
     * onBindView Payloads
     */
    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.size == 0) {
            this.onBindViewHolder(holder, position)
        } else {
            if (payloads[0] is List<*>) {
                if (holder is GalleryViewHolder) {
                    holder.onBindView(payloads[0] as List<Any>)
                }
            } else if (payloads[0] is Boolean) {
                if (holder is GalleryViewHolder) {
                    holder.onPerformClick(position)
                }
            }
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
                val selectGalleryItem = pickerMap[uri.toString()]
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

    /**
     * 사진을 선택하고 나서
     * 선택한 숫자 갱신 처리하는 함수
     * @param isAdd 추가 or 제거
     * @param item 갤러리 선택 아이템
     * @param map Map
     */
    fun sortedPickerMap(
        isAdd: Boolean,
        item: GalleryItem,
        map: MutableMap<String, GalleryItem>
    ) {
        if (isAdd) {
            item.selectedNum = "${map.size.plus(1)}"
            map[item.imagePath] = item
        } else {
            map.remove(item.imagePath)
            var idx = 1
            map.forEach { entry ->
                entry.value.selectedNum = idx.toString()
                idx++
            }
        }
    }

    inner class GalleryViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.vh_child_gallery, parent, false
        )
    ) {

        private val ivThumb: AppCompatImageView by lazy { itemView.findViewById(R.id.ivThumb) }
        private val vSelected: View by lazy { itemView.findViewById(R.id.vSelected) }
        private val clSelectedNumber: ConstraintLayout by lazy { itemView.findViewById(R.id.clSelectedNumber) }
        private val vSelectedNum: View by lazy { itemView.findViewById(R.id.vSelectedNum) }
        private val tvSelectNum: AppCompatTextView by lazy { itemView.findViewById(R.id.tvSelectNum) }
        private val placeHolder: ColorDrawable by lazy { ColorDrawable(Color.argb(50, 60, 63, 65)) }

        private var resizeWidth = 300
        private var model: GalleryItem? = null

        init {
            initStyle()
            ivThumb.setOnClickListener {
                performAddPhotoClick()
            }

            clSelectedNumber.setOnClickListener {
                performRemovePhotoClick()
            }
        }

        private fun initStyle() {
            clSelectedNumber.updateLayoutParams {
                width = selectedSize.plus(10.dp)
                height = selectedSize.plus(10.dp)
                if (this is ConstraintLayout.LayoutParams) {
                    topToTop = -1
                    bottomToBottom = -1
                    leftToLeft = -1
                    rightToRight = -1
                    if (isAndOperatorTrue(selectedGravity,SELECT_TOP)) {
                        topToTop = R.id.ivThumb
                    }
                    if (isAndOperatorTrue(selectedGravity,SELECT_BOTTOM)) {
                        bottomToBottom = R.id.ivThumb
                    }
                    if (isAndOperatorTrue(selectedGravity,SELECT_LEFT)) {
                        leftToLeft = R.id.ivThumb
                    }
                    if (isAndOperatorTrue(selectedGravity,SELECT_RIGHT)) {
                        rightToRight = R.id.ivThumb
                    }
                }
            }
            vSelectedNum.background = selectedBgDrawable
            tvSelectNum.setTextColor(selectedTxtColor)
            ivThumb.post {
                resizeWidth = ivThumb.width.minus(30.dp)
            }
        }

        /**
         * Normal onBindView
         */
        fun onBindView(item: BaseGalleryItem?) {
            if (item == null) return

            if (item is GalleryItem) {
                model = item
                setLoadImage(item)
                setPickerItem(item)
            }
        }

        /**
         * Payload BindView
         * @param payloads Payload DataList
         */
        fun onBindView(payloads: List<Any>) {
            model?.runCatching {
                for (idx: Int in payloads.indices) {
                    val item = payloads[idx]
                    if (item is GalleryItem) {
                        if (item.imagePath == this.imagePath) {
                            onBindView(item)
                            break
                        }
                    }
                }
            }
        }

        /**
         * 인위적으로 클릭 처리하는 함수
         */
        fun onPerformClick(pos: Int) {
            if (bindingAdapterPosition == pos) {
                ivThumb.post {
                    performAddPhotoClick()
                }
            }
        }

        /**
         * Perform Add Photo Click Function
         */
        private fun performAddPhotoClick() {
            model?.runCatching {
                // 선택 하는 경우
                if (!isSelected) {
                    // Max Size
                    if (pickerMap.size >= maxPickerCnt) {
                        listener?.onMaxPickerCount()
                        return@runCatching
                    }

                    // 추가인 경우 나머지 갱신 처리할 필요가 없음
                    sortedPickerMap(true, this, pickerMap)
                    isSelected = !isSelected
                    notifyItemChanged(bindingAdapterPosition)
                    listener?.onPhotoPicker(this, true)
                } else {
                    // 이미 선택한 경우
                    listener?.onPhotoPicker(this, true)
                }
            }
        }

        /**
         * Perform Remove Photo Click
         */
        private fun performRemovePhotoClick() {
            model?.runCatching {
                if (isSelected) {
                    sortedPickerMap(false, this, pickerMap)
                    isSelected = !isSelected
                    rangeNotifyPayload(this)

                    listener?.onPhotoPicker(this, false)
                }
            }
        }

        private fun setLoadImage(item: GalleryItem) {
            val manager = requestManager ?: Glide.with(itemView.context)
            manager.load(item.imagePath)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .override(resizeWidth, resizeWidth)
                .placeholder(placeHolder)
                .transition(crossFadeTransition)
                .into(ivThumb)
        }

        private fun setPickerItem(item: GalleryItem) {
            if (item.isSelected) {
                vSelected.changeVisible(View.VISIBLE)
                clSelectedNumber.changeVisible(View.VISIBLE)
                tvSelectNum.text = item.selectedNum
            } else {
                vSelected.changeVisible(View.GONE)
                clSelectedNumber.changeVisible(View.GONE)
            }
        }

        /**
         * 선택한 아이템들만 갱신 처리하도록 하는 함수
         * @param changeItem Current Selected Item
         */
        private fun rangeNotifyPayload(changeItem: GalleryItem) {
            if (itemView.parent is RecyclerView) {
                val layoutManager = (itemView.parent as RecyclerView).layoutManager
                if (layoutManager is GridLayoutManager) {
                    val firstPosition = layoutManager.findFirstVisibleItemPosition()
                    val lastPosition = layoutManager.findLastVisibleItemPosition()
                    val notifyList = mutableListOf<GalleryItem>()
                    notifyList.add(changeItem)
                    pickerMap.forEach { notifyList.add(it.value) }

                    // 현재 보여지고 있는 뷰에 대해서만 갱신 처리
                    notifyItemRangeChanged(firstPosition, lastPosition.plus(1), notifyList)
                } else if (layoutManager is LinearLayoutManager) {
                    val firstPosition = layoutManager.findFirstVisibleItemPosition()
                    val lastPosition = layoutManager.findLastVisibleItemPosition()
                    val notifyList = mutableListOf<GalleryItem>()
                    notifyList.add(changeItem)
                    pickerMap.forEach { notifyList.add(it.value) }
                    // 현재 보여지고 있는 뷰에 대해서만 갱신 처리
                    notifyItemRangeChanged(firstPosition, lastPosition.plus(1), notifyList)
                }
            }
        }


    }

    inner class CameraOpenViewHolder(
        parent: ViewGroup
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
            ivCamera.setImageResource(cameraDrawableRes)
            ivCamera.setOnClickListener {
                listener?.onCameraOpen()
            }
        }
    }
}