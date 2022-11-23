package com.gallery.ui.internal

import androidx.recyclerview.widget.DiffUtil
import com.gallery.model.GalleryItem

/**
 * Description : Gallery DiffUtil Class
 *
 * Created by juhongmin on 2022/11/23
 */
internal class GalleryDiffUtil(
    private val oldList: List<GalleryItem>,
    private val newList: List<GalleryItem>
) : DiffUtil.Callback(){

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        TODO("Not yet implemented")
    }
}