package com.gallery.example

import android.content.Context
import com.gallery.core.Factory
import com.gallery.core.GalleryProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
}
