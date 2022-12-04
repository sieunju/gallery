package com.gallery.edit

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.animation.doOnCancel
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.gallery.edit.detector.FlexibleStateItem
import com.gallery.edit.internal.dp

/**
 * Description :
 *
 * Created by juhongmin on 2022/12/03
 */
class FlexibleImageEditGuideView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(ctx, attrs, defStyleAttr), FlexibleImageGuideListener {

    private var imageView: FlexibleImageEditView? = null

    private val alphaAnimation: ObjectAnimator by lazy { initAlphaAnimation() }

    private val guideLine: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
            strokeWidth = 1F.dp
        }
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawGuideLine(canvas)
    }

    private fun initAlphaAnimation(): ObjectAnimator {
        return ObjectAnimator.ofFloat(this, ALPHA, alpha, 0F).apply {
            duration = 1000
            interpolator = FastOutSlowInInterpolator()
            doOnCancel { alpha = 1.0F }
        }
    }

    fun setImageEditView(view: FlexibleImageEditView) {
        imageView = view
        view.guideListener = this
    }

    override fun onUpdateItem(newItem: FlexibleStateItem) {
        invalidate()
    }

    private fun drawGuideLine(canvas: Canvas?) {
        if (canvas == null) return
        val locationRect = imageView?.computeImageLocation() ?: return
        // Width Guide Location | -- "|" -- "|" -- |
        val standWidth = locationRect.width() / 3F
        val standHeight = locationRect.height() / 3F
        val widthGuides = listOf(
            locationRect.left.plus(standWidth),
            locationRect.left.plus(standWidth * 2)
        )
        val heightGuides = listOf(
            locationRect.top.plus(standHeight),
            locationRect.top.plus(standHeight * 2)
        )
        // |--'|'--|--|
        //  --'|'--|--
        // |--'|'--|--|
        //  --'|'--|--
        // |--'|'--|--|
        canvas.drawLine(
            widthGuides[0],
            locationRect.top,
            widthGuides[0],
            locationRect.bottom,
            guideLine
        )

        // |--|--'|'--|
        //  --|--'|'--
        // |--|--'|'--|
        //  --|--'|'--
        // |--|--'|'--|
        canvas.drawLine(
            widthGuides[1],
            locationRect.top,
            widthGuides[1],
            locationRect.bottom,
            guideLine
        )

        // |--|--|--|
        // ===|==|===
        // |--|--|--|
        // ---|--|---
        // |--|--|--|
        canvas.drawLine(
            locationRect.left,
            heightGuides[0],
            locationRect.right,
            heightGuides[0],
            guideLine
        )

        // |--|--|--|
        // ---|--|---
        // |--|--|--|
        // ===|==|===
        // |--|--|--|
        canvas.drawLine(
            locationRect.left,
            heightGuides[1],
            locationRect.right,
            heightGuides[1],
            guideLine
        )

        if (alphaAnimation.isRunning) {
            alphaAnimation.cancel()
            alphaAnimation.start()
        } else {
            alphaAnimation.start()
        }
    }
}