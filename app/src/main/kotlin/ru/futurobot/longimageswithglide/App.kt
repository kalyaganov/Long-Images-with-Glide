package ru.futurobot.longimageswithglide

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
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
    }

    override fun onCreate() {
        super.onCreate()
        screenSize = calculateScreenSize()

        //Cleanup glide
        Thread({
            Glide.get(this).clearDiskCache()
            Glide.get(this).clearMemory()
        }).start()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        screenSize = calculateScreenSize()
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
}