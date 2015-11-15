package ru.futurobot.longimageswithglide.liglide

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.AsyncTask
import com.bumptech.glide.Glide
import ru.futurobot.longimageswithglide.misc.Size

/**
 * Created by Alexey on 15.11.15.
 */
class SizeDetector(val context: Context, val url: String, val callback: SizeDetector.Callback) : AsyncTask<Void, Void, Size>() {

    override fun doInBackground(vararg params: Void?): Size? {
        try {
            var image = Glide.with(context)
                    .load(url)
                    .downloadOnly(com.bumptech.glide.request.target.Target.SIZE_ORIGINAL,
                            com.bumptech.glide.request.target.Target.SIZE_ORIGINAL)
                    .get();
            var options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(image.absolutePath, options)
            return Size(options.outWidth, options.outHeight)
        } catch(ignore: Exception) {
            return null
        }
    }

    override fun onPostExecute(result: Size?) {
        if (result == null) {
            callback.failure()
        } else {
            callback.success(url, result!!)
        }
    }

    interface Callback {
        fun success(url: String, size: Size)
        fun failure()
    }

    fun isRunning(): Boolean {
        return status == AsyncTask.Status.RUNNING
    }
}