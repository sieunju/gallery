package com.gallery.ui.internal

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.gallery.ui.R
import com.gallery.ui.SelectionAlbumBottomSheet
import com.gallery.ui.model.PickerAlbum

/**
 * Description : Selection Album Adapter
 *
 * Created by juhongmin on 4/11/24
 */
internal class SelectionAlbumAdapter(
    private val bottomSheet: SelectionAlbumBottomSheet,
    private val dataList: List<PickerAlbum>
) : RecyclerView.Adapter<SelectionAlbumAdapter.ViewHolder>() {

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
            .inflate(R.layout.vh_child_selection_album, parent, false)
    ) {

        private val tvTitle: AppCompatTextView by lazy { itemView.findViewById(R.id.tvTitle) }
        private var model: PickerAlbum? = null

        init {
            tvTitle.setOnClickListener {
                val model = model ?: return@setOnClickListener
                bottomSheet.onSelectedAlbum(model)
            }
        }

        fun onBindView(item: PickerAlbum) {
            model = item
            tvTitle.text = item.getTitle()
        }
    }
}