package com.andreick.autenticaobiomtrica.utils

import android.graphics.Bitmap
import com.andreick.autenticaobiomtrica.extensions.toBitmap
import com.andreick.autenticaobiomtrica.extensions.toMat
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*
import javax.inject.Inject

class FingerprintProcessor @Inject constructor() {

    fun processImage(input: Bitmap): Pair<Bitmap, Bitmap> {
        val (harrisNormalised, harrisKeyPointsVisualisation) = processImage(input.toMat())
        return Pair(harrisNormalised.toBitmap(), harrisKeyPointsVisualisation.toBitmap())
    }

    fun processImage(input: Mat): Pair<Mat, Mat> {
        val grayMat = Mat()
        Imgproc.cvtColor(input, grayMat, Imgproc.COLOR_RGB2GRAY)
        val inputBinary = Mat()
        Imgproc.threshold(grayMat, inputBinary, 0.0, 255.0, 8)

        // Detecta as minúcias mais fortes usando o algoritmo Harris Corner Detector
        val harrisCorners = Mat.zeros(inputBinary.size(), CvType.CV_32FC1)
        Imgproc.cornerHarris(inputBinary, harrisCorners, 2, 3, 0.04, Core.BORDER_DEFAULT)
        val harrisNormalised = Mat()
        Core.normalize(
            harrisCorners,
            harrisNormalised,
            0.0,
            255.0,
            Core.NORM_MINMAX,
            CvType.CV_8UC3
        )

        // Marca círculos na imagem para fins de visualização
        val thresholdHarris = 175
        val rescaled = Mat()
        Core.convertScaleAbs(harrisNormalised, rescaled)
        val harrisKeyPointsVisualisation = Mat(rescaled.rows(), rescaled.cols(), CvType.CV_8UC3)
        val src: List<Mat> = listOf(rescaled, rescaled, rescaled)
        val fromTo = MatOfInt(0, 0, 1, 1, 2, 2)
        Core.mixChannels(src, Collections.singletonList(harrisKeyPointsVisualisation), fromTo)
        for (x in 0 until harrisNormalised.cols()) {
            for (y in 0 until harrisNormalised.rows()) {
                if (harrisNormalised.get(y, x)[0] > thresholdHarris) {
                    // Desenha a localização da minúcia
                    Imgproc.circle(
                        harrisKeyPointsVisualisation, Point(x.toDouble(), y.toDouble()),
                        5, Scalar(0.0, 255.0, 0.0), 1
                    )
                    Imgproc.circle(
                        harrisKeyPointsVisualisation, Point(x.toDouble(), y.toDouble()),
                        1, Scalar(0.0, 0.0, 255.0), 1
                    )
                }
            }
        }

        return Pair(harrisNormalised, harrisKeyPointsVisualisation)
    }
}