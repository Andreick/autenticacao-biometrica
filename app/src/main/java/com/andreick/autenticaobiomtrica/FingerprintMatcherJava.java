package com.andreick.autenticaobiomtrica;

import android.util.Log;

import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.SIFT;

import java.util.LinkedList;
import java.util.List;

public class FingerprintMatcherJava {

    private static final String TAG = "FingerprintMatcher";

    public int matching(Mat image1, Mat image2) {
        List<DMatch> matchesList;
        List<DMatch> goodMatchesList = new LinkedList<>();

        Mat descriptor1 = new Mat();
        Mat descriptor2 = new Mat();

        SIFT detector = SIFT.create();
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED);

        MatOfKeyPoint keyPoints1 = new MatOfKeyPoint();
        MatOfKeyPoint keyPoints2 = new MatOfKeyPoint();
        MatOfDMatch matches = new MatOfDMatch();
        MatOfDMatch goodMatches = new MatOfDMatch();

        // detect features
        detector.detect(image1, keyPoints1);
        detector.detect(image2, keyPoints2);

        // extract features
        detector.compute(image1, keyPoints1, descriptor1);
        detector.compute(image2, keyPoints2, descriptor2);

        // match features
        matcher.match(descriptor1, descriptor2, matches);
        matchesList = matches.toList();

        // find good matches
        double minDist = 45;
        double min = 1000000;
        double max = 0;
        double distance;
        for (int i = 0; i < descriptor1.rows(); i++) {
            distance = matchesList.get(i).distance;
            if (distance > max) max = distance;
            if (distance < min) min = distance;
            if (distance < minDist) {
                goodMatchesList.add(matchesList.get(i));
            }
        }
        goodMatches.fromList(goodMatchesList);
        Log.i(TAG, String.format("MinMax: %f %f", min, max));
        Log.i(TAG, String.format("All, good: %d %d", matchesList.size(), goodMatchesList.size()));

        return goodMatchesList.size();
    }
}
