package com.gallery.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gallery.ui.internal.BasePickerViewHolder
import com.gallery.ui.model.PickerAlbum
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Description : Selection Album BottomSheet
 *
 * Created by juhongmin on 4/11/24
 */
internal class SelectionAlbumBottomSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun onSelectedAlbum(album: PickerAlbum)
    }

    private val dataList: MutableList<PickerAlbum> by lazy { mutableListOf() }
    private var selectedAlbum: PickerAlbum? = null
    private var listener: Listener? = null

    // [s] View
    private var rvContents: RecyclerView? = null

    fun setSelectedItem(item: PickerAlbum?) : SelectionAlbumBottomSheet {
        selectedAlbum = item
        return this
    }

    fun setData(list: List<PickerAlbum>): SelectionAlbumBottomSheet {
        dataList.clear()
        dataList.addAll(list)
        return this
    }

    fun setListener(l: Listener): SelectionAlbumBottomSheet {
        listener = l
        return this
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.SelectionAlbumBottomSheet)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.d_selection_album, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view)
    }

    private fun initView(
        view:View
    ) {
        rvContents = view.findViewById<RecyclerView?>(R.id.rvContents).apply {
            layoutManager = LinearLayoutManager(context)
        }
    }

    /**
     * BottomSheet Show
     * @param fm FragmentManager
     */
    fun simpleShow(fm: FragmentManager) {
        runCatching {
            // 이미 보여지고 있는 Dialog 인경우 스킵
            if (!isAdded) {
                super.show(fm, "PhotoPickerBottomSheet")
            }
        }
    }
}
