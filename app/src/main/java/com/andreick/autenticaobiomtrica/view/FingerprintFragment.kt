package com.andreick.autenticaobiomtrica.view

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.andreick.autenticaobiomtrica.*
import com.andreick.autenticaobiomtrica.databinding.FragmentFingerprintBinding
import com.andreick.autenticaobiomtrica.viewmodel.FingerprintViewModel

class FingerprintFragment : Fragment() {

    private lateinit var binding: FragmentFingerprintBinding

    private val args: FingerprintFragmentArgs by navArgs()

    private val viewModel: FingerprintViewModel by viewModels {
        FingerprintViewModel.Factory(FingerprintViewModel(
            FingerprintDetectionJava()
        ))
    }

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
                        requireContext().showToast("Escolha a imagem da sua digital")
                    } else {
                        requireContext().showToast("Permissão necessária para tirar a digital")
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
                    binding.pbProcessing.visible()
                }
                is FingerprintViewModel.State.FingerprintProcessed -> {
                    binding.pbProcessing.gone()
                    binding.ivFingerprint.setImageBitmap(state.fingerprint)
                    showFingerprintProcessedButtons()
                }
                FingerprintViewModel.State.TakingUserDetails -> {
                    val action = FingerprintFragmentDirections.actionShowUserDetailsDialog()
                    findNavController().navigate(action)
                }
                FingerprintViewModel.State.RegisteringFingerprint -> {
                    binding.pbProcessing.visible()
                }
                FingerprintViewModel.State.FingerprintRegistered -> {
                    binding.pbProcessing.gone()
                    requireContext().showToast("Digital registrada com sucesso")
                }
                FingerprintViewModel.State.AnalyzingFingerprint -> TODO()
                FingerprintViewModel.State.LoginAllowed -> TODO()
                FingerprintViewModel.State.LoginDenied -> TODO()
            }
        }
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
            .savedStateHandle.getLiveData<String>("name")
            .observe(viewLifecycleOwner) { name -> viewModel.registerFingerprint(name) }
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