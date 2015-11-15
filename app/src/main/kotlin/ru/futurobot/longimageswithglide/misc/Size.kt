package ru.futurobot.longimageswithglide.misc

/**
 * Created by Alexey on 15.11.15.
 */
public data class Size(val width: Int, val height: Int) {
    /**
     * Does this size has width and height?
     */
    fun hasBothSize(): Boolean {
        return width != 0 && height != 0
    }
}
