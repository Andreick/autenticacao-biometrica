package com.andreick.autenticaobiomtrica.view

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.andreick.autenticaobiomtrica.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import org.opencv.android.OpenCVLoader

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
}