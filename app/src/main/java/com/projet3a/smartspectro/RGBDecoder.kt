package com.projet3a.smartspectro

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Created by Rémy Cordeau-Mirani on 20/09/2019.
 */
object RGBDecoder {
    /**
     * Gets for each pixel of Bitmap its RGB encoding
     */
    fun getRGBCode(bitmap: Bitmap, width: Int, height: Int): IntArray {
        val rgb = IntArray(width * height)
        bitmap.getPixels(rgb, 0, width, 0, 0, width, height)
        var r: Int
        var g: Int
        var b: Int
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                r = rgb[index] shr 16 and 0xff
                g = rgb[index] shr 8 and 0xff
                b = rgb[index] and 0xff
                rgb[index] = -0x1000000 or (r shl 16) or (g shl 8) or b //color encoding
            }
        }
        return rgb
    }

    /**
     * Computes for each pixel in array the intensity from its RGB encoding
     */
    fun getImageIntensity(rgb: IntArray): DoubleArray {
        val intensity = DoubleArray(rgb.size)
        for (i in intensity.indices) {
            intensity[i] = 0.2126 * Color.red(rgb[i]) + 0.7152 * Color.green(
                rgb[i]
            ) + 0.0722 * Color.blue(rgb[i])
        }
        return intensity
    }

    /**
     * Calculates the intensity mean on each column for the captured frame
     */
    fun computeIntensityMean(intensity: DoubleArray, width: Int, height: Int): DoubleArray {
        val intensityMean = DoubleArray(width)
        var meanValue = 0.0
        var index: Int
        for (i in intensityMean.indices) {
            index = i
            while (index < intensity.size) { //if the index is defined, we add it to the mean
                meanValue += intensity[index]
                index += width //go to next line value for the considered column
            } //if it is not, it means that we have to change column in our captured picture
            intensityMean[i] = meanValue / height
            meanValue = 0.0
        }
        return intensityMean
    }

}