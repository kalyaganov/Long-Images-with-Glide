package ru.futurobot.longimageswithglide.misc

import android.graphics.Bitmap
import android.os.Build
import android.support.v4.util.LruCache
import ru.futurobot.longimageswithglide.App

/**
 * Created by Alexey on 20.11.2015.
 */
public class BitmapLruCache private constructor(maxSize: Int) : LruCache<String, Bitmap>(maxSize) {
    companion object {
        public val memoryCache: BitmapLruCache by lazy {
            synchronized(syncObject) {
                if (App.displayMetrics == null)
                    return@lazy BitmapLruCache(((Runtime.getRuntime().maxMemory() / 1024) / 8) as Int)
                else
                    return@lazy BitmapLruCache(App.displayMetrics!!.widthPixels * App.displayMetrics!!.heightPixels * 4 * 3)
            }
        }

        public val syncObject = Object()

        private fun getBitmapSize(bitmap: Bitmap?): Int {
            if (bitmap == null) {
                return 0
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return bitmap.allocationByteCount
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                return bitmap.byteCount
            }
            return bitmap.rowBytes * bitmap.height
        }
    }

    override fun sizeOf(key: String?, value: Bitmap?): Int {
        return getBitmapSize(value)
    }
}