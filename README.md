# Android Gallery Library (Simple is Best 🤩).
[![](https://jitpack.io/v/sieunju/gallery.svg)](https://jitpack.io/#sieunju/gallery) ![AndroidMinSdkVersion](https://img.shields.io/badge/minSdkVersion-21-green.svg) ![AndroidTargetSdkVersion](https://img.shields.io/badge/targetSdkVersion-32-brightgreen.svg)
---

### HowTo

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

### Example
![Example](https://raw.githubusercontent.com/sieunju/gallery/develop/storage/example_readme.gif)


## Core Module

> [Wiki](https://github.com/sieunju/gallery/wiki/Core-Module-Wiki)

#### How To

```groovy
dependencies {
  implementation 'com.github.sieunju.gallery:core:$latestVersion'
}
```

#### Simple Description
- GalleryProviderImpl Example (Using Hilt Library!!)
```kotlin
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

## Core-rx Module
This module is converted from core module to ReactiveX and processed.

#### How To

```groovy
dependencies {
  implementation 'com.github.sieunju.gallery:core:$$latestVersion' // required
  implementation 'com.github.sieunju.gallery:model:$$latestVersion' // required
  implementation 'com.github.sieunju.gallery:core-rx:$latestVersion' // optional
}
```

## Core-coroutines Module
This module is converted from core module to Coroutines and processed.

#### How To

```groovy
dependencies {
  implementation 'com.github.sieunju.gallery:core:$$latestVersion' // required
  implementation 'com.github.sieunju.gallery:model:$$latestVersion' // required
  implementation 'com.github.sieunju.gallery:core-coroutines:$latestVersion' // optional
}
```

#### java.lang.ClassCastException Error?!
[Issue Link](https://youtrack.jetbrains.com/issue/KT-27105).

[Solution] kotlin version update! 🙏

```groovy
project.gradle
classpath "com.android.tools.build:gradle:7.1.3"
classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10"

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

  ![flexibleimage_example](https://user-images.githubusercontent.com/33802191/205475538-9446da12-e997-49e7-84bb-d88330087ef5.gif)

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

    
