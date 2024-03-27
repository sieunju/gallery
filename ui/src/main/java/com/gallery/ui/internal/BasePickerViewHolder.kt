package com.gallery.ui.internal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.gallery.ui.model.PhotoPicker

/**
 * Description : PhotoPicker ViewHolder
 *
 * Created by juhongmin on 3/27/24
 */
abstract class BasePickerViewHolder(
    parent: ViewGroup,
    @LayoutRes layoutId: Int
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context)
        .inflate(layoutId, parent, false)
) {
    abstract fun onBindView(item: PhotoPicker)
    open fun onPayloadBindView(payloads: List<Any>) {}
}
