package ru.futurobot.longimageswithglide.liglide

import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.util.DisplayMetrics
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.load.Key
import ru.futurobot.longimageswithglide.App
import ru.futurobot.longimageswithglide.misc.BitmapLruCache
import ru.futurobot.longimageswithglide.misc.MathUtils
import java.io.File
import java.io.FileDescriptor
import java.io.InputStream
import java.lang.ref.WeakReference
import java.security.MessageDigest
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Alexey on 20.11.2015.
 */
public class TileImageDrawable private constructor(imageView: ImageView, decoder: BitmapRegionDecoder, screenNail: Bitmap) : Drawable() {

    public companion object {
        public val TAG = TileImageDrawable.javaClass.simpleName
        private val TILE_SIZE_DENSITY_HIGH = 256
        private val TILE_SIZE_DENSITY_DEFAULT = 128
        // Instance ids are used to identify a cache hit for a specific instance of TileBitmapDrawable on the shared BitmapLruCache
        private val instanceIds = AtomicInteger(1)
        private val instanceId = instanceIds.andIncrement

        public fun attachToView(imageView: ImageView, imageFile : FileDescriptor, placeHolder: Drawable){
            InitializationTask(imageView, placeHolder).execute(imageFile)
        }
    }

    // The reference of the parent ImageView is needed in order to get the Matrix values and determine the visible area
    private val parentView: WeakReference<ImageView>
    private val localViewVisibleRect = Rect()
    private var mRegionDecoder: BitmapRegionDecoder? = null
    private val mDecodeQueue = LinkedBlockingQueue<Tile>()
    private val mDecoderWorker: DecoderWorker
    private var mIntrinsicWidth: Int = 0
    private var mIntrinsicHeight: Int = 0
    private val mTileSize: Int
    private val mScreenNail: Bitmap
    private val mPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val mMatrixValues = FloatArray(9)
    private val mLastMatrixValues = FloatArray(9)
    private val mTileRect = Rect()
    private val mVisibleAreaRect = Rect()
    private val mScreenNailRect = Rect()
    private var matrix: Matrix? = null

    init {
        parentView = WeakReference(imageView)
        synchronized(decoder) {
            mRegionDecoder = decoder
            mIntrinsicWidth = decoder.width
            mIntrinsicHeight = decoder.height
        }

        val displayMetrics = App.displayMetrics
        mTileSize = if (displayMetrics != null && displayMetrics!!.densityDpi >= DisplayMetrics.DENSITY_HIGH) TILE_SIZE_DENSITY_HIGH else TILE_SIZE_DENSITY_DEFAULT
        mScreenNail = screenNail

        mDecoderWorker = DecoderWorker(this, mRegionDecoder!!, mDecodeQueue)
        mDecoderWorker.start()
    }

