package ru.futurobot.longimageswithglide.liglide

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import ru.futurobot.longimageswithglide.App
import ru.futurobot.longimageswithglide.misc.Size
import java.lang.ref.WeakReference

/**
 * Created by Alexey on 16.11.2015.
 */
public class LongImageDrawable(context: Context, url: String, viewSize: Size, imageSize: Size) : Drawable() {
    private val TAG: String = this@LongImageDrawable.javaClass.simpleName
    private var imageTiles: Array<ImageTile?> = emptyArray()
    private var fillPaints: Array<Paint> = arrayOf(newPaint(Color.RED), newPaint(Color.GREEN), newPaint(Color.BLUE))
    private var tileBitmapReadyCallback: ImageTile.BitmapReadyCallback = object : ImageTile.BitmapReadyCallback {
        override fun onReady(tile: ImageTile) {
            bitmapWasReady = true
            if (tile.allowDrawFlag && !isInUpdateCycle) {
                invalidateSelf()    //redraw self
            }
        }
    }
    private var isInUpdateCycle: Boolean = false
    private var bitmapWasReady: Boolean = false

    private var grayPaint : Paint = Paint()
    init {
        grayPaint.color = Color.LTGRAY
    }

    init {
        //Setup sizes
        var viewToImageRatio = viewSize.width / imageSize.width.toFloat()
        var screenSize = App.screenSize
        var screenToImageRatio = screenSize.width / imageSize.width.toFloat()
        var minScreenChunkHeight = screenSize.height / 3
        var screenChunkHeight = leastMultiple(screenSize.width / gcd(screenSize.width, imageSize.width), minScreenChunkHeight)
        var imageChunkHeight = Math.round(screenChunkHeight / screenToImageRatio)
        // screen: Point(720, 1280), image: Point(500, 4784), ratio: 1.44, screenChunk: 396 (396.000031), imageChunk: 275 (275)
        // screen: Point(1280, 720), image: Point(7388, 16711), ratio: 0.173254, screenChunk: 320 (320.000000), imageChunk: 1847 (1847.000000)
        Log.d(TAG, "Screen: $screenSize\nImage: $imageSize\nView size: $viewSize\nScreen to image ratio: $screenToImageRatio\nMin screen chunk height: $minScreenChunkHeight\nScreen chunk height: $screenChunkHeight\nImage chunk height: $imageChunkHeight")

        //Setup tiles
        if (imageSize.height <= imageChunkHeight) {
            //We got single tile
            imageTiles = arrayOf(ImageTile(context, url,
                    imageRegionRect = Rect(0, 0, imageSize.width, imageSize.height),
                    viewRect = Rect(0, 0, viewSize.width, viewSize.height),
                    paint = fillPaints[0]))
        } else {
            //We got a lot of tiles
            var fullTilesCount = imageSize.height / imageChunkHeight
            var tilesCount = fullTilesCount + (if (imageSize.height % imageChunkHeight == 0) 0 else 1)
            imageTiles = arrayOfNulls(tilesCount)
            for (i in 0..tilesCount - 1) {
                var top = i * imageChunkHeight
                var bottom = top + (if (i == tilesCount - 1 && fullTilesCount < tilesCount) imageSize.height % imageChunkHeight else imageChunkHeight )
                imageTiles[i] = ImageTile(context, url,
                        imageRegionRect = Rect(0, top, imageSize.width, bottom),
                        viewRect = Rect(0, (top * viewToImageRatio).toInt(), viewSize.width, (bottom * viewToImageRatio).toInt()),
                //        paint = fillPaints[i % fillPaints.size]
                        paint = grayPaint)
                imageTiles[i]!!.bitmapReadyCallback = tileBitmapReadyCallback
            }
        }
//        Log.d(TAG, "TILES:")
//        imageTiles.forEach { tile -> Log.d(TAG, "\tTile: $tile") }
    }

    /**
     * Check tile visibility
     * Add visibility no next invisible tile at the bottom and at the top if need to
     */
    public fun onVisibleRectUpdated(visibleRect: Rect) {
        isInUpdateCycle = true
        bitmapWasReady = false
        var needRedraw: Boolean = false
        imageTiles.forEach { tile ->
            if (Rect.intersects(tile!!.viewRect, visibleRect)) {
                needRedraw = needRedraw || tile.allowDrawFlag == false
                if(tile.allowDrawFlag == false) {
                    tile.loadBitmap()
                }
                tile.allowDrawFlag = true
            } else {
                tile.allowDrawFlag = false
            }
        }
        if (needRedraw || bitmapWasReady) {
            invalidateSelf()
        }
        isInUpdateCycle = false
    }

