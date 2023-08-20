package com.gallery.example.ui.flexible

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.gallery.example.R

internal class EditFlexibleImageViewHolder(
    private val requestManager: RequestManager,
    parent: ViewGroup,
    delegate: Listener
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context)
        .inflate(
            R.layout.vh_child_edit_flexible_image,
            parent,
            false
        )
) {

    interface Listener {
        fun onSelectPhoto(data: EditFlexibleImageItem)
    }

    private val ivThumb: AppCompatImageView by lazy { itemView.findViewById(R.id.ivThumb) }
    private val vSelected: View by lazy { itemView.findViewById(R.id.vSelected) }
    private val cvSelected: CardView by lazy { itemView.findViewById(R.id.cvSelected) }
    private val tvSelectedNum: AppCompatTextView by lazy { itemView.findViewById(R.id.tvSelectNum) }
    private var tempData: EditFlexibleImageItem? = null

    init {
        ivThumb.setOnClickListener {
            val data = tempData ?: return@setOnClickListener
            delegate.onSelectPhoto(data)
        }
    }

    fun onBindView(data: EditFlexibleImageItem) {
        tempData = data
        requestManager
            .load(data.imageUrl)
            .thumbnail(0.1F)
            .transition(DrawableTransitionOptions.withCrossFade())
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .override(300, 300)
            .into(ivThumb)
        setSelectedUI(data)
    }

    private fun setSelectedUI(data: EditFlexibleImageItem) {
        if (data.selectedNum != null) {
            vSelected.changeVisible(View.VISIBLE)
            cvSelected.changeVisible(View.VISIBLE)
            tvSelectedNum.text = "${data.selectedNum}"
        } else {
            vSelected.changeVisible(View.GONE)
            cvSelected.changeVisible(View.GONE)
        }
    }

    fun onPayloadBindView(list: List<*>) {
        val currentData = tempData ?: return
        for (element in list) {
            if (element is EditFlexibleImageItem) {
                if (element.imageUrl == currentData.imageUrl) {
                    setSelectedUI(element)
                    tempData = element
                    break
                }
            }
        }
    }

    private fun View.changeVisible(visible: Int) {
        if (visibility != visible) {
            visibility = visible
        }
    }
}
