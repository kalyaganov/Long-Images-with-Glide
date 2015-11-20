package ru.futurobot.longimageswithglide.liglide

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.ImageView
import ru.futurobot.longimageswithglide.misc.Size
import java.io.File
import java.io.FileDescriptor

/**
 * Created by Alexey on 15.11.15.
 * ImageView that can handle displaying long images in ScrollView
 * See https://github.com/bumptech/glide/issues/700
 */
public class LongImageView : ImageView {

    public companion object {
        public val REGION_LOADER_GLIDE = 1
        public val REGION_LOADER_THREAD = 2
    }

    public constructor(context: Context) : super(context) {
    }

    public constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    }

    public constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    public constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defTheme: Int) : super(context, attrs, defStyleAttr, defTheme) {
    }

    /**
     * Region loader mode
     */
    public var regionLoader = REGION_LOADER_GLIDE
    /**
     * Cached view`s visible rect on the screen
     */
    private var localVisibleRect: Rect = Rect()
    private var glideLongImageDrawable: LongImageDrawable? = null
    private var threadLongImageDrawable: LongImageDrawable? = null

    /**
     * Image view scroll observer
     */
    var scrollObserver: ViewTreeObserver.OnScrollChangedListener = object : ViewTreeObserver.OnScrollChangedListener {
        override fun onScrollChanged() {
            if (drawable is LongImageDrawable) {
                getLocalVisibleRect(localVisibleRect)
                //Weird bug here localVisibleRect.bottom > height. For some reason getLocalVisibleRect set wrong size to rectangle if it not visible and scrolls down
                if (localVisibleRect.top >= localVisibleRect.bottom || localVisibleRect.bottom < 0 || localVisibleRect.bottom > height) {
                    //Log.i("LongImageView onScrollChanged${hashCode()}", "invisible")
                } else {
                    //Log.i("LongImageView onScrollChanged${hashCode()}", "Local visible rect: $localVisibleRect ${imgData?.url?.hashCode()} w:${this@LongImageView.width} h:$height")
                    glideLongImageDrawable?.onVisibleRectUpdated(localVisibleRect)
                }
            } else if (drawable is TileImageDrawable) {
                threadLongImageDrawable!!.invalidateSelf()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnScrollChangedListener(scrollObserver)
        post { scrollObserver.onScrollChanged() }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewTreeObserver.removeOnScrollChangedListener(scrollObserver)
    }

    //Size detector callback
    var sizeDetectorCallback = object : SizeDetector.Callback {
        override fun success(file: FileDescriptor, url: String, size: Size) {
            if (imgData?.url == url) {
                measureCallback?.onMeasure(imgData!!.url, size)
                imgData = LongImageData(file, url, size)
                displayImage()
            }
        }

        override fun failure() {
            setImageDrawable(ColorDrawable(0xff0000))   //red Do nothing?
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
        if (regionLoader == REGION_LOADER_GLIDE) {
            imgData = LongImageData(null, url, size)
        } else if (regionLoader == REGION_LOADER_THREAD) {
            imgData = LongImageData(null, url, Size(0, 0))
        }
        displayImage()
    }

    /**
     * Internal method to display image
     */
    private fun displayImage() {
        if (imgData != null) {
            //image must have both width and height size
            if (!imgData!!.size.hasBothSize()) {
                glideLongImageDrawable = null
                setImageDrawable(null)
                //check if we are getting this image size right now and get size
                if (sizeDetectorTask == null || sizeDetectorTask!!.url != imgData!!.url || !sizeDetectorTask!!.isRunning()) {
                    sizeDetectorTask = SizeDetector(context, imgData!!.url, sizeDetectorCallback)
                    sizeDetectorTask!!.execute()
                }
            } else {
                //resize view
                Log.i("displayImage", "Resize view")
                val lparams = layoutParams
                val imgRatio = this.width / imgData!!.size.width.toFloat()
                lparams.height = (imgData!!.size.height * imgRatio).toInt()
                glideLongImageDrawable = LongImageDrawable(context, imgData!!.url, Size(this.width, lparams.height), imgData!!.size)
                if (regionLoader == REGION_LOADER_GLIDE) {
                    setImageDrawable(glideLongImageDrawable)
                } else if (regionLoader == REGION_LOADER_THREAD) {
                    TileImageDrawable.attachToView(this, imgData!!.file!!, ColorDrawable(Color.DKGRAY))
                }
                layoutParams = lparams  //update layout
            }
        }
    }

    /**
     * Image data
     */
    private data class LongImageData(val file: FileDescriptor?, val url: String, val size: Size = Size(0, 0))

    /**
     * Callback for measured images
     */
    interface MeasureCallback {
        fun onMeasure(url: String, size: Size)
    }
}