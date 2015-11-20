package ru.futurobot.longimageswithglide.liglide

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import com.bumptech.glide.Glide
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.model.ImageVideoWrapper
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import java.io.File

/**
 * Abstract bitmap region decoder
 */
abstract class RegionResourceDecoder<T>(val bitmapPool: BitmapPool, val region: Rect) : ResourceDecoder<T, Bitmap> {

    constructor(context: Context, region: Rect) : this(Glide.get(context).bitmapPool, region) {
    }

    override fun decode(source: T, width: Int, height: Int): Resource<Bitmap>? {
        val options = BitmapFactory.Options()
        // Algorithm from Glide's Downsampler.getRoundedSampleSize
        var sampleSize = Math.ceil(region.width().toDouble() / width).toInt()
        sampleSize = if (sampleSize == 0) 0 else Integer.highestOneBit(sampleSize)
        sampleSize = Math.max(1, sampleSize)
        options.inSampleSize = sampleSize

        var decoder = createDecoder(source, width, height)
        var bitmap = decoder.decodeRegion(region, options)
        // probably not worth putting it into the pool because we'd need to get from the pool too to be efficient
        return BitmapResource.obtain(bitmap, bitmapPool)
    }

    abstract fun createDecoder(source: T, width: Int, height: Int): BitmapRegionDecoder

    override fun getId(): String? {
        return javaClass.name + region
    }
}

/**
 * Region decoder for images and video
 */
class RegionImageVideoDecoder(context: Context, region: Rect) : RegionResourceDecoder<ImageVideoWrapper>(context, region) {

    override fun createDecoder(source: ImageVideoWrapper, width: Int, height: Int): BitmapRegionDecoder {
        try {
            return BitmapRegionDecoder.newInstance(source.getStream(), false)
        } catch(ignore: Exception) {
            return BitmapRegionDecoder.newInstance(source.fileDescriptor.fileDescriptor, false)
        }
    }

}

/**
 * Prebuilded Region decoder for images and video
 */
class PrebuildedRegionImageVideoDecoder(context: Context, val decoder: BitmapRegionDecoder, region: Rect) : RegionResourceDecoder<ImageVideoWrapper>(context, region) {

    override fun createDecoder(source: ImageVideoWrapper, width: Int, height: Int): BitmapRegionDecoder {
        return decoder
    }

}

class RegionFileDecoder(context: Context, region: Rect) : RegionResourceDecoder<File>(context, region) {

    override fun createDecoder(source: File, width: Int, height: Int): BitmapRegionDecoder {
        return BitmapRegionDecoder.newInstance(source.absolutePath, false)
    }

}