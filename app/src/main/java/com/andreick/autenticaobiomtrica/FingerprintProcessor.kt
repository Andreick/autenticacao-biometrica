package com.andreick.autenticaobiomtrica

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*
import kotlin.math.*

class FingerprintProcessor(private val matSnapShot: Mat) {

    /**
     * Process the image to get the skeleton.
     */
    fun processImage(): Mat {
        val rows: Int = matSnapShot.rows()
        val cols: Int = matSnapShot.cols()

        // apply histogram equalization
        val equalized = Mat(rows, cols, CvType.CV_32FC1)
        Imgproc.equalizeHist(matSnapShot, equalized)

        // convert to float, very important
        val floated = Mat(rows, cols, CvType.CV_32FC1)
        equalized.convertTo(floated, CvType.CV_32FC1)

        // normalise image to have zero mean and 1 standard deviation
        val normalized = Mat(rows, cols, CvType.CV_32FC1)
        normalizeImage(floated, normalized)

        // step 1: get ridge segment by padding then do block process
        val blockSize = 24
        val threshold = 0.05
        val padded: Mat = imagePadding(floated, blockSize)
        val imgRows = padded.rows()
        val imgCols = padded.cols()
        val matRidgeSegment = Mat(imgRows, imgCols, CvType.CV_32FC1)
        val segmentMask = Mat(imgRows, imgCols, CvType.CV_8UC1)
        ridgeSegment(padded, matRidgeSegment, segmentMask, blockSize, threshold)

        // step 2: get ridge orientation
        val gradientSigma = 1
        val blockSigma = 13
        val orientSmoothSigma = 15
        val matRidgeOrientation = Mat(imgRows, imgCols, CvType.CV_32FC1)
        ridgeOrientation(
            matRidgeSegment,
            matRidgeOrientation,
            gradientSigma,
            blockSigma,
            orientSmoothSigma
        )

        // step 3: get ridge frequency
        val fBlockSize = 36
        val fWindowSize = 5
        val fMinWaveLength = 5
        val fMaxWaveLength = 25
        val matFrequency = Mat(imgRows, imgCols, CvType.CV_32FC1)
        val medianFreq: Double = ridgeFrequency(
            matRidgeSegment,
            segmentMask,
            matRidgeOrientation,
            matFrequency,
            fBlockSize,
            fWindowSize,
            fMinWaveLength,
            fMaxWaveLength
        )

        // step 4: get ridge filter
        val matRidgeFilter = Mat(imgRows, imgCols, CvType.CV_32FC1)
        val filterSize = 1.9
        ridgeFilter(
            matRidgeSegment,
            matRidgeOrientation,
            matFrequency,
            matRidgeFilter,
            filterSize,
            filterSize,
            medianFreq
        )

        // step 5: enhance image after ridge filter
        val matEnhanced = Mat(imgRows, imgCols, CvType.CV_8UC1)
        enhancement(matRidgeFilter, matEnhanced, blockSize)

        return matEnhanced
    }

    /**
     * Normalize the image to have zero mean and unit standard deviation.
     */
    private fun normalizeImage(src: Mat, dst: Mat) {
        val mean = MatOfDouble(0.0)
        val std = MatOfDouble(0.0)

        // get mean and standard deviation
        Core.meanStdDev(src, mean, std)
        Core.subtract(src, Scalar.all(mean.toArray()[0]), dst)
        Core.meanStdDev(dst, mean, std)
        Core.divide(dst, Scalar.all(std.toArray()[0]), dst)
    }

    /**
     * Apply padding to the image.
     */
    private fun imagePadding(source: Mat, blockSize: Int): Mat {
        val width = source.width()
        val height = source.height()
        var bottomPadding = 0
        var rightPadding = 0
        if (width % blockSize != 0) {
            bottomPadding = blockSize - width % blockSize
        }
        if (height % blockSize != 0) {
            rightPadding = blockSize - height % blockSize
        }
        Core.copyMakeBorder(
            source,
            source,
            0,
            bottomPadding,
            0,
            rightPadding,
            Core.BORDER_CONSTANT,
            Scalar.all(0.0)
        )
        return source
    }

