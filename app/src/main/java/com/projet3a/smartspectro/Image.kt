package com.projet3a.smartspectro

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.opencv.core.MatOfPoint
import org.opencv.core.Size
import java.util.ArrayList

class Image(bitmap: Bitmap) {
    private var image: Mat

    fun filtreMedian() {
        val img_filtrer = Mat()
        //Filtre median + seuillage
        Imgproc.medianBlur(image, img_filtrer, 9)
        Imgproc.threshold(img_filtrer, img_filtrer, 20.0, 255.0, Imgproc.THRESH_BINARY)
        //elmenent structurant du morphing
        val element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(200.0, 50.0))
        /*Fermeture*/Imgproc.dilate(img_filtrer, img_filtrer, element)
        Imgproc.erode(img_filtrer, img_filtrer, element)
        val hierarchey = Mat()
        val contours: List<MatOfPoint> = ArrayList()
        Imgproc.findContours(
            img_filtrer,
            contours,
            hierarchey,
            Imgproc.RETR_TREE,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        var max_area = 0.0
        var contours_max = 0
        for (i in contours.indices) {
            //Calculating the area
            val cont_area = Imgproc.contourArea(contours[i])
            println(cont_area)
            if (cont_area > max_area) {
                contours_max = i
                max_area = cont_area
            }
        }
        val rect_mini = Imgproc.boundingRect(contours[contours_max])
        val image_output = image.submat(rect_mini)
    }

    fun canny(): Bitmap {
        val result_canny = Mat()
        Imgproc.Canny(image, result_canny, 10.0, 150.0)
        val hierarchey = Mat()
        val contours: List<MatOfPoint> = ArrayList()
        Imgproc.findContours(
            result_canny,
            contours,
            hierarchey,
            Imgproc.RETR_TREE,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        var max_area = 0.0
        var contours_max = 0
        for (i in contours.indices) {
            //Calculating the area
            val cont_area = Imgproc.contourArea(contours[i])
            println(cont_area)
            if (cont_area > max_area) {
                contours_max = i
                max_area = cont_area
            }
        }
        val rect_mini = Imgproc.boundingRect(contours[contours_max])
        val image_output = image.submat(rect_mini)
        val result = Bitmap.createBitmap(
            image_output.width(),
            image_output.height(),
            Bitmap.Config.ARGB_8888
        )
        Utils.matToBitmap(image_output, result)
        return result
    }

    init {
        val height = bitmap.height
        val width = bitmap.width
        image = Mat()
        Utils.bitmapToMat(bitmap, image)
        for (row in 0 until height) {
            for (col in 0 until width) image.put(row, col, bitmap.getPixel(row, col).toDouble())
        }
    }
}