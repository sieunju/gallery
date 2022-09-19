package com.gallery.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.MainThread
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * Description : Gallery Edit Root Layout
 *
 * Created by juhongmin on 2022/09/17
 */
class GalleryEditView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr) {

    private val ivFlexible: FlexibleImageView

    init {
        // ImageView 추가
        ivFlexible = FlexibleImageView(ctx).apply {
            layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    topToTop = this@GalleryEditView.id
                    bottomToBottom = this@GalleryEditView.id
                }
            adjustViewBounds = true
            setBackgroundColor(Color.BLACK)
        }
        addView(ivFlexible)
    }

    @MainThread
    fun setImageBitmap(bitmap: Bitmap?) {
        ivFlexible.loadBitmap(bitmap)
    }
}
