package com.andreick.autenticaobiomtrica.extensions

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import org.opencv.android.Utils
import org.opencv.core.Mat

fun Context.showToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

fun Bitmap.toMat(): Mat = Mat().also { Utils.bitmapToMat(this, it) }

fun Mat.toBitmap(): Bitmap =
    Bitmap.createBitmap(cols(), rows(), Bitmap.Config.ARGB_8888).also {
        Utils.matToBitmap(this, it)
    }