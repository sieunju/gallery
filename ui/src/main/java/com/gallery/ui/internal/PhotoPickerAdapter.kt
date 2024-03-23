package com.gallery.ui.internal

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.gallery.core.GalleryProvider
import com.gallery.ui.R
import com.gallery.ui.model.PhotoPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Description : Photo Picker Adapter
 *
 * Created by juhongmin on 3/23/24
 */
internal class PhotoPickerAdapter(
    private val fragment: Fragment,
    private val provider: GalleryProvider
) : RecyclerView.Adapter<PhotoPickerAdapter.BaseViewHolder>() {

    private val crossFadeFactory: DrawableCrossFadeFactory by lazy {
        DrawableCrossFadeFactory
            .Builder()
            .setCrossFadeEnabled(true)
            .build()
    }

    private val crossFadeTransition: DrawableTransitionOptions by lazy {
        DrawableTransitionOptions
            .withCrossFade(crossFadeFactory)
    }

    private val placeHolder: ColorDrawable by lazy { ColorDrawable(Color.parseColor("#eeeeee")) }

    private val requestManager: RequestManager by lazy { Glide.with(fragment) }
    private val dataList: MutableList<PhotoPicker> by lazy { mutableListOf() }

    /**
     * 데이터가 변경되었을때 이전 데이터들 비교하여 갱신 처리 함수
     * @param newList oldList + 새로운 데이터 리스트
     */
    fun submitList(newList: List<PhotoPicker>) {
        val diffResult = DiffUtil.calculateDiff(SimpleDiffUtil(dataList, newList))
        dataList.clear()
        dataList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            R.layout.vh_child_camera -> CameraViewHolder(parent)
            R.layout.vh_child_photo -> PhotoViewHolder(parent)
            else -> throw IllegalArgumentException("Invalid ViewType")
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val item = dataList.getOrNull(position) ?: return
        holder.onBindView(item)
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            this.onBindViewHolder(holder, position)
        } else if (payloads[0] is List<*>) {
            @Suppress("UNCHECKED_CAST")
            holder.onPayloadBindView(payloads[0] as List<Any>)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = dataList.getOrNull(position) ?: return super.getItemViewType(position)
        return when (item) {
            is PhotoPicker.Camera -> R.layout.vh_child_camera
            is PhotoPicker.Photo -> R.layout.vh_child_photo
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class SimpleDiffUtil(
        private val oldList: List<PhotoPicker>,
        private val newList: List<PhotoPicker>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            val oldItem = oldList[oldPosition]
            val newItem = newList[newPosition]
            return if (oldItem is PhotoPicker.Photo && newItem is PhotoPicker.Photo) {
                oldItem.imagePath == newItem.imagePath
            } else if (oldItem is PhotoPicker.Camera && newItem is PhotoPicker.Camera) {
                true
            } else {
                false
            }
        }

        override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
            val oldItem = oldList[oldPosition]
            val newItem = newList[newPosition]
            return if (oldItem is PhotoPicker.Photo && newItem is PhotoPicker.Photo) {
                oldItem == newItem
            } else if (oldItem is PhotoPicker.Camera && newItem is PhotoPicker.Camera) {
                true
            } else {
                false
            }
        }
    }

    abstract class BaseViewHolder(
        parent: ViewGroup,
        @LayoutRes layoutId: Int
    ) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
    ) {
        abstract fun onBindView(item: PhotoPicker)
        open fun onPayloadBindView(payloads: List<Any>) {}
    }

    inner class CameraViewHolder(
        parent: ViewGroup
    ) : BaseViewHolder(parent, R.layout.vh_child_camera) {

        override fun onBindView(item: PhotoPicker) {

        }
    }

    inner class PhotoViewHolder(
        parent: ViewGroup
    ) : BaseViewHolder(parent, R.layout.vh_child_photo) {

        private val ivThumb: AppCompatImageView by lazy { itemView.findViewById(R.id.ivThumb) }
        private val vSelected: View by lazy { itemView.findViewById(R.id.vSelected) }
        private val clSelectedNum: ConstraintLayout by lazy { itemView.findViewById(R.id.clSelectedNum) }
        private val vBgNotSelected: View by lazy { itemView.findViewById(R.id.vBgNotSelected) }
        private val vBgSelected: View by lazy { itemView.findViewById(R.id.vBgSelected) }
        private val tvSelectNum: AppCompatTextView by lazy { itemView.findViewById(R.id.tvSelectNum) }
        private val overrideSize: Int by lazy { itemView.context.getDeviceWidth() / 3 }

        init {
            clSelectedNum.background = GradientDrawable(
                GradientDrawable.Orientation.BL_TR,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT)
            ).apply {
                cornerRadius = 10F.dp
            }
            clSelectedNum.clipToOutline = true
        }

        override fun onBindView(item: PhotoPicker) {
            if (item !is PhotoPicker.Photo) return
            requestManager.load(provider.getThumbnail(item.id, overrideSize))
                .transition(crossFadeTransition)
                .placeholder(placeHolder)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(ivThumb)

//            requestManager
//                .load(item.imagePath)
//                .transition(crossFadeTransition)
//                .placeholder(placeHolder)
//                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
//                .override(overrideSize)
//                .into(ivThumb)
        }
    }
}