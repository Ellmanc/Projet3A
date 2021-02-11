package com.projet3a.smartspectro;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class Image {

    private final Mat image;
    private Rect rect_mini;

    public Image(Mat mat) {
        image = mat;
    }

    public Mat Canny() {
        //Canny
        Mat result_canny = new Mat();
        Imgproc.Canny(image, result_canny, 10, 100, 3, false);
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(200, 50));
        Imgproc.dilate(result_canny, result_canny, element);
        Imgproc.erode(result_canny, result_canny, element);
        Mat hierarchy = new Mat();
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(result_canny, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        double max_area = 0;
        int contours_max = 0;
        for (int i = 0; i < contours.size(); i++) {
            //Calculating the area
            double cont_area = Imgproc.contourArea(contours.get(i));
            System.out.println(cont_area);
            if (cont_area > max_area) {
                contours_max = i;
                max_area = cont_area;
            }
        }
        rect_mini = Imgproc.boundingRect(contours.get(contours_max));
        rect_mini.x -= AppParameters.getInstance().getHeightOrigin() * 0.03;
        rect_mini.width += 2 * (AppParameters.getInstance().getHeightOrigin() * 0.03);
        return image.submat(rect_mini);
    }

    public int getRectOrigin() {
        return rect_mini.x;
    }

    public int getYOrigin() {
        return rect_mini.y;
    }

}
