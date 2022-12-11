package com.gallery.ui.internal

import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory

internal fun View.changeVisible(changeVisible: Int) {
    if (visibility != changeVisible) {
        visibility = changeVisible
    }
}

internal val crossFadeFactory: DrawableCrossFadeFactory by lazy {
    DrawableCrossFadeFactory
        .Builder()
        .setCrossFadeEnabled(true)
        .build()
}

internal val crossFadeTransition: DrawableTransitionOptions by lazy {
    DrawableTransitionOptions
        .withCrossFade(crossFadeFactory)
}

internal val Int.dp: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()

/**
 * 비트연산자 And 타입
 * @param value 비교하고싶은 수
 * @param compare 기준점
 * @return true 해당 Value 비트안에는 Standard 가 속해 있습니다. false 속해 있지 않습니다.
 */
internal fun isAndOperatorTrue(value: Int, compare: Int): Boolean {
    return value and compare == compare
}