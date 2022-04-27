package com.andreick.autenticaobiomtrica.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.*
import com.andreick.autenticaobiomtrica.*
import com.andreick.autenticaobiomtrica.extensions.toBitmap
import com.andreick.autenticaobiomtrica.extensions.toMat
import com.andreick.autenticaobiomtrica.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.lang.RuntimeException

class FingerprintViewModel(
    private val fingerprintDetection: FingerprintDetectionJava,
    private val fingerprintMatcher: FingerprintMatcher
) : ViewModel() {

    private val _state = MutableLiveData<State>(State.ShouldTakeFingerprint)
    val state: LiveData<State> = _state

    private var fingerprint: Bitmap? = null
    private val userCollectionRef by lazy(LazyThreadSafetyMode.NONE) {
        Firebase.firestore.collection("users")
    }

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
                uploadImageToStorage(fingerprint, FINGERPRINTS_FOLDER, fingerprintName)
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
        return userCollectionRef.add(user)
    }

    private fun analyzeFingerprint() = viewModelScope.launch {
        val fingerprint = fingerprint ?: return@launch
        _state.value = State.AnalyzingFingerprint
        try {
            val fingerprintName = getMatchedFingerprintName(fingerprint)
            if (fingerprintName != null) {
                val user = getUser(fingerprintName)
                if (user != null) {
                    _state.value = State.LoginAllowed(user.name)
                } else {
                    throw RuntimeException("User not found")
                }
            } else {
                _state.value = State.LoginDenied
            }
        } catch (e: Exception) {
            Log.e(TAG, e.stackTraceToString())
            _state.value = State.LoginFailed
        }
    }

    private suspend fun getMatchedFingerprintName(fingerprint: Bitmap): String? {
        val items = Firebase.storage.reference.child(FINGERPRINTS_FOLDER).listAll().await().items
        val fingerprintMap = mutableMapOf<Mat, String>()
        for (item in items) {
            val bytes = item.getBytes(MAX_DOWNLOAD_SIZE)
            val name = item.name
            val mat = Imgcodecs.imdecode(MatOfByte(*bytes.await()), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED)
            fingerprintMap[mat] = name
        }
        val matchedFingerprint = fingerprintMatcher.matchFingerprints(fingerprint.toMat(), fingerprintMap.keys)
        return fingerprintMap[matchedFingerprint]
    }

    private suspend fun getUser(fingerprintName: String): User? {
        val querySnapshot = userCollectionRef.whereEqualTo("fingerprintName", fingerprintName).get().await()
        val document = querySnapshot.documents[0]
        return document.toObject<User>()
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
        data class LoginAllowed(val username: String) : State()
        object LoginDenied : State()
        object LoginFailed : State()
    }

    class Factory(private val fingerprintViewModel: FingerprintViewModel) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return fingerprintViewModel as T
        }
    }

    companion object {
        private const val TAG = "FingerprintViewModel"
        private const val FINGERPRINTS_FOLDER = "fingerprints/"
        private const val MAX_DOWNLOAD_SIZE = 1L * 1024 * 1024
    }
}