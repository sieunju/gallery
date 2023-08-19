package com.gallery.example.ui.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.gallery.example.R

class GeneralGalleryViewHolder(
    private val requestManager: RequestManager,
    parent: ViewGroup,
    delegate: Listener
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context)
        .inflate(
            R.layout.vh_child_general_gallery,
            parent,
            false
        )
) {

    interface Listener {
        fun onSelectPhoto(data: GeneralGalleryItem): List<GeneralGalleryItem>
    }

    private val ivThumb: AppCompatImageView by lazy { itemView.findViewById(R.id.ivThumb) }
    private val vSelected: View by lazy { itemView.findViewById(R.id.vSelected) }
    private val cvSelected: CardView by lazy { itemView.findViewById(R.id.cvSelected) }
    private val tvSelectedNum: AppCompatTextView by lazy { itemView.findViewById(R.id.tvSelectNum) }
    private var tempData: GeneralGalleryItem? = null

    init {
        ivThumb.setOnClickListener {
            val data = tempData ?: return@setOnClickListener
            rangeNotifyPayload(delegate.onSelectPhoto(data))
        }
    }

    fun onBindView(data: GeneralGalleryItem) {
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

    private fun setSelectedUI(data: GeneralGalleryItem) {
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
            if (element is GeneralGalleryItem) {
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

    private fun rangeNotifyPayload(notifyList: List<GeneralGalleryItem>) {
        if (itemView.parent is RecyclerView) {
            val rv = itemView.parent as RecyclerView
            val lm = rv.layoutManager ?: return
            val adapter = rv.adapter ?: return
            val firstPos = if (lm is LinearLayoutManager) {
                lm.findFirstVisibleItemPosition()
            } else {
                0
            }
            val lastPos = if (lm is LinearLayoutManager) {
                lm.findLastVisibleItemPosition()
            } else {
                lm.itemCount
            }

            adapter.notifyItemRangeChanged(firstPos, lastPos, notifyList)
        }
    }
}