package com.gallery.ui.internal

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Description : Grid Span 3 ItemDecoration
 *
 * Created by juhongmin on 3/23/24
 */
internal class GridItemDecoration(
    private val spacing: Int
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position: Int = parent.getChildAdapterPosition(view)

        if (position >= 0) {
            val column = position % 3 // item column
            outRect.left = spacing - column * spacing / 3
            outRect.right = (column.plus(1)) * spacing / 3
            if (position < 3) {
                outRect.top = spacing
            }
            outRect.bottom = spacing
        } else {
            outRect.left = 0
            outRect.right = 0
            outRect.top = 0
            outRect.bottom = 0
        }
    }
}