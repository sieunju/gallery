package com.gallery.ui.internal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gallery.ui.R
import com.gallery.ui.SelectionAlbumBottomSheet
import com.gallery.ui.model.PhotoPicker
import com.gallery.ui.model.PickerAlbum

/**
 * Description : Selection Album Adapter
 *
 * Created by juhongmin on 4/11/24
 */
internal class SelectionAlbumAdapter(
    private val listener: SelectionAlbumBottomSheet.Listener
) : RecyclerView.Adapter<SelectionAlbumAdapter.ViewHolder>() {

    private val dataList: MutableList<PickerAlbum> by lazy { mutableListOf() }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        dataList.getOrNull(position)?.let { holder.onBindView(it) }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class ViewHolder(
        parent: ViewGroup
    ) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.vh_child_selection_album,parent,false)
    ) {

        fun onBindView(item: PickerAlbum) {

        }
    }
}