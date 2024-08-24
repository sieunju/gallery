package com.gallery.ui.internal.view

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import com.gallery.ui.R

/**
 * Description : MaxHeight RecyclerView
 *
 * Created by juhongmin on 2024. 8. 15.
 */
class MaxHeightRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private var maxHeight: Int = 0

    init {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.MaxHeightRecyclerView
        ).runCatching {
            maxHeight = getDimensionPixelSize(
                R.styleable.MaxHeightRecyclerView_maxHeight,
                0
            )
            recycle()
        }
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        var newHeightSpec = heightSpec
        if (maxHeight > 0) {
            newHeightSpec = MeasureSpec.makeMeasureSpec(
                maxHeight,
                MeasureSpec.AT_MOST
            )
        }
        super.onMeasure(widthSpec, newHeightSpec)
    }
}