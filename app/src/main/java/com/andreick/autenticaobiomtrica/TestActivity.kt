package com.andreick.autenticaobiomtrica

import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.andreick.autenticaobiomtrica.databinding.ActivityMainBinding
import com.andreick.autenticaobiomtrica.extensions.toMat
import org.opencv.android.OpenCVLoader
import org.opencv.core.*


class TestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fingerprintDetection: FingerprintDetectionJava
    private lateinit var fingerprintMatcher: FingerprintMatcherJava
    private lateinit var cameraFrame: Mat
    private val matList: MutableList<Mat> = mutableListOf()

    private val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
        for ((permissionName, isGranted) in permissions.entries) {
            when (permissionName) {
                Manifest.permission.READ_EXTERNAL_STORAGE -> {
                    if (isGranted) {
                        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(pickIntent)
                    }
                    else Toast.makeText(
                        this, "Permission denied for fine location", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private val openGalleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))

            cameraFrame = bitmap.toMat()

            /*val center = Point(cameraFrame.width() / 2.0, cameraFrame.height() / 2.0)
            val axes = Size(cameraFrame.width() * 0.4, cameraFrame.height() * 0.28)
            val thickness = -1
            val lineType = 8

            val scalarWhite = Scalar(255.0, 255.0, 255.0)
            val scalarGray = Scalar(100.0, 100.0, 100.0)
            val scalarBlack = Scalar(0.0, 0.0, 0.0)

            val roi = Mat(cameraFrame.height(), cameraFrame.width(), CvType.CV_8UC1)
            roi.setTo(scalarWhite)
            Imgproc.ellipse(roi, center, axes, 0.0, 0.0, 360.0, scalarBlack, thickness, lineType, 0)
            cameraFrame.setTo(scalarGray, roi)

            val colStart = (cameraFrame.width() - axes.width * 2) / 2
            val rowStart = (cameraFrame.height() - axes.height * 2) / 2
            cameraFrame = cameraFrame.submat(
                Rect(
                    colStart.toInt(),
                    rowStart.toInt(),
                    axes.width.toInt() * 2,
                    axes.height.toInt() * 2
                )
            )

            mask = Mat(cameraFrame.rows(), cameraFrame.cols(), CvType.CV_8UC1, scalarBlack)
            Imgproc.ellipse(roi, center, axes, 0.0, 0.0, 360.0, scalarWhite, thickness, lineType, 0)*/

            //binding.ivImg.setImageBitmap(cameraFrame.toBitmap())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fingerprintDetection = FingerprintDetectionJava()
        fingerprintMatcher = FingerprintMatcherJava()

        //binding.btnRetake.setOnClickListener { requestStoragePermission() }

        /*binding.btnProcess.setOnClickListener {
            binding.pbProcess.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.Default).launch {
                val result = fingerprintDetection.processImage(cameraFrame)
                matList.add(result)
                runOnUiThread {
                    binding.ivImg.setImageBitmap(result.toBitmap())
                    binding.pbProcess.visibility = View.GONE
                }
            }
        }

        binding.btnMatch.setOnClickListener {
            binding.pbProcess.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.Default).launch {
                val mat = fingerprintDetection.processImage(cameraFrame)
                for (dbMat in matList) {
                    val score = fingerprintMatcher.matching(mat, dbMat)
                    Log.d(TAG, "$score")
                }
                runOnUiThread {
                    binding.pbProcess.visibility = View.GONE
                }
            }
        }*/
    }

    override fun onResume() {
        super.onResume()
        if (OpenCVLoader.initDebug()) {
            Log.i("MainActivity", "OpenCV loaded successfully")
        }
        else {
            Log.e("MainActivity", "OpenCV failed to load")
            finish()
        }
    }

    private fun requestStoragePermission() {
        val storagePermissionArray = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        requestPermissions.launch(storagePermissionArray)
    }

    companion object {
        const val TAG = "MainActivity"
    }
}