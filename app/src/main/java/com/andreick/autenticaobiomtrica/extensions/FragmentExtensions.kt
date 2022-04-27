package com.andreick.autenticaobiomtrica.extensions

import android.view.WindowManager
import androidx.fragment.app.Fragment

fun Fragment.showToast(text: String) {
    requireContext().showToast(text)
}

fun Fragment.disableInput() {
    requireActivity().window.setFlags(
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
    )
}

fun Fragment.enableInput() {
    requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
}