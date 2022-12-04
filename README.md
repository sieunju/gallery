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

## Core Module

> [Wiki](https://github.com/sieunju/gallery/wiki/Core-Module-Wiki)

#### How To

```groovy
dependencies {
    	implementation 'com.github.sieunju.gallery:core:$latestVersion'
}
```

#### Simple Description
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
   
   
## Edit Module

> [Wiki](https://github.com/sieunju/gallery/wiki/Edit-Module-Wiki)

#### How To

```groovy
dependencies {
    	implementation 'com.github.sieunju.gallery:edit:$latestVersion'
}
```

#### Simple Description
- FlexibleImageEditView.  
    - This is a view class that allows you to zoom in, zoom out, and move through gestures.
    - When the image is out of the area, there is a logic to reposition it. It's similar to adding an Instagram story.
    - Preview.  
    
    ![ezgif com-gif-maker](https://user-images.githubusercontent.com/33802191/205472555-85e07b24-cac3-49b2-af01-b5bc225df7f8.gif)

- CropImageEditView.
    - Referred to the https://github.com/ArthurHub/Android-Image-Cropper Light Version!!
    - Preview.   
    ![crop_image_edit_view](https://user-images.githubusercontent.com/33802191/205473714-c513d8e8-9ab8-436c-99cd-3a2775620933.gif)


## Ui Module

> [Wiki](https://github.com/sieunju/gallery/wiki/Ui-Module-Wiki)
#### How To

```groovy
dependencies {
    	implementation 'com.github.sieunju.gallery:ui:$latestVersion'
}
```

#### Simple Description.  

- GalleryRecyclerView.   
    ![gallery_recyclerview_example](https://user-images.githubusercontent.com/33802191/205474967-a3146c32-35b7-40cf-98e5-7ed2d380357c.gif)

    