    private fun newPaint(color: Int): Paint {
        val paint = Paint()
        paint.color = color
        return paint
    }

    override fun draw(canvas: Canvas?) {
        imageTiles.forEach { tile -> tile!!.draw(canvas) }
    }

    override fun setAlpha(alpha: Int) {
        throw UnsupportedOperationException()
    }

    override fun getOpacity(): Int {
        return 0
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        throw UnsupportedOperationException()
    }

    /** Greatest Common Divisor */
    private fun gcd(a: Int, b: Int): Int {
        var ca = a
        var cb = b
        while (cb != 0) {
            var t = cb
            cb = ca % cb
            ca = t
        }
        return ca
    }

    /**
     * @param base positive whole number
     * @param threshold positive whole number
     * @return multiple of base that is >= threshold
     */
    private fun leastMultiple(base: Int, threshold: Int): Int {
        var minMul = Math.max(1, threshold / base)
        return base * minMul
    }
}


/**
 * Image tile class.
 * Handle image region loading and proper drawing on canvas
 */
private data class ImageTile(val context: Context, val url: String, val viewRect: Rect, val imageRegionRect: Rect, val paint: Paint) {
    public var allowDrawFlag: Boolean = false
    public var bitmapReadyCallback: BitmapReadyCallback? = null
    //Weak reference to bitmap. Some kind of little cache inside tile
    private var weakBitmap: WeakReference<Bitmap>? = null
    private var bitmapRect: Rect = Rect()
    private var debugPaint: Paint? = null
    //Glide loading target
    private var glideLoadingTarget: Target<Bitmap> = object : SimpleTarget<Bitmap>() {
        override fun onResourceReady(resource: Bitmap?, glideAnimation: GlideAnimation<in Bitmap>?) {
            weakBitmap = WeakReference<Bitmap>(resource)
            bitmapRect.set(0, 0, resource!!.width, resource!!.height)
            bitmapReadyCallback?.onReady(this@ImageTile)
        }
    }

    init {
        debugPaint = Paint()
        debugPaint!!.color = Color.YELLOW
    }


    /**
     * Tile drawing method
     */
    public fun draw(canvas: Canvas?) {
        if (allowDrawFlag) {
            val bmp = weakBitmap?.get()
            if (bmp != null) {
                canvas!!.drawBitmap(bmp, bitmapRect, viewRect, null)
            } else {
                canvas!!.drawRect(viewRect, paint)
            }
        } else {
            canvas!!.drawRect(viewRect, debugPaint)
        }
    }

    /**
     * Request for load
     */
    public fun loadBitmap() {
        if (weakBitmap?.get() == null) {
            Glide.with(context)
                    .load(url)
                    .asBitmap()
                    // overshoot a little so fitCenter uses width's ratio (see minPercentage)
                    .override(viewRect.width(), viewRect.height())
                    .fitCenter()
                    // Cannot use .imageDecoder, only decoder; see bumptech/glide#708
                    //.imageDecoder(new RegionStreamDecoder(context, rect))
                    .decoder(RegionImageVideoDecoder(context, imageRegionRect))
                    .cacheDecoder(RegionFileDecoder(context, imageRegionRect))
                    // Cannot use RESULT cache; see bumptech/glide#707
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .listener(object : RequestListener<String, Bitmap> {
                        override fun onException(e: Exception?, model: String?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                            Log.e("GLIDE", "Loading image region failed with exception: $e\nTile: ${this@ImageTile}")
                            return false
                        }

                        override fun onResourceReady(resource: Bitmap?, model: String?, target: Target<Bitmap>?, isFromMemoryCache: Boolean, isFirstResource: Boolean): Boolean {
                            Log.i("GLIDE", "Image region loaded and ready. Tile: ${this@ImageTile}")
                            return false
                        }
                    })
                    .into(glideLoadingTarget)

        }
    }

    public interface BitmapReadyCallback {
        fun onReady(tile: ImageTile)
    }
}
