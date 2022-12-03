package com.gallery.edit.enums

/**
 * Options for scaling the bounds of cropping image to the bounds of Crop Image View.<br></br>
 * Note: Some options are affected by auto-zoom, if enabled.
 */
enum class ScaleType {

    /**
     * Scale the image uniformly (maintain the image's aspect ratio) to fit in crop image view.<br></br>
     * The largest dimension will be equals to crop image view and the second dimension will be
     * smaller.
     */
    FIT_CENTER,

    /**
     * Center the image in the view, but perform no scaling.<br></br>
     * Note: If auto-zoom is enabled and the source image is smaller than crop image view then it
     * will be scaled uniformly to fit the crop image view.
     */
    CENTER,

    /**
     * Scale the image uniformly (maintain the image's aspect ratio) so that both dimensions (width
     * and height) of the image will be equal to or **larger** than the corresponding dimension
     * of the view (minus padding).<br></br>
     * The image is then centered in the view.
     */
    CENTER_CROP,

    /**
     * Scale the image uniformly (maintain the image's aspect ratio) so that both dimensions (width
     * and height) of the image will be equal to or **less** than the corresponding dimension of
     * the view (minus padding).<br></br>
     * The image is then centered in the view.<br></br>
     * Note: If auto-zoom is enabled and the source image is smaller than crop image view then it
     * will be scaled uniformly to fit the crop image view.
     */
    CENTER_INSIDE
}
