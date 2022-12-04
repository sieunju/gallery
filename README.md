> Android Gallery Library.  
> [![](https://jitpack.io/v/sieunju/gallery.svg)](https://jitpack.io/#sieunju/gallery).  
> Referred to the https://github.com/ArthurHub/Android-Image-Cropper library. @see CropImageEditView

![AndroidMinSdkVersion](https://img.shields.io/badge/minSdkVersion-21-green.svg) ![AndroidTargetSdkVersion](https://img.shields.io/badge/targetSdkVersion-32-brightgreen.svg)
---

## HowTo

- Project Gradle

```groovy
allprojects {
	    repositories {
		    ...
		    maven { url 'https://jitpack.io' }
	    }
}
```

- We have classified it by 'features' so that only the necessary features can be added and used. (AKA. Multi-module)

### Core Module

```groovy
dependencies {
    	implementation 'com.github.sieunju.gallery:core:$latestVersion'
}
```

- GalleryProviderImpl Example
```kotlin
Used Hilt
import com.gallery.core.Factory
import com.gallery.core.GalleryProvider 
@InstallIn(SingletonComponent::class)
@Module
internal class CoreModule {
    @Singleton
    @Provides
    fun provideGalleryCore(
        @ApplicationContext context: Context
    ): GalleryProvider = Factory.create(context)
}
```

### Edit Module

```groovy
dependencies {
    	implementation 'com.github.sieunju.gallery:edit:$latestVersion'
}
```

- FlexibleImageEditView.  
    - This is a view class that allows you to zoom in, zoom out, and move through gestures.
    - When the image is out of the area, there is a logic to reposition it. It's similar to adding an Instagram story.
    - Preview.  
    
    ![ezgif com-gif-maker](https://user-images.githubusercontent.com/33802191/205472555-85e07b24-cac3-49b2-af01-b5bc225df7f8.gif)

- CropImageEditView
    - Referred to the https://github.com/ArthurHub/Android-Image-Cropper Light Version!!
    
