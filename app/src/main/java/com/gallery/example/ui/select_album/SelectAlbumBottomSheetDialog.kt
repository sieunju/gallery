package com.gallery.example.ui.select_album

import android.app.ActionBar.LayoutParams
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.FragmentManager
import com.gallery.core.model.GalleryFilterData
import com.gallery.example.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.DecimalFormat

/**
 * Description : 앨범 선택하는 BottomSheet Dialog
 * 예제라 그냥 ScrollView 안에 LinearView 로 처리할 예정
 *
 * Created by juhongmin on 2023/08/15
 */
internal class SelectAlbumBottomSheetDialog : BottomSheetDialogFragment() {
    interface Listener {
        fun onSelectedFilter(data: GalleryFilterData)
    }

    private var listener: Listener? = null
    private val dataList: MutableList<GalleryFilterData> by lazy { mutableListOf() }
    private var currentAlbum: GalleryFilterData? = null

    fun setData(list: List<GalleryFilterData>): SelectAlbumBottomSheetDialog {
        dataList.addAll(list)
        return this
    }

    fun setCurrentAlbum(currentAlbum: GalleryFilterData?): SelectAlbumBottomSheetDialog {
        this.currentAlbum = currentAlbum
        return this
    }

    fun setListener(listener: Listener): SelectAlbumBottomSheetDialog {
        this.listener = listener
        return this
    }

    fun simpleShow(fm: FragmentManager) {
        super.show(fm, "SelectAlbumBottomSheetDialog")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.d_select_album_bottom_sheet_v2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val llContents = view.findViewById<LinearLayoutCompat>(R.id.llContents)
        dataList.forEach { item ->
            val textView = AppCompatTextView(view.context).apply {
                layoutParams = LinearLayoutCompat.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
                setPadding(30, 30, 30, 30)
                setTextColor(if (item == currentAlbum) Color.BLACK else Color.GRAY)
                setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14F)
                text = "${item.bucketName} (${DecimalFormat("#,###").format(item.count)})"
            }
            textView.setOnClickListener {
                if (currentAlbum == item) {
                    Toast.makeText(it.context, "이미 선택한 앨범입니다.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                listener?.onSelectedFilter(item)
                dismiss()
            }
            llContents.addView(textView)
        }
    }
}