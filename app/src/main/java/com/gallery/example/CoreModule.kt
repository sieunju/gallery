package com.gallery.example

import android.content.Context
import com.gallery.core.Factory
import com.gallery.core.GalleryProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Description :
 *
 * Created by juhongmin on 2022/09/13
 */
@InstallIn(SingletonComponent::class)
@Module
internal class CoreModule {
    @Singleton
    @Provides
    fun provideGalleryCore(
        @ApplicationContext context: Context
    ): GalleryProvider = Factory.create(context)

    @Singleton
    @Provides
    fun provideCoreApiService(): CoreApiService {
        val client = OkHttpClient.Builder()
            .build()
        return Retrofit.Builder().apply {
            baseUrl("https://cdn.qtzz.synology.me")
            client(client)
            addCallAdapterFactory(RxJava3CallAdapterFactory.createWithScheduler(Schedulers.io()))
            addConverterFactory(GsonConverterFactory.create())
        }.build().create(CoreApiService::class.java)
    }
}
