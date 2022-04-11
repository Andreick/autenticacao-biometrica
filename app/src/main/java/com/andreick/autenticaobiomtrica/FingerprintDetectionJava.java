package com.andreick.autenticaobiomtrica;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class FingerprintDetectionJava {

    private static final String TAG = "FingerprintDetection";

    public Mat processImage(Mat input) {

        Mat grayMat = new Mat();
        Imgproc.cvtColor(input, grayMat, Imgproc.COLOR_RGB2GRAY);

        Mat inputBinary = new Mat();
        Imgproc.threshold(grayMat, inputBinary, 0, 255, 8);

        /*Mat input_thinned = input.clone();
        thinning(input_thinned);*/

        // Now lets detect the strong minutiae using Haris corner detection
        Mat harris_corners, harris_normalised = new Mat();
        harris_corners = Mat.zeros(inputBinary.size(), CvType.CV_32FC1);
        Imgproc.cornerHarris(inputBinary, harris_corners, 2, 3, 0.04, Core.BORDER_DEFAULT);
        Core.normalize(harris_corners, harris_normalised, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC3);

        /*// Select the strongest corners that you want
        int threshold_harris = 125;
        List<KeyPoint> keyPoints = new ArrayList<>();

        // Make a color clone for visualisation purposes
        Mat rescaled = new Mat();
        Core.convertScaleAbs(harris_normalised, rescaled);
        Mat harris_c = new Mat(rescaled.rows(), rescaled.cols(), CvType.CV_8UC3);
        List<Mat> in = Arrays.asList(rescaled, rescaled, rescaled);
        MatOfInt from_to = new MatOfInt(0, 0, 1, 1, 2, 2);
        Core.mixChannels(in, Collections.singletonList(harris_c), from_to);
        for(int x=0; x<harris_normalised.cols(); x++) {
            for(int y=0; y<harris_normalised.rows(); y++) {
                //Log.d(TAG, String.valueOf(harris_normalised.get(y, x)[0]));
                if ( harris_normalised.get(y, x)[0] > threshold_harris ){
                    // Draw or store the keypoint location here, just like you decide. In our case we will store the location of the keypoint
                    Imgproc.circle(harris_c, new Point(x, y), 5, new Scalar(0,255,0), 1);
                    Imgproc.circle(harris_c, new Point(x, y), 1, new Scalar(0,0,255), 1);
                    keyPoints.add(new KeyPoint(x, y, 1));
                }
            }
        }

        Log.d(TAG, String.valueOf(keyPoints.size()));

        // Calculate the ORB descriptor based on the keypoint
        Feature2D orb_descriptor = ORB.create();
        Mat descriptors = new Mat();
        orb_descriptor.compute(input, new MatOfKeyPoint(keyPoints.toArray(new KeyPoint[0])), descriptors);*/

        return harris_normalised;
    }

    private Mat thinningIteration(Mat im, int iteration)
    {
        Mat marker = Mat.zeros(im.size(), CvType.CV_8UC1);
        for (int i = 1; i < im.rows() - 1; i++)
        {
            for (int j = 1; j < im.cols() - 1; j++)
            {
                int p2 = (int) im.get(i-1, j)[0];
                int p3 = (int) im.get(i-1, j+1)[0];
                int p4 = (int) im.get(i, j+1)[0];
                int p5 = (int) im.get(i+1, j+1)[0];
                int p6 = (int) im.get(i+1, j)[0];
                int p7 = (int) im.get(i+1, j-1)[0];
                int p8 = (int) im.get(i, j-1)[0];
                int p9 = (int) im.get(i-1, j-1)[0];

                int a = 0;
                if (p2 == 0 && p3 == 1) a++;
                if (p3 == 0 && p4 == 1) a++;
                if (p4 == 0 && p5 == 1) a++;
                if (p5 == 0 && p6 == 1) a++;
                if (p6 == 0 && p7 == 1) a++;
                if (p7 == 0 && p8 == 1) a++;
                if (p8 == 0 && p9 == 1) a++;
                if (p9 == 0 && p2 == 1) a++;

                int b = p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9;
                int m1 = iteration == 0 ? (p2 * p4 * p6) : (p2 * p4 * p8);
                int m2 = iteration == 0 ? (p4 * p6 * p8) : (p2 * p6 * p8);

                if (a == 1 && (b >= 2 && b <= 6) && m1 == 0 && m2 == 0)
                    marker.put(i, j, 1);
            }
        }
        return marker;
    }

    // Function for thinning any given binary image within the range of 0-255. If not you should first make sure that your image has this range preset and configured!
    private Mat thinning(Mat im)
    {
        // Enforce the range tob e in between 0 - 255
        Core.divide(im, Scalar.all(255), im);

        Mat prev = Mat.zeros(im.size(), CvType.CV_8UC1);
        Mat diff = new Mat();

        do {
            im = thinningIteration(im, 0);
            im = thinningIteration(im, 1);
            Core.absdiff(im, prev, diff);
            im.copyTo(prev);
        }
        while (Core.countNonZero(diff) > 0);

        Core.multiply(im, Scalar.all(255), im);
        return im;
    }
}
