package ru.futurobot.longimageswithglide.misc

/**
 * Created by Alexey on 20.11.2015.
 */
public class MathUtils private constructor() {
    companion object {
        public fun ceilLog2(value: Float): Int {
            for (i in 0..30) {
                if ((1 shl i) >= value) {
                    return i
                }
            }
            return 0
        }

        public fun floorLog2(value: Float): Int {
            for (i in 0..30) {
                if ((1 shl i) > value) {
                    return i - 1
                }
            }
            return 0
        }

        // Returns the input value x clamped to the range [min, max].
        public fun clamp(x: Int, min: Int, max: Int): Int {
            if (x > max) return max
            if (x < min) return min
            return x
        }

        // Returns the input value x clamped to the range [min, max].
        public fun clamp(x: Float, min: Float, max: Float): Float {
            if (x > max) return max
            if (x < min) return min
            return x
        }

        // Returns the input value x clamped to the range [min, max].
        public fun clamp(x: Long, min: Long, max: Long): Long {
            if (x > max) return max
            if (x < min) return min
            return x
        }
    }
}