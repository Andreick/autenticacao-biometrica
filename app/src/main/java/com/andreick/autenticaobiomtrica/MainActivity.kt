package com.andreick.autenticaobiomtrica

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.andreick.autenticaobiomtrica.databinding.ActivityMainBinding
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraFrame: Mat
    private lateinit var maskSize: Size
    private var croppedCameraFrame: Mat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.openCvCamera.setCvCameraViewListener(this)

        binding.openCvCamera.setOnClickListener { takeSnapShot() }
        binding.btnProcess.setOnClickListener {
            croppedCameraFrame?.let {
                binding.pbProcess.visibility = View.VISIBLE
                val result = FingerprintProcessor(it).processImage()
                binding.pbProcess.visibility = View.INVISIBLE
                binding.ivImg.setImageBitmap(result.toBitmap())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (OpenCVLoader.initDebug()) {
            Log.i("MainActivity", "OpenCV loaded successfully")
            binding.openCvCamera.enableView()
        }
        else {
            Log.e("MainActivity", "OpenCV failed to load")
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.openCvCamera.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.openCvCamera.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        binding.openCvCamera.setFocusMode(this, 5)
        binding.openCvCamera.cameraDistance
        maskSize = Size(width * 0.3, height * 0.28)
    }

    override fun onCameraViewStopped() {
        cameraFrame.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        cameraFrame = inputFrame.rgba().apply { drawEllipse(this) }
        return cameraFrame
    }

    private fun drawEllipse(mat: Mat) {
        /*Log.d(TAG, "${mat.height()}")
        Log.d(TAG, "${mat.width()}")
        Log.d(TAG, "${openCvCamera.width}")
        Log.d(TAG, "${openCvCamera.height}")*/
        val center = Point(mat.width() / 2.0, mat.height() / 2.0)
        val axes = maskSize
        val thickness = 2
        val lineType = 8
        Imgproc.ellipse(mat, center, axes, 0.0, 0.0, 360.0, Scalar(0.0, 173.0, 142.0), thickness, lineType, 0)
    }

    private fun takeSnapShot() {
        val frameGrayScale = Mat(cameraFrame.height(), cameraFrame.width(), CvType.CV_8UC1)
        Imgproc.cvtColor(cameraFrame, frameGrayScale, Imgproc.COLOR_RGB2GRAY)

        val center = Point(cameraFrame.width() / 2.0, cameraFrame.height() / 2.0)
        val axes = maskSize
        val thickness = -1
        val lineType = 8

        val scalarWhite = Scalar(255.0, 255.0, 255.0)
        val scalarGray = Scalar(100.0, 100.0, 100.0)
        val scalarBlack = Scalar(0.0, 0.0, 0.0)

        val roi = Mat(cameraFrame.height(), cameraFrame.width(), CvType.CV_8UC1)
        roi.setTo(scalarWhite)
        Imgproc.ellipse(roi, center, axes, 0.0, 0.0, 360.0, scalarBlack, thickness, lineType, 0)
        frameGrayScale.setTo(scalarGray, roi)

        val colStart = (cameraFrame.width() - axes.width * 2) / 2
        val rowStart = (cameraFrame.height() - axes.height * 2) / 2
        croppedCameraFrame = frameGrayScale.submat(
            Rect(
                colStart.toInt(),
                rowStart.toInt(),
                axes.width.toInt() * 2,
                axes.height.toInt() * 2
            )
        )

        binding.ivImg.setImageBitmap(croppedCameraFrame?.toBitmap())
        binding.openCvCamera.disableView()
        binding.ivImg.visibility = View.VISIBLE
        binding.btnProcess.visibility = View.VISIBLE
        binding.btnRetake.visibility = View.VISIBLE
        binding.openCvCamera.visibility = View.GONE
    }

    companion object {
        const val TAG = "MainActivity"
    }
}