    override fun draw(canvas: Canvas?) {
        var parentView: ImageView = parentView.get() ?: return
        val parentViewWidth = parentView.width
        val parentViewHeight = parentView.height


        parentView.getLocalVisibleRect(localViewVisibleRect)
        //Weird bug here localVisibleRect.bottom > height. For some reason getLocalVisibleRect set wrong size to rectangle if it not visible and scrolls down
        if (localViewVisibleRect.top >= localViewVisibleRect.bottom || localViewVisibleRect.bottom < 0 || localViewVisibleRect.bottom > parentViewHeight) {
            return //We are invisible
        }
        mVisibleAreaRect.set(localViewVisibleRect)

        matrix = parentView.imageMatrix

        matrix?.getValues(mMatrixValues)
        val translationX = mMatrixValues[Matrix.MTRANS_X]
        val translationY = mMatrixValues[Matrix.MTRANS_Y]
        val scale = mMatrixValues[Matrix.MSCALE_X]

        // If the matrix values have changed, the decode queue must be cleared in order to avoid decoding unused tiles
        if (translationX != mLastMatrixValues[Matrix.MTRANS_X] || translationY != mLastMatrixValues[Matrix.MTRANS_Y] || scale != mLastMatrixValues[Matrix.MSCALE_X]) {
            mDecodeQueue.clear();
        }
        for(i in 0..8){
            mLastMatrixValues[i] = mMatrixValues[i]
        }

        // The scale required to display the whole Bitmap inside the ImageView. It will be the minimum allowed scale value
        val minScale = Math.min(parentViewWidth / mIntrinsicWidth.toFloat(), parentViewHeight / mIntrinsicHeight.toFloat())

        // The number of allowed levels for this Bitmap. Each subsequent level is half size of the previous one
        val levelCount = Math.max(1, MathUtils.ceilLog2(mIntrinsicWidth / (mIntrinsicWidth * minScale)))

        // sampleSize = 2 ^ currentLevel
        val currentLevel = MathUtils.clamp(MathUtils.floorLog2(1 / scale), 0, levelCount - 1)
        val sampleSize = 1 shl currentLevel

        val currentTileSize: Int = mTileSize * sampleSize
        val horizontalTiles: Int = Math.ceil(mIntrinsicWidth / currentTileSize.toDouble()).toInt()
        val verticalTiles: Int = Math.ceil(mIntrinsicHeight / currentTileSize.toDouble()).toInt()

        var cacheMiss = false
        for (i in 0..horizontalTiles - 1) {
            for (j in 0..verticalTiles - 1) {
                var tileLeft: Int = i * currentTileSize as Int
                var tileTop: Int = j * currentTileSize as Int
                var tileRight: Int = if (((i + 1) * currentTileSize  as Int) <= mIntrinsicWidth as Int) (((i + 1) * currentTileSize as Int)) else mIntrinsicWidth
                var tileBottom: Int = if (((j + 1) * currentTileSize  as Int) <= mIntrinsicHeight as Int) (((j + 1) * currentTileSize as Int)) else mIntrinsicHeight

                mTileRect.set(tileLeft, tileTop, tileRight, tileBottom)

                if (Rect.intersects(mVisibleAreaRect, mTileRect)) {
                    Log.d(TAG, "Tile [$i:$j] intersected")
                    val tile = Tile(instanceId, mTileRect, i, j, currentLevel)

                    var cached: Bitmap? = null
                    synchronized(BitmapLruCache.syncObject) {
                        cached = BitmapLruCache.memoryCache.get(tile.getKey())
                    }
                    if (cached != null) {
                        canvas!!.drawBitmap(cached, null, mTileRect, mPaint)
                    } else {
                        cacheMiss = true

                        synchronized(mDecodeQueue) {
                            if (!mDecodeQueue.contains(tile)) {
                                mDecodeQueue.add(tile)
                            }
                        }

                        val screenNailLeft = Math.round(tileLeft * mScreenNail.width / intrinsicWidth.toFloat())
                        val screenNailTop = Math.round(tileTop * mScreenNail.height / mIntrinsicHeight.toFloat())
                        val screenNailRight = Math.round(tileRight * mScreenNail.width / mIntrinsicWidth.toFloat())
                        val screenNailBottom = Math.round(tileBottom * mScreenNail.height / mIntrinsicHeight.toFloat())
                        mScreenNailRect.set(screenNailLeft, screenNailTop, screenNailRight, screenNailBottom)

                        canvas!!.drawBitmap(mScreenNail, mScreenNailRect, mTileRect, mPaint)
                    }
                } else {
                    Log.d(TAG, "Tile [$i:$j] not intersected")
                }
            }
        }

        // If we had a cache miss, we will need to redraw until all needed tiles have been decoded by our worker thread
        if (cacheMiss) {
            invalidateSelf();
        }
    }

    override fun setAlpha(alpha: Int) {
        val oldAlpha = mPaint.alpha
        if (alpha != oldAlpha) {
            mPaint.alpha = alpha
            invalidateSelf()
        }
    }

    override fun getAlpha(): Int {
        return mPaint.alpha
    }

    override fun getOpacity(): Int {
        if (mScreenNail == null || mScreenNail.hasAlpha() || mPaint.alpha < 255) {
            return PixelFormat.TRANSLUCENT
        }
        return PixelFormat.OPAQUE
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mPaint.setColorFilter(colorFilter)
        invalidateSelf()
    }

    override fun getIntrinsicWidth(): Int {
        return mIntrinsicWidth
    }

    override fun getIntrinsicHeight(): Int {
        return mIntrinsicHeight
    }

    protected fun finalize() {
        mDecoderWorker.quit()
        Log.d(TAG, "finalize")
    }

    private class InitializationTask(val imageView: ImageView, val placeHolder: Drawable) : AsyncTask<Any, Void, Any>() {

        init {
            if (placeHolder != null) {
                imageView.setImageDrawable(placeHolder)
            }
        }

        override fun doInBackground(vararg params: Any?): Any? {
            var decoder: BitmapRegionDecoder
            try {
                if (params[0] is String) {
                    decoder = BitmapRegionDecoder.newInstance(params[0] as String, false)
                } else if (params[0] is FileDescriptor) {
                    decoder = BitmapRegionDecoder.newInstance(params[0] as FileDescriptor, false)
                } else {
                    decoder = BitmapRegionDecoder.newInstance(params[0] as InputStream, false)
                }
            } catch(e: Exception) {
                return e
            }
            var displayMetrics = App.displayMetrics
            val minScale = Math.min(displayMetrics!!.widthPixels / decoder.width.toFloat(), displayMetrics!!.heightPixels / decoder.height.toFloat())
            val levelCount = Math.max(1, MathUtils.ceilLog2((decoder.width / (decoder.height * minScale))))

            val screenNailRect = Rect(0, 0, decoder.width, decoder.height)
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            options.inPreferQualityOverSpeed = true
            options.inSampleSize = 1 shl (levelCount - 1)

            var screenNail: Bitmap
            try {
                var bitmap = decoder.decodeRegion(screenNailRect, options)
                screenNail = Bitmap.createScaledBitmap(bitmap, Math.round(decoder.width * minScale), Math.round(decoder.height * minScale), true)
                if (!bitmap.equals(screenNail)) {
                    bitmap.recycle()
                }
            } catch(e: OutOfMemoryError) {
                // We're under memory pressure. Let's try again with a smaller size
                options.inSampleSize = options.inSampleSize shl 1
                screenNail = decoder.decodeRegion(screenNailRect, options);
            }

            try {
                return TileImageDrawable(imageView, decoder, screenNail)
            } catch(e: Exception) {
                return e
            }
        }

        override fun onPostExecute(result: Any?) {
            if (result is TileImageDrawable) {
                imageView.setImageDrawable(result)
            } else if (result is Exception) {
                //tell about error
            } else {
                //tell about error
            }
        }
    }

    /**
     * Tile class
     */
    private class Tile : Key {
        override fun updateDiskCacheKey(messageDigest: MessageDigest?) {
            throw UnsupportedOperationException()
        }

        public val instanceId: Int
        public val tileRect: Rect
        public val horizontalPos: Int
        public val verticalPos: Int
        public val level: Int

        constructor(instanceId: Int, tileRect: Rect, horizontalPos: Int, verticalPos: Int, level: Int) {
            this.instanceId = instanceId
            this.tileRect = Rect()
            this.tileRect.set(tileRect)
            this.horizontalPos = horizontalPos
            this.verticalPos = verticalPos
            this.level = level
        }

        public fun getKey(): String {
            return StringBuilder(20)
                    .append("#")
                    .append(instanceId)
                    .append("#")
                    .append(horizontalPos)
                    .append("#")
                    .append(verticalPos)
                    .append("#")
                    .append(level)
                    .toString()
        }

        override fun hashCode(): Int {
            return getKey().hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this == other) {
                return true
            }
            return false
        }
    }

    /**
     * Thread that loads decoded regions
     */
    private class DecoderWorker(val imageDrawable: WeakReference<TileImageDrawable>, val decoder: BitmapRegionDecoder, val queue: LinkedBlockingQueue<TileImageDrawable.Tile>) : Thread() {
        private var mQuit: Boolean = false
        private val memoryCache = BitmapLruCache.memoryCache

        constructor(imageDrawable: TileImageDrawable, decoder: BitmapRegionDecoder, queue: LinkedBlockingQueue<TileImageDrawable.Tile>)
        : this(WeakReference(imageDrawable), decoder, queue) {
        }

        override fun run() {
            while (true) {
                if (imageDrawable.get() == null) {
                    return
                }

                var tile: Tile
                try {
                    tile = queue.take()
                } catch(e: InterruptedException) {
                    if (mQuit) {
                        return
                    }
                    continue
                }

                Log.d(TAG, "loading tile [${tile.getKey()}] Rect: ${tile.tileRect}")

                var isContinue = false
                synchronized(BitmapLruCache.syncObject) {
                    if (memoryCache.get(tile.getKey()) != null) {
                        isContinue = true
                    }
                }
                if(isContinue){
                    continue
                }

                val options = BitmapFactory.Options()
                options.inPreferredConfig = Bitmap.Config.ARGB_8888
                options.inPreferQualityOverSpeed = true
                options.inSampleSize = (1 shl tile.level)

                var bitmap: Bitmap? = null
                synchronized(decoder) {
                    try {
                        bitmap = decoder.decodeRegion(tile.tileRect, options)
                    } catch(e: OutOfMemoryError) {
                        // Skip for now. The screenNail will be used instead
                    }
                }

                if (bitmap == null) {
                    continue
                }

                synchronized(BitmapLruCache.syncObject) {
                    memoryCache.put(tile.getKey(), bitmap)
                }
            }
        }

        fun quit() {
            mQuit = true
            interrupt()
        }

    }
}