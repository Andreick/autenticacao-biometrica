package com.andreick.autenticaobiomtrica.utils

import org.opencv.core.DMatch
import org.opencv.core.Mat
import org.opencv.core.MatOfDMatch
import org.opencv.core.MatOfKeyPoint
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.SIFT
import javax.inject.Inject

class FingerprintMatcher @Inject constructor() {

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
        // detecta minúcias
        val detector = SIFT.create()
        val keyPoints1 = MatOfKeyPoint()
        val keyPoints2 = MatOfKeyPoint()
        detector.detect(fingerprint1, keyPoints1)
        detector.detect(fingerprint2, keyPoints2)

        // extrai minúcias
        val descriptor1 = Mat()
        val descriptor2 = Mat()
        detector.compute(fingerprint1, keyPoints1, descriptor1)
        detector.compute(fingerprint2, keyPoints2, descriptor2)

        // compara minúcas
        val matcher = DescriptorMatcher.create(DescriptorMatcher.FLANNBASED)
        val matches = MatOfDMatch()
        matcher.match(descriptor1, descriptor2, matches)

        // encontra a quantidade de minúcias que combinam
        val matchesList = matches.toList()
        val minDist = 45.0
        val goodMatchesList = mutableListOf<DMatch>()
        for (i in 0 until descriptor1.rows()) {
            val distance = matchesList[i].distance.toDouble()
            if (distance < minDist) {
                goodMatchesList.add(matchesList[i])
            }
        }
        return goodMatchesList.size
    }

    companion object {
        private const val TAG = "FingerprintMatcher"
        private const val MIN_SCORE = 15
    }
}