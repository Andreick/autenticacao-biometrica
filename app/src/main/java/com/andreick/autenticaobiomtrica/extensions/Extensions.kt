package com.andreick.autenticaobiomtrica.extensions

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Mat

fun Bitmap.toMat(): Mat = Mat().also { Utils.bitmapToMat(this, it) }

fun Mat.toBitmap(): Bitmap =
    Bitmap.createBitmap(cols(), rows(), Bitmap.Config.ARGB_8888).also { Utils.matToBitmap(this, it) }