package com.gallery.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import androidx.annotation.MainThread
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import timber.log.Timber

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

    private val imgView: AppCompatImageView
    private val guideLineView : GalleryGuideLineView

    init {
        // ImageView 추가
        imgView = AppCompatImageView(ctx).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = this@GalleryEditView.id
                bottomToBottom = this@GalleryEditView.id
            }
            adjustViewBounds = true
            scaleType = ScaleType.MATRIX
        }

        // GuidLine 추가
        val halfWidth = context.resources.displayMetrics.widthPixels / 2
        Timber.d("HalfWidth $halfWidth")
        guideLineView = GalleryGuideLineView(ctx).apply {
            layoutParams = LayoutParams(
                halfWidth,
                halfWidth
            ).apply {
                rightToRight = this@GalleryEditView.id
                leftToLeft = this@GalleryEditView.id
                topToTop = this@GalleryEditView.id
                bottomToBottom = this@GalleryEditView.id
            }
        }

        addView(imgView)
        addView(guideLineView)
    }

    @MainThread
    fun setImageBitmap(bitmap: Bitmap?) {
        if (bitmap == null) return

        imgView.setImageBitmap(bitmap)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if(canvas == null) return

        drawGuidLine(canvas)
    }

    private fun drawGuidLine(canvas: Canvas){

    }
}
