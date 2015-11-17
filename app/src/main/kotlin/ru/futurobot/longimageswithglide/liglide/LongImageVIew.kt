package ru.futurobot.longimageswithglide.liglide

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.ImageView
import ru.futurobot.longimageswithglide.misc.Size

/**
 * Created by Alexey on 15.11.15.
 * ImageView that can handle displaying long images in ScrollView
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

    /**
     * Cached view`s visible rect on the screen
     */
    private var localVisibleRect: Rect = Rect()
    private var longImageDrawable : LongImageDrawable?

    /**
     * Image view scroll observer
     */
    var scrollObserver: ViewTreeObserver.OnScrollChangedListener = object : ViewTreeObserver.OnScrollChangedListener {
        override fun onScrollChanged() {
            getLocalVisibleRect(localVisibleRect)
            //Weird bug here localVisibleRect.bottom > height. For some reason getLocalVisibleRect set wrong size to rectangle if it not visible and scrolls down
            if (localVisibleRect.top >= localVisibleRect.bottom || localVisibleRect.bottom < 0 || localVisibleRect.bottom > height) {
                Log.i("LongImageView onScrollChanged${hashCode()}", "invisible")
            } else {
                Log.i("LongImageView onScrollChanged${hashCode()}", "Local visible rect: $localVisibleRect ${imgData?.url?.hashCode()} w:${this@LongImageView.width} h:$height")
                longImageDrawable?.onVisibleRectUpdated(localVisibleRect)
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
        override fun success(url: String, size: Size) {
            if (imgData?.url == url) {
                measureCallback?.onMeasure(imgData!!.url, size)
                imgData = LongImageData(url, size)
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
        imgData = LongImageData(url, size)
        displayImage()
    }

    /**
     * Internal method to display image
     */
    private fun displayImage() {
        if (imgData != null) {
            //image must have both width and height size
            if (!imgData!!.size.hasBothSize()) {
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
                longImageDrawable = LongImageDrawable(context, imgData!!.url, Size(this.width, lparams.height), imgData!!.size)
                setImageDrawable(longImageDrawable)
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