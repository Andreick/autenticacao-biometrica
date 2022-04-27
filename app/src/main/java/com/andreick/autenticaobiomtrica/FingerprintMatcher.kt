package com.andreick.autenticaobiomtrica

import android.util.Log
import org.opencv.core.DMatch
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.SIFT

class FingerprintMatcher {

    fun matchFingerprints(matchFingerprint: Mat, fingerprintMap: Set<Mat>): Mat? {
        var maxScore = MIN_SCORE
        var fingerprintWithMaxScore: Mat? = null
        for (fingerprint in fingerprintMap) {
            val score = matchTwoFingerprints(matchFingerprint, fingerprint)
            if (score > maxScore) {
                maxScore = score
                fingerprintWithMaxScore = fingerprint
            }
        }
        return fingerprintWithMaxScore
    }

    private fun matchTwoFingerprints(fingerprint1: Mat, fingerprint2: Mat): Int {
        // detect features
        val detector = SIFT.create()
        val keyPoints1 = MatOfKeyPoint()
        val keyPoints2 = MatOfKeyPoint()
        detector.detect(fingerprint1, keyPoints1)
        detector.detect(fingerprint2, keyPoints2)

        // extract features
        val descriptor1 = Mat()
        val descriptor2 = Mat()
        detector.compute(fingerprint1, keyPoints1, descriptor1)
        detector.compute(fingerprint2, keyPoints2, descriptor2)

        // match features
        val matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED)
        val matches = MatOfDMatch()
        matcher.match(descriptor1, descriptor2, matches)

        // find good matches
        val matchesList = matches.toList()
        val minDist = 45.0
        val goodMatchesList = mutableListOf<DMatch>()
        var min = 1000000.0
        var max = 0.0
        var distance: Double
        for (i in 0 until descriptor1.rows()) {
            distance = matchesList[i].distance.toDouble()
            if (distance > max) max = distance
            if (distance < min) min = distance
            if (distance < minDist) {
                goodMatchesList.add(matchesList[i])
            }
        }
        Log.i(TAG, String.format("MinMax: %f %f", min, max))
        Log.i(TAG, String.format("All, good: %d %d", matchesList.size, goodMatchesList.size))
        return goodMatchesList.size
    }

    companion object {
        private const val TAG = "FingerprintMatcher"
        private const val MIN_SCORE = 15
    }
}