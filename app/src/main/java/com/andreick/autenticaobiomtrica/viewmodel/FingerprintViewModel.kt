package com.andreick.autenticaobiomtrica.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.*
import com.andreick.autenticaobiomtrica.Action
import com.andreick.autenticaobiomtrica.FingerprintDetectionJava
import com.andreick.autenticaobiomtrica.toBitmap
import com.andreick.autenticaobiomtrica.toMat
import kotlinx.coroutines.launch

class FingerprintViewModel(
    private val fingerprintDetection: FingerprintDetectionJava
) : ViewModel() {

    private val _state = MutableLiveData<State>(State.ShouldTakeFingerprint)
    val state: LiveData<State> = _state

    private var fingerprint: Bitmap? = null

    fun setFingerprint(bitmap: Bitmap?) {
        if (bitmap != null) {
            fingerprint = bitmap
            _state.value = State.FingerprintTaken(bitmap)
        }
        else if (fingerprint == null) _state.value = State.WithoutFingerprint
    }

    fun processFingerprint() {
        val fingerprint = fingerprint ?: return
        _state.value = State.ProcessingFingerprint
        viewModelScope.launch {
            val fingerprintProcessed = run {
                val fingerprintMat = fingerprint.toMat()
                val fingerprintMatProcessed = fingerprintDetection.processImage(fingerprintMat)
                fingerprintMatProcessed.toBitmap()
            }
            _state.value = State.FingerprintProcessed(fingerprintProcessed)
        }
    }

    fun onFingerprintConfirmed(action: Action) {
        when (action) {
            Action.REGISTER -> _state.value = State.TakingUserDetails
            Action.LOGIN -> analyzeFingerprint()
        }
    }

    fun registerFingerprint(name: String) {
        if (name.isNotEmpty()) {
            _state.value = State.RegisteringFingerprint
        }
    }

    private fun analyzeFingerprint() {
        val fingerprint = fingerprint ?: return
        _state.value = State.AnalyzingFingerprint
    }

    sealed class State {
        object ShouldTakeFingerprint : State()
        object WithoutFingerprint : State()
        data class FingerprintTaken(val fingerprint: Bitmap) : State()
        object ProcessingFingerprint : State()
        data class FingerprintProcessed(val fingerprint: Bitmap) : State()
        object TakingUserDetails : State()
        object RegisteringFingerprint : State()
        object FingerprintRegistered : State()
        object AnalyzingFingerprint: State()
        object LoginAllowed : State()
        object LoginDenied : State()
    }

    class Factory(private val fingerprintViewModel: FingerprintViewModel) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return fingerprintViewModel as T
        }
    }
}