package com.gallery.ui.internal

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory

internal fun View.changeVisible(isVisible: Boolean) {
    changeVisible(if (isVisible) View.VISIBLE else View.GONE)
}

internal fun View.changeVisible(changeVisible: Int) {
    if (visibility != changeVisible) {
        visibility = changeVisible
    }
}

internal val crossFadeTransition: DrawableTransitionOptions by lazy {
    DrawableTransitionOptions
        .withCrossFade(
            DrawableCrossFadeFactory
                .Builder()
                .setCrossFadeEnabled(true)
                .build()
        )
}

internal val placeHolder: ColorDrawable by lazy {
    ColorDrawable(Color.parseColor("#eeeeee"))
}

internal val Int.dp: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()

internal val Float.dp: Float
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics
    )

internal fun Context.getDeviceWidth(): Int {
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        windowManager.currentWindowMetrics.bounds.width()
    } else {
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        displayMetrics.widthPixels
    }
}

/**
 * 비트연산자 And 타입
 * @param value 비교하고싶은 수
 * @param compare 기준점
 * @return true 해당 Value 비트안에는 Standard 가 속해 있습니다. false 속해 있지 않습니다.
 */
internal fun isAndOperatorTrue(value: Int, compare: Int): Boolean {
    return value and compare == compare
}

/**
 * View SetBackground
 * @param colorHexCode Color HexCode ex.) #0091EA
 * @param corner Radius
 */
internal fun View.setCornerAndBgColor(
    colorHexCode: String,
    corner: Float,
    appendPredicate: (GradientDrawable.() -> Unit)? = null
) {
    val drawable = GradientDrawable(
        GradientDrawable.Orientation.BL_TR,
        intArrayOf(Color.parseColor(colorHexCode), Color.parseColor(colorHexCode))
    )
    drawable.cornerRadius = corner
    appendPredicate?.invoke(drawable)
    background = drawable
    clipToOutline = true
}
