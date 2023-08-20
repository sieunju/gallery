package com.gallery.example.ui.crop

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.gallery.example.R

internal class EditCropImageViewHolder(
    private val requestManager: RequestManager,
    parent: ViewGroup,
    delegate: Listener
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context)
        .inflate(
            R.layout.vh_child_edit_crop_image,
            parent,
            false
        )
) {

    interface Listener {
        fun onSelectPhoto(data: EditCropImageItem)
    }

    private val ivThumb: AppCompatImageView by lazy { itemView.findViewById(R.id.ivThumb) }
    private val placeHolder: ColorDrawable by lazy { ColorDrawable(Color.parseColor("#EFEFEF")) }

    private var tempData: EditCropImageItem? = null

    init {
        ivThumb.setOnClickListener {
            val data = tempData ?: return@setOnClickListener
            delegate.onSelectPhoto(data)
        }
    }

    fun onBindView(data: EditCropImageItem) {
        tempData = data
        requestManager.load(data.imageUrl)
            .placeholder(placeHolder)
            .thumbnail(0.1F)
            .transition(DrawableTransitionOptions.withCrossFade())
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .override(300, 300)
            .into(ivThumb)
    }
}
