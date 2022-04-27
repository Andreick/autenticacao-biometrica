package com.andreick.autenticaobiomtrica.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.Mat

fun Context.showToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}

fun ImageProxy.toMat(): Mat = toBitmap().toMat()

fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

fun Bitmap.toMat(): Mat = Mat().also { Utils.bitmapToMat(this, it) }

fun Mat.toBitmap(): Bitmap =
    Bitmap.createBitmap(cols(), rows(), Bitmap.Config.ARGB_8888).also {
        Utils.matToBitmap(this, it)
    }