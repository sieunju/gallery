package com.gallery.ui.model

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import java.text.DecimalFormat

/**
 * Description : PhotoPicker 전용 Album Data
 *
 * Created by juhongmin on 3/29/24
 */
sealed interface PickerAlbum {

    data class Normal(
        val id: String,
        val name: String,
        val imagePath: String,
        val count: Int
    ) : PickerAlbum {

        constructor(data: GalleryFilterData) : this(
            id = data.bucketId,
            name = data.bucketName,
            imagePath = data.photoUri,
            count = data.count
        )

        override fun getTitle(): SpannableStringBuilder {
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

    object OtherApp : PickerAlbum {
        override fun getTitle(): SpannableStringBuilder {
            return SpannableStringBuilder()
        }
    }

    fun getTitle(): SpannableStringBuilder
}
