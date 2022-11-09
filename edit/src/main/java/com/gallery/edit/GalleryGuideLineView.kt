package com.gallery.edit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Description : Gallery Edit GuideLine
 *
 * Created by juhongmin on 2022/10/07
 */
internal class GalleryGuideLineView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(ctx, attrs, defStyleAttr) {

    private var edgeOffset: Float = 5F.dp // 가장자리 터치 기준

    private val scaleDetector: ScaleGestureDetector by lazy {
        ScaleGestureDetector(
            ctx,
            ScaleListener()
        )
    }

    private val outLinePaint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = ContextCompat.getColor(ctx, android.R.color.white)
            strokeWidth = 5F.dp
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas == null) return

        drawOutLine(canvas)
    }

    @SuppressLint("Recycle", "ClickableViewAccessibility")
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return false
        performGuideResize(ev)
        return super.dispatchTouchEvent(ev)
    }

    @SuppressLint("Recycle", "ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return false
//        Timber.d("onTouchEvent ${ev.pointerCount} ${ev.x} ${ev.y}")
//        return if (isEnabled) {
//            if (ev.pointerCount > 1) {
//                scaleDetector.onTouchEvent(ev)
//            } else {
//                when (ev.action) {
//                    MotionEvent.ACTION_DOWN -> {
//                        true
//                    }
//                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//                        parent.requestDisallowInterceptTouchEvent(false)
//                        true
//                    }
//                    MotionEvent.ACTION_MOVE -> {
//                        parent.requestDisallowInterceptTouchEvent(true)
//                        true
//                    }
//                    else -> false
//                }
//            }
//        } else false
        return super.onTouchEvent(ev)
    }

    private fun performGuideResize(ev: MotionEvent) {
        if (ev.pointerCount == 1) {
            if (isEdgeTouch(ev.x, ev.y)) {
                val resizeRect = performResizeRect(ev.x, ev.y)
                Timber.d("가장자리 터치중입니다. $resizeRect")
                layoutParams.width = resizeRect.width()
                layoutParams.height = resizeRect.height()
                requestLayout()
            } else {
                Timber.d("가장자리에 터치 안했습니다. ")
            }
        } else {

        }
    }

    private fun performResizeRect(x: Float, y: Float): Rect {
        // 초기값은 현재 너비 / 높이값
        val rect = Rect(0, 0, width, height)
        val _edge = edgeOffset * 2
        if (0F <= x && x < _edge) {
            rect.left = x.plus(edgeOffset).toInt()
        }
        if (width.minus(_edge) <= x && x < width) {
            rect.right = x.minus(edgeOffset).toInt()
        }
        if (0F <= y && y < _edge) {
            rect.top = y.toInt()
        }
        if (height.minus(_edge) <= y && y < height) {
            rect.bottom = y.toInt()
        }
        return rect
    }

    private fun isEdgeTouch(x: Float, y: Float): Boolean {
        val _width = width.toFloat()
        val _height = height.toFloat()
        val _edge = edgeOffset * 2
        // Timber.d("Touch $_edge $x $y")
        // Left Edge or Right Edge
        return if ((0F <= x && x < _edge) || (_width.minus(_edge) <= x && x < _width)) {
            true
        } else if ((0F <= y && y < _edge) || (_height.minus(_edge) <= y && y < _height)) {
            // Top Edge or Bottom Edge
            true
        } else {
            false
        }
    }

    private fun drawOutLine(canvas: Canvas) {
        val rect = RectF()
        rect.top = edgeOffset
        rect.left = edgeOffset
        rect.right = width.minus(edgeOffset)
        rect.bottom = height.minus(edgeOffset)
        canvas.drawRect(rect, outLinePaint)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector?): Boolean {

            return true
        }
    }
}
