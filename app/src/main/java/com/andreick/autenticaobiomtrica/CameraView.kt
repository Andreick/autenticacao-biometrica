package com.andreick.autenticaobiomtrica

import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import android.widget.Toast
import org.opencv.android.JavaCameraView

class CameraView(context: Context, attrs: AttributeSet) : JavaCameraView(context, attrs) {

    fun setFocusMode(item: Context?, type: Int) {
        val params = mCamera.parameters
        val focusModes = params.supportedFocusModes
        when (type) {
            0 -> if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) params.focusMode =
                Camera.Parameters.FOCUS_MODE_AUTO else Toast.makeText(
                item,
                "Auto Mode not supported",
                Toast.LENGTH_SHORT
            ).show()
            1 -> if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) params.focusMode =
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO else Toast.makeText(
                item,
                "Continuous Mode not supported",
                Toast.LENGTH_SHORT
            ).show()
            2 -> if (focusModes.contains(Camera.Parameters.FOCUS_MODE_EDOF)) params.focusMode =
                Camera.Parameters.FOCUS_MODE_EDOF else Toast.makeText(
                item,
                "EDOF Mode not supported",
                Toast.LENGTH_SHORT
            ).show()
            3 -> if (focusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) params.focusMode =
                Camera.Parameters.FOCUS_MODE_FIXED else Toast.makeText(
                item,
                "Fixed Mode not supported",
                Toast.LENGTH_SHORT
            ).show()
            4 -> if (focusModes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) params.focusMode =
                Camera.Parameters.FOCUS_MODE_INFINITY else Toast.makeText(
                item,
                "Infinity Mode not supported",
                Toast.LENGTH_SHORT
            ).show()
            5 -> if (focusModes.contains(Camera.Parameters.FOCUS_MODE_MACRO)) params.focusMode =
                Camera.Parameters.FOCUS_MODE_MACRO else Toast.makeText(
                item,
                "Macro Mode not supported",
                Toast.LENGTH_SHORT
            ).show()
        }
        mCamera.parameters = params
    }
}