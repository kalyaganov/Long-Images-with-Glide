package ru.futurobot.longimageswithglide.liglide

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.widget.ImageView
import ru.futurobot.longimageswithglide.misc.Size

/**
 * Created by Alexey on 15.11.15.
 */
public class LongImageView : ImageView {

    public constructor(context: Context) : super(context) {
    }

    public constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    public constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    public constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defTheme: Int) : super(context, attrs, defStyleAttr, defTheme) {
    }

    //Size detector callback
    var sizeDetectorCallback = object : SizeDetector.Callback {
        override fun success(url: String, size: Size) {
            if (imgData?.url == url) {
                measureCallback?.onMeasure(imgData!!.url, size)
                imgData = LongImageData(url, size)
                displayImage()
            }
        }

        override fun failure() {
            //Do nothing?
            setImageDrawable(ColorDrawable(0xff0000))   //red
        }
    }

    //Measure callback
    var measureCallback: MeasureCallback? = null
    //Size detector async task
    private var sizeDetectorTask: SizeDetector? = null
    //Long image data
    private var imgData: LongImageData? = null

    /**
     * Display image with given size and url
     */
    public fun displayImage(url: String, size: Size) {
        imgData = LongImageData(url, size)
        displayImage()
    }

    /**
     * Internal method to display image
     */
    private fun displayImage() {
        if (imgData != null) {
            //image must have both width and height size
            if (imgData!!.size.hasBothSize()) {
                //check if we are getting this image size right now and get size
                if (sizeDetectorTask == null || sizeDetectorTask!!.url != imgData!!.url || !sizeDetectorTask!!.isRunning())
                    sizeDetectorTask = SizeDetector(context, imgData!!.url, sizeDetectorCallback)
                sizeDetectorTask!!.execute()
            } else {
                //resize view
                val lparams = layoutParams
                val imgRatio = this.width / imgData!!.size.width
                lparams.height = imgData!!.size.height * imgRatio
                setImageDrawable(ColorDrawable(0xf00ff0))
                //Add tiles for images
                //Sources https://github.com/bumptech/glide/issues/700
                layoutParams = lparams  //update layout
            }
        }
    }

    /**
     * Image data
     */
    private data class LongImageData(val url: String, val size: Size = Size(0, 0))

    /**
     * Callback for measured images
     */
    interface MeasureCallback {
        fun onMeasure(url: String, size: Size)
    }
}