package com.gallery.ui.internal.listener

import android.os.SystemClock
import android.view.View

/**
 * Description : 중복 클릭 방지 인터페이스
 *
 * Created by juhongmin on 2024. 8. 26.
 */
fun interface TurtleClickListener {
    fun callback(v: View)
}

fun View.onClick(
    l: TurtleClickListener
) {
    var lastClickTime = 0L
    setOnClickListener { view ->
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime > 1000) {
            lastClickTime = currentTime
            l.callback(view)
        }
    }
}
