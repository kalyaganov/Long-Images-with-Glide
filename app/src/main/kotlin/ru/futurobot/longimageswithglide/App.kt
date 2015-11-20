package ru.futurobot.longimageswithglide

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import com.bumptech.glide.Glide
import ru.futurobot.longimageswithglide.misc.Size

/**
 * Created by Alexey on 16.11.2015.
 */
public class App : Application() {

    companion object {
        public var screenSize: Size = Size(0, 0)
            private set
        public var displayMetrics: DisplayMetrics? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        screenSize = calculateScreenSize()
        displayMetrics = getDisplayMetrics()

        //Cleanup glide
//        Thread({
//            Glide.get(this).clearDiskCache()
//            Glide.get(this).clearMemory()
//        }).start()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        screenSize = calculateScreenSize()
        displayMetrics = getDisplayMetrics()
    }

    private fun calculateScreenSize(): Size {
        var window = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        var display = window.defaultDisplay
        var screen = Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(screen)
        } else {
            screen.set(display.width, display.height)
        }
        return Size(screen.x, screen.y)
    }

    private fun getDisplayMetrics(): DisplayMetrics {
        var window = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        var display = window.defaultDisplay
        var displayMetrics = DisplayMetrics()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealMetrics(displayMetrics)
        } else {
            display.getMetrics(displayMetrics)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                try {
                    displayMetrics.widthPixels = Display::class.javaClass.getMethod("getRawWidth").invoke(display) as Int
                    displayMetrics.heightPixels = Display::class.javaClass.getMethod("getRawHeight").invoke(display) as Int
                    return displayMetrics
                } catch(e: Exception) {

                }
            }
        }
        return displayMetrics
    }
}