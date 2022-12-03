package com.gallery.example

import com.google.gson.JsonElement
import io.reactivex.rxjava3.core.Single
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Description :
 *
 * Created by juhongmin on 2022/09/16
 */
interface CoreApiService {

    @Multipart
    @POST("/api/uploads")
    fun uploads(
        @Part file: List<MultipartBody.Part>
    ): Single<JsonElement>
}
