package com.projet3a.smartspectro;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.*;

public class Image {

    private Mat image;

    public Image(Mat mat) {
        image = mat;
    }

    public Mat filtreMedian() {
        Mat img_filtrer = new Mat();
        //Filtre median + seuillage
        Imgproc.medianBlur(image, img_filtrer, 9);
        Imgproc.threshold(img_filtrer, img_filtrer, 20, 255, Imgproc.THRESH_BINARY);
        //elmenent structurant du morphing
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(200, 50));
        /*Fermeture*/
        Imgproc.dilate(img_filtrer, img_filtrer, element);
        Imgproc.erode(img_filtrer, img_filtrer, element);
        //Canny
        Mat result_canny = new Mat();
        Imgproc.Canny(image, result_canny, 10, 150);
        Mat hierarchey = new Mat();
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(img_filtrer, contours, hierarchey, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Mat draw = Mat.zeros(image.size(), CvType.CV_8UC3);
        double max_area = 0;
        int contours_max = 0;
        for (int i = 0; i < contours.size(); i++) {
            Scalar color = new Scalar(0, 0, 255);
            //Calculating the area
            double cont_area = Imgproc.contourArea(contours.get(i));
            System.out.println(cont_area);
            if (cont_area > max_area) {
                contours_max = i;
                max_area = cont_area;
            }
        }
        Rect rect_mini = Imgproc.boundingRect(contours.get(contours_max));
        Mat resImage = image.submat(rect_mini);
        return resImage;
    }

}
