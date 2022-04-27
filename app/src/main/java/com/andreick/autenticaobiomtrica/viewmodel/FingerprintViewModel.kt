package com.andreick.autenticaobiomtrica.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.*
import com.andreick.autenticaobiomtrica.Action
import com.andreick.autenticaobiomtrica.FingerprintDetectionJava
import com.andreick.autenticaobiomtrica.model.User
import com.andreick.autenticaobiomtrica.toBitmap
import com.andreick.autenticaobiomtrica.toMat
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.lang.Exception

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
        } else if (fingerprint == null) _state.value = State.WithoutFingerprint
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

    fun registerFingerprint(username: String) = viewModelScope.launch {
        if (username.isEmpty()) return@launch
        val fingerprint = fingerprint ?: return@launch
        _state.value = State.RegisteringFingerprint
        try {
            val fingerprintName = username + System.currentTimeMillis()
            val uploadFingerprintTask =
                uploadImageToStorage(fingerprint, "fingerprints/", fingerprintName)
            val saveUserTask = saveUserToFirestore(username, fingerprintName)
            uploadFingerprintTask.await()
            saveUserTask.await()
            _state.value = State.FingerprintRegistered
        } catch (e: Exception) {
            Log.e(TAG, e.stackTraceToString())
            _state.value = State.FingerprintRegisterFailed
        }
    }

    private fun uploadImageToStorage(
        image: Bitmap,
        filePath: String,
        fileName: String
    ): UploadTask {
        val stream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return Firebase.storage.reference.child(filePath + fileName).putBytes(stream.toByteArray())
    }

    private fun saveUserToFirestore(username: String, fingerprintName: String): Task<DocumentReference> {
        val user = User(username, fingerprintName)
        return Firebase.firestore.collection("users").add(user)
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
        object FingerprintRegisterFailed : State()
        object FingerprintRegistered : State()
        object AnalyzingFingerprint : State()
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

    companion object {
        const val TAG = "FingerprintViewModel"
    }
}