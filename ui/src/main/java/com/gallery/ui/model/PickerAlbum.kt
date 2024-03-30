package com.gallery.ui.model

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import com.gallery.core.model.GalleryFilterData
import java.text.DecimalFormat

/**
 * Description : PhotoPicker 전용 Album Data
 *
 * Created by juhongmin on 3/29/24
 */
data class PickerAlbum(
    val id: String,
    val name: String,
    val imagePath: String,
    val count: Int
) {
    constructor(data: GalleryFilterData) : this(
        id = data.bucketId,
        name = data.bucketName,
        imagePath = data.photoUri,
        count = data.count
    )

    fun getTitle(): SpannableStringBuilder {
        val ssb = SpannableStringBuilder()
        ssb.append(name)
        ssb.append(" ")
        ssb.append(
            DecimalFormat("###,###").format(count), ForegroundColorSpan(
                Color.parseColor("#A3A3A3")
            ), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return ssb
    }
}
