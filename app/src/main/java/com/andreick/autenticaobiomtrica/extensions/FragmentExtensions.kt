package com.andreick.autenticaobiomtrica.extensions

import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment

fun Fragment.showShortToast(text: String) {
    Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show()
}

fun Fragment.showLongToast(text: String) {
    Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show()
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