    /**
     * calculate ridge segment by doing block process for the given image using the given block size.
     */
    private fun ridgeSegment(
        source: Mat,
        result: Mat,
        mask: Mat,
        blockSize: Int,
        threshold: Double
    ) {
        // for each block, get standard deviation
        // and replace the block with it
        val widthSteps = source.width() / blockSize
        val heightSteps = source.height() / blockSize
        val mean = MatOfDouble()
        val std = MatOfDouble()
        var window: Mat?
        val scalarBlack = Scalar.all(0.0)
        val scalarWhile = Scalar.all(255.0)
        val windowMask = Mat(source.rows(), source.cols(), CvType.CV_8UC1)
        var roi: Rect
        var stdVal: Double
        for (y in 1..heightSteps) {
            for (x in 1..widthSteps) {
                roi = Rect(blockSize * (x - 1), blockSize * (y - 1), blockSize, blockSize)
                windowMask.setTo(scalarBlack)
                Imgproc.rectangle(
                    windowMask, Point(roi.x.toDouble(), roi.y.toDouble()), Point(
                        (roi.x + roi.width).toDouble(),
                        (roi.y + roi.height).toDouble()
                    ), scalarWhile, -1, 8, 0
                )
                window = source.submat(roi)
                Core.meanStdDev(window, mean, std)
                stdVal = std.toArray()[0]
                result.setTo(Scalar.all(stdVal), windowMask)

                // mask used to calc mean and standard deviation later
                mask.setTo(Scalar.all(if (stdVal >= threshold) 1.0 else 0.0), windowMask)
            }
        }

        // get mean and standard deviation
        Core.meanStdDev(source, mean, std, mask)
        Core.subtract(source, Scalar.all(mean.toArray()[0]), result)
        Core.meanStdDev(result, mean, std, mask)
        Core.divide(result, Scalar.all(std.toArray()[0]), result)
    }

    /**
     * Calculate ridge orientation.
     */
    private fun ridgeOrientation(
        ridgeSegment: Mat,
        result: Mat,
        gradientSigma: Int,
        blockSigma: Int,
        orientSmoothSigma: Int
    ) {
        val rows = ridgeSegment.rows()
        val cols = ridgeSegment.cols()

        // calculate image gradients
        var kSize = (6 * gradientSigma)
        if (kSize % 2 == 0) {
            kSize++
        }
        var kernel: Mat? = gaussianKernel(kSize, gradientSigma)
        val fXKernel = Mat(1, 3, CvType.CV_32FC1)
        val fYKernel = Mat(3, 1, CvType.CV_32FC1)
        fXKernel.put(0, 0, -1.0)
        fXKernel.put(0, 1, 0.0)
        fXKernel.put(0, 2, 1.0)
        fYKernel.put(0, 0, -1.0)
        fYKernel.put(1, 0, 0.0)
        fYKernel.put(2, 0, 1.0)
        val fX = Mat(kSize, kSize, CvType.CV_32FC1)
        val fY = Mat(kSize, kSize, CvType.CV_32FC1)
        Imgproc.filter2D(kernel, fX, CvType.CV_32FC1, fXKernel)
        Imgproc.filter2D(kernel, fY, CvType.CV_32FC1, fYKernel)
        val gX = Mat(rows, cols, CvType.CV_32FC1)
        val gY = Mat(rows, cols, CvType.CV_32FC1)
        Imgproc.filter2D(ridgeSegment, gX, CvType.CV_32FC1, fX)
        Imgproc.filter2D(ridgeSegment, gY, CvType.CV_32FC1, fY)

        // covariance data for the image gradients
        val gXX = Mat(rows, cols, CvType.CV_32FC1)
        val gXY = Mat(rows, cols, CvType.CV_32FC1)
        val gYY = Mat(rows, cols, CvType.CV_32FC1)
        Core.multiply(gX, gX, gXX)
        Core.multiply(gX, gY, gXY)
        Core.multiply(gY, gY, gYY)

        // smooth the covariance data to perform a weighted summation of the data.
        kSize = (6 * blockSigma).toFloat().roundToInt()
        if (kSize % 2 == 0) {
            kSize++
        }
        kernel = gaussianKernel(kSize, blockSigma)
        Imgproc.filter2D(gXX, gXX, CvType.CV_32FC1, kernel)
        Imgproc.filter2D(gYY, gYY, CvType.CV_32FC1, kernel)
        Imgproc.filter2D(gXY, gXY, CvType.CV_32FC1, kernel)
        Core.multiply(gXY, Scalar.all(2.0), gXY)

        // analytic solution of principal direction
        val denom = Mat(rows, cols, CvType.CV_32FC1)
        val gXXMiusgYY = Mat(rows, cols, CvType.CV_32FC1)
        val gXXMiusgYYSquared = Mat(rows, cols, CvType.CV_32FC1)
        val gXYSquared = Mat(rows, cols, CvType.CV_32FC1)
        Core.subtract(gXX, gYY, gXXMiusgYY)
        Core.multiply(gXXMiusgYY, gXXMiusgYY, gXXMiusgYYSquared)
        Core.multiply(gXY, gXY, gXYSquared)
        Core.add(gXXMiusgYYSquared, gXYSquared, denom)
        Core.sqrt(denom, denom)

        // sine and cosine of doubled angles
        val sin2Theta = Mat(rows, cols, CvType.CV_32FC1)
        val cos2Theta = Mat(rows, cols, CvType.CV_32FC1)
        Core.divide(gXY, denom, sin2Theta)
        Core.divide(gXXMiusgYY, denom, cos2Theta)

        // smooth orientations (sine and cosine)
        // smoothed sine and cosine of doubled angles
        kSize = (6 * orientSmoothSigma).toFloat().roundToInt()
        if (kSize % 2 == 0) {
            kSize++
        }
        kernel = gaussianKernel(kSize, orientSmoothSigma)
        Imgproc.filter2D(sin2Theta, sin2Theta, CvType.CV_32FC1, kernel)
        Imgproc.filter2D(cos2Theta, cos2Theta, CvType.CV_32FC1, kernel)

        // calculate the result as the following, so the values of the matrix range [0, PI]
        //orientim = atan2(sin2theta,cos2theta)/360;
        atan2(sin2Theta, cos2Theta, result)
        Core.multiply(result, Scalar.all(Math.PI / 360.0), result)
    }

    /**
     * Create Gaussian kernel.
     */
    private fun gaussianKernel(kSize: Int, sigma: Int): Mat {
        val kernelX = Imgproc.getGaussianKernel(kSize, sigma.toDouble(), CvType.CV_32FC1)
        val kernelY = Imgproc.getGaussianKernel(kSize, sigma.toDouble(), CvType.CV_32FC1)
        val kernel = Mat(kSize, kSize, CvType.CV_32FC1)
        Core.gemm(
            kernelX,
            kernelY.t(),
            1.0,
            Mat.zeros(kSize, kSize, CvType.CV_32FC1),
            0.0,
            kernel,
            0
        )
        return kernel
    }

    /**
     * Calculate bitwise atan2 for the given 2 images.
     */
    private fun atan2(src1: Mat, src2: Mat, dst: Mat) {
        val height = src1.height()
        val width = src2.width()
        for (y in 0 until height) {
            for (x in 0 until width) {
                dst.put(
                    y, x, Core.fastAtan2(
                        src1[y, x][0].toFloat(),
                        src2[y, x][0].toFloat()
                    ).toDouble()
                )
            }
        }
    }

    /**
     * Calculate ridge frequency.
     */
    private fun ridgeFrequency(
        ridgeSegment: Mat,
        segmentMask: Mat,
        ridgeOrientation: Mat,
        frequencies: Mat,
        blockSize: Int,
        windowSize: Int,
        minWaveLength: Int,
        maxWaveLength: Int
    ): Double {
        val rows = ridgeSegment.rows()
        val cols = ridgeSegment.cols()
        var blockSegment: Mat?
        var blockOrientation: Mat?
        var frequency: Mat
        var y = 0
        while (y < rows - blockSize) {
            var x = 0
            while (x < cols - blockSize) {
                blockSegment = ridgeSegment.submat(y, y + blockSize, x, x + blockSize)
                blockOrientation = ridgeOrientation.submat(y, y + blockSize, x, x + blockSize)
                frequency = calculateFrequency(
                    blockSegment,
                    blockOrientation,
                    windowSize,
                    minWaveLength,
                    maxWaveLength
                )
                frequency.copyTo(frequencies.rowRange(y, y + blockSize).colRange(x, x + blockSize))
                x += blockSize
            }
            y += blockSize
        }

        // mask out frequencies calculated for non ridge regions
        Core.multiply(frequencies, segmentMask, frequencies, 1.0, CvType.CV_32FC1)

        // find median frequency over all the valid regions of the image.
        val medianFrequency: Double = medianFrequency(frequencies)

        // the median frequency value used across the whole fingerprint gives a more satisfactory result
        Core.multiply(segmentMask, Scalar.all(medianFrequency), frequencies, 1.0, CvType.CV_32FC1)
        return medianFrequency
    }

    /**
     * Estimate fingerprint ridge frequency within image block.
     */
    private fun calculateFrequency(
        block: Mat,
        blockOrientation: Mat,
        windowSize: Int,
        minWaveLength: Int,
        maxWaveLength: Int
    ): Mat {
        val rows = block.rows()
        val cols = block.cols()
        val orientation = blockOrientation.clone()
        Core.multiply(orientation, Scalar.all(2.0), orientation)
        val orientLength = orientation.total().toInt()
        val orientations = FloatArray(orientLength)
        orientation[0, 0, orientations]
        val sinOrient = DoubleArray(orientLength)
        val cosOrient = DoubleArray(orientLength)
        for (i in 1 until orientLength) {
            sinOrient[i] = sin(orientations[i].toDouble())
            cosOrient[i] = cos(orientations[i].toDouble())
        }
        val orient = Core.fastAtan2(
            calculateMean(sinOrient).toFloat(),
            calculateMean(cosOrient).toFloat()
        ) / 2.0.toFloat()

        // rotate the image block so that the ridges are vertical
        val rotated = Mat(rows, cols, CvType.CV_32FC1)
        val center = Point((cols / 2).toDouble(), (rows / 2).toDouble())
        val rotateAngle = orient / Math.PI * 180.0 + 90.0
        val rotateScale = 1.0
        val rotatedSize = Size(cols.toDouble(), rows.toDouble())
        val rotateMatrix = Imgproc.getRotationMatrix2D(center, rotateAngle, rotateScale)
        Imgproc.warpAffine(block, rotated, rotateMatrix, rotatedSize, Imgproc.INTER_NEAREST)

        // crop the image so that the rotated image does not contain any invalid regions
        // this prevents the projection down the columns from being mucked up
        val cropSize = (rows / sqrt(2.0)).roundToInt()
        val offset = ((rows - cropSize) / 2.0).roundToInt() - 1
        val cropped = rotated.submat(offset, offset + cropSize, offset, offset + cropSize)

        // get sums of columns
        var sum: Float
        val proj = Mat(1, cropped.cols(), CvType.CV_32FC1)
        for (c in 1 until cropped.cols()) {
            sum = 0f
            for (r in 1 until cropped.cols()) {
                sum += cropped[r, c][0].toFloat()
            }
            proj.put(0, c, sum.toDouble())
        }

        // find peaks in projected grey values by performing a grayScale
        // dilation and then finding where the dilation equals the original values.
        val dilateKernel = Mat(windowSize, windowSize, CvType.CV_32FC1, Scalar.all(1.0))
        val dilate = Mat(1, cropped.cols(), CvType.CV_32FC1)
        Imgproc.dilate(proj, dilate, dilateKernel, Point(-1.0, -1.0), 1)
        //Imgproc.dilate(proj, dilate, dilateKernel, new Point(-1, -1), 1, Imgproc.BORDER_CONSTANT, Scalar.all(0.0));
        val projMean = Core.mean(proj).`val`[0]
        var projValue: Double
        var dilateValue: Double
        val roundPoints = 1000.0
        val maxind = ArrayList<Int>()
        for (i in 0 until cropped.cols()) {
            projValue = proj[0, i][0]
            dilateValue = dilate[0, i][0]

            // round to maximize the likelihood of equality
            projValue = (projValue * roundPoints).roundToInt() / roundPoints
            dilateValue = (dilateValue * roundPoints).roundToInt() / roundPoints
            if (dilateValue == projValue && projValue > projMean) {
                maxind.add(i)
            }
        }

        // determine the spatial frequency of the ridges by dividing the distance between
        // the 1st and last peaks by the (No of peaks-1). If no peaks are detected
        // or the wavelength is outside the allowed bounds, the frequency image is set to 0
        var result = Mat(rows, cols, CvType.CV_32FC1, Scalar.all(0.0))
        val peaks: Int = maxind.size
        if (peaks >= 2) {
            val waveLength = ((maxind[peaks - 1] - maxind[0]) / (peaks - 1)).toDouble()
            if (waveLength >= minWaveLength && waveLength <= maxWaveLength) {
                result = Mat(rows, cols, CvType.CV_32FC1, Scalar.all(1.0 / waveLength))
            }
        }
        return result
    }

    /**
     * Calculate mean of given array.
     */
    private fun calculateMean(m: DoubleArray): Double {
        var sum = 0.0
        for (i in m.indices) {
            sum += m[i]
        }
        return sum / m.size
    }

    /**
     * Calculate the median of all values greater than zero.
     */
    private fun medianFrequency(image: Mat): Double {
        val values = ArrayList<Double>()
        var value: Double
        for (r in 0 until image.rows()) {
            for (c in 0 until image.cols()) {
                value = image[r, c][0]
                if (value > 0) {
                    values.add(value)
                }
            }
        }
        values.sort()
        val size: Int = values.size
        var median = 0.0
        if (size > 0) {
            val halfSize = size / 2
            median = if (size % 2 == 0) {
                (values[halfSize - 1] + values[halfSize]) / 2.0
            } else {
                values[halfSize]
            }
        }
        return median
    }

    /**
     * Enhance fingerprint image using oriented filters.
     */
    private fun ridgeFilter(
        ridgeSegment: Mat,
        orientation: Mat,
        frequency: Mat,
        result: Mat,
        kx: Double,
        ky: Double,
        medianFreq: Double
    ) {
        val angleInc = 3
        val rows = ridgeSegment.rows()
        val cols = ridgeSegment.cols()
        val filterCount = 180 / angleInc
        val filters = arrayOfNulls<Mat>(filterCount)
        val sigmaX = kx / medianFreq
        val sigmaY = ky / medianFreq

        //mat refFilter = exp(-(x. ^ 2 / sigmaX ^ 2 + y. ^ 2 / sigmaY ^ 2) / 2). * cos(2 * pi * medianFreq * x);
        var size = (3 * max(sigmaX, sigmaY)).roundToInt()
        size = if (size % 2 == 0) size else size + 1
        val length = size * 2 + 1
        val x: Mat = meshGrid(size)
        val y = x.t()
        val xSquared = Mat(length, length, CvType.CV_32FC1)
        val ySquared = Mat(length, length, CvType.CV_32FC1)
        Core.multiply(x, x, xSquared)
        Core.multiply(y, y, ySquared)
        Core.divide(xSquared, Scalar.all(sigmaX * sigmaX), xSquared)
        Core.divide(ySquared, Scalar.all(sigmaY * sigmaY), ySquared)
        val refFilterPart1 = Mat(length, length, CvType.CV_32FC1)
        Core.add(xSquared, ySquared, refFilterPart1)
        Core.divide(refFilterPart1, Scalar.all(-2.0), refFilterPart1)
        Core.exp(refFilterPart1, refFilterPart1)
        var refFilterPart2 = Mat(length, length, CvType.CV_32FC1)
        Core.multiply(x, Scalar.all(2 * Math.PI * medianFreq), refFilterPart2)
        refFilterPart2 = matCos(refFilterPart2)
        val refFilter = Mat(length, length, CvType.CV_32FC1)
        Core.multiply(refFilterPart1, refFilterPart2, refFilter)

        // Generate rotated versions of the filter.  Note orientation
        // image provides orientation *along* the ridges, hence +90
        // degrees, and the function requires angles +ve anticlockwise, hence the minus sign.
        var rotated: Mat
        var rotateMatrix: Mat?
        var rotateAngle: Double
        val center = Point((length / 2).toDouble(), (length / 2).toDouble())
        val rotatedSize = Size(length.toDouble(), length.toDouble())
        val rotateScale = 1.0
        for (i in 0 until filterCount) {
            rotateAngle = -(i * angleInc).toDouble()
            rotated = Mat(length, length, CvType.CV_32FC1)
            rotateMatrix = Imgproc.getRotationMatrix2D(center, rotateAngle, rotateScale)
            Imgproc.warpAffine(refFilter, rotated, rotateMatrix, rotatedSize, Imgproc.INTER_LINEAR)
            filters[i] = rotated
        }

        // convert orientation matrix values from radians to an index value
        // that corresponds to round(degrees/angleInc)
        val orientIndexes = Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1)
        Core.multiply(
            orientation,
            Scalar.all(filterCount.toDouble() / Math.PI),
            orientIndexes,
            1.0,
            CvType.CV_8UC1
        )
        var orientMask = Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1, Scalar.all(0.0))
        var orientThreshold = Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1, Scalar.all(0.0))
        Core.compare(orientIndexes, orientThreshold, orientMask, Core.CMP_LT)
        Core.add(orientIndexes, Scalar.all(filterCount.toDouble()), orientIndexes, orientMask)
        orientMask = Mat(orientation.rows(), orientation.cols(), CvType.CV_8UC1, Scalar.all(0.0))
        orientThreshold = Mat(
            orientation.rows(),
            orientation.cols(),
            CvType.CV_8UC1,
            Scalar.all(filterCount.toDouble())
        )
        Core.compare(orientIndexes, orientThreshold, orientMask, Core.CMP_GE)
        Core.subtract(orientIndexes, Scalar.all(filterCount.toDouble()), orientIndexes, orientMask)

        // finally, find where there is valid frequency data then do the filtering
        val value = Mat(length, length, CvType.CV_32FC1)
        var subSegment: Mat?
        var orientIndex: Int
        var sum: Double
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                if (frequency[r, c][0] > 0 && r > size + 1 && r < rows - size - 1 && c > size + 1 && c < cols - size - 1) {
                    orientIndex = orientIndexes[r, c][0].toInt()
                    subSegment = ridgeSegment.submat(r - size - 1, r + size, c - size - 1, c + size)
                    Core.multiply(subSegment, filters[orientIndex], value)
                    sum = Core.sumElems(value).`val`[0]
                    result.put(r, c, sum)
                }
            }
        }
    }

    /**
     * Create mesh grid.
     */
    private fun meshGrid(size: Int): Mat {
        val l = size * 2 + 1
        var value = -size
        val result = Mat(l, l, CvType.CV_32FC1)
        for (c in 0 until l) {
            for (r in 0 until l) {
                result.put(r, c, value.toDouble())
            }
            value++
        }
        return result
    }

    /**
     * Apply cos to each element of the matrix.
     */
    private fun matCos(source: Mat): Mat {
        val rows = source.rows()
        val cols = source.cols()
        val result = Mat(cols, rows, CvType.CV_32FC1)
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                result.put(r, c, cos(source[r, c][0]))
            }
        }
        return result
    }

    /**
     * Enhance the image after ridge filter.
     * Apply mask, binary threshold, thinning, ..., etc.
     */
    private fun enhancement(source: Mat, result: Mat, blockSize: Int) {
        //val paddedMask = imagePadding(MatSnapShotMask, blockSize)

        // apply the original mask to get rid of extras
        //Core.multiply(source, paddedMask, result, 1.0, CvType.CV_8UC1)

        // apply binary threshold
        Imgproc.threshold(result, result, 0.0, 255.0, Imgproc.THRESH_BINARY)

        // apply thinning
        //int thinIterations = 2;
        //thin(result, thinIterations);

        //// normalize the values to the binary scale [0, 255]
        //Core.normalize(result, result, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);

        // apply morphing (erosion, opening, ... )
        //int erosionSize = 1;
        //int erosionLength = 2 * erosionSize + 1;
        //Mat erosionKernel = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(erosionLength, erosionLength), new Point(erosionSize, erosionSize));
        //Imgproc.erode(result, result, erosionKernel);
    }
}