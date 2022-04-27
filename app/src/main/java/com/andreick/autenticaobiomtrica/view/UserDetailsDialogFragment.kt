package com.andreick.autenticaobiomtrica.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.andreick.autenticaobiomtrica.R
import com.andreick.autenticaobiomtrica.databinding.FragmentUserDetailsBinding
import com.andreick.autenticaobiomtrica.extensions.showToast

class UserDetailsDialogFragment : DialogFragment() {

    private lateinit var binding: FragmentUserDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_DialogOverlay)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentUserDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnUserDetailsRegister.setOnClickListener {
            val name = binding.tietUserDetailsName.text.toString()
            if (name.isNotEmpty()) {
                findNavController().previousBackStackEntry?.savedStateHandle?.set("name", name)
                dismiss()
            }
            else {
                showToast("Preencha o nome")
            }
        }

        binding.btnUserDetailsCancel.setOnClickListener { dismiss() }
    }
}