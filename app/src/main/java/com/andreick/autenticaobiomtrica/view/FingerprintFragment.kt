package com.andreick.autenticaobiomtrica.view

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.andreick.autenticaobiomtrica.R
import com.andreick.autenticaobiomtrica.databinding.FragmentFingerprintBinding
import com.andreick.autenticaobiomtrica.extensions.*
import com.andreick.autenticaobiomtrica.model.User
import com.andreick.autenticaobiomtrica.viewmodel.FingerprintViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FingerprintFragment : Fragment() {

    private lateinit var binding: FragmentFingerprintBinding

    private val args: FingerprintFragmentArgs by navArgs()

    private val viewModel: FingerprintViewModel by viewModels()

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        for((permission, isGranted) in permissions.entries) {
            when (permission) {
                Manifest.permission.READ_EXTERNAL_STORAGE -> {
                    if (isGranted) {
                        val pickIntent = Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                        openGalleryLauncher.launch(pickIntent)
                        showLongToast("Escolha a imagem da sua digital")
                    } else {
                        showShortToast("Permissão necessária para tirar a digital")
                        findNavController().popBackStack()
                    }
                }
                else -> throw RuntimeException("Unhandled permission: $permission")
            }
        }
    }

    private val openGalleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        var fingerprint: Bitmap? = null
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            result.data?.data?.let { uri ->
                context?.contentResolver?.let { contentResolver ->
                    fingerprint = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                }
            }
        }
        viewModel.setFingerprint(fingerprint)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFingerprintBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
        setOnCLickListeners()
        setBackStackEntryObserver()
    }

    private fun setObservers() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when(state) {
                is FingerprintViewModel.State.ShouldTakeFingerprint -> {
                    takeFingerprint()
                }
                is FingerprintViewModel.State.FingerprintTaken -> {
                    binding.ivFingerprint.setImageBitmap(state.fingerprint)
                    showFingerprintLoadedButtons()
                }
                FingerprintViewModel.State.WithoutFingerprint -> {
                    findNavController().popBackStack()
                }
                FingerprintViewModel.State.ProcessingFingerprint -> {
                    hideButtons()
                    loading()
                }
                is FingerprintViewModel.State.FingerprintProcessed -> {
                    loadingFinished()
                    binding.ivFingerprint.setImageBitmap(state.fingerprint)
                    showFingerprintProcessedButtons()
                }
                FingerprintViewModel.State.TakingUserDetails -> {
                    val direction = FingerprintFragmentDirections.showUserDetailsDialog()
                    findNavController().navigate(direction)
                }
                is FingerprintViewModel.State.RegisteringFingerprint -> {
                    hideButtons()
                    binding.ivFingerprint.setImageBitmap(state.fingerprint)
                    loading()
                }
                FingerprintViewModel.State.FingerprintRegistered -> {
                    onSuccess("Digital registrada com sucesso")
                }
                FingerprintViewModel.State.FingerprintRegisterFailed -> {
                    onFail("Falha ao registrar a digital")
                }
                is FingerprintViewModel.State.AnalyzingFingerprint -> {
                    hideButtons()
                    binding.ivFingerprint.setImageBitmap(state.fingerprint)
                    loading()
                }
                is FingerprintViewModel.State.LoginAllowed -> {
                    showLongToast("Bem vindo ${state.username}!")
                    findNavController().navigate(R.id.infoFragment)
                    enableInput()
                }
                FingerprintViewModel.State.LoginDenied -> {
                    onFail("Sua digital não foi identificada, tente novamente")
                }
                FingerprintViewModel.State.LoginFailed -> {
                    onFail("Falha ao identificar a sua digital")
                }
            }
        }
    }

    private fun onSuccess(message: String) {
        showLongToast(message)
        findNavController().popBackStack()
        enableInput()
    }

    private fun onFail(message: String) {
        loadingFinished()
        showLongToast(message)
        showFingerprintProcessedButtons()
    }

    private fun setOnCLickListeners() {
        binding.btnRetake.setOnClickListener {
            takeFingerprint()
        }
        binding.btnProcess.setOnClickListener {
            viewModel.processFingerprint()
        }
        binding.btnConfirm.setOnClickListener {
            viewModel.onFingerprintConfirmed(args.action)
        }
    }

    private fun takeFingerprint() {
        val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        requestPermissions.launch(permissions)
    }

    private fun setBackStackEntryObserver() {
        findNavController().getBackStackEntry(R.id.fingerprintFragment)
            .savedStateHandle.getLiveData<User>("user")
            .observe(viewLifecycleOwner) { user -> viewModel.registerFingerprint(user) }
    }

    private fun loading() {
        disableInput()
        binding.pbProcessing.visible()
    }

    private fun loadingFinished() {
        enableInput()
        binding.pbProcessing.gone()
    }

    private fun hideButtons() {
        binding.btnRetake.invisible()
        binding.btnProcess.invisible()
        binding.btnConfirm.invisible()
    }

    private fun showFingerprintProcessedButtons() {
        binding.btnRetake.visible()
        binding.btnProcess.invisible()
        binding.btnConfirm.visible()
    }

    private fun showFingerprintLoadedButtons() {
        binding.btnRetake.visible()
        binding.btnProcess.visible()
        binding.btnConfirm.invisible()
    }
}