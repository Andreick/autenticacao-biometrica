package com.andreick.autenticaobiomtrica.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.andreick.autenticaobiomtrica.R
import com.andreick.autenticaobiomtrica.databinding.FragmentUserDetailsBinding
import com.andreick.autenticaobiomtrica.enums.AccessLevel
import com.andreick.autenticaobiomtrica.extensions.showShortToast
import com.andreick.autenticaobiomtrica.model.User

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

        binding.spinnerUserDetailsRoles.adapter = ArrayAdapter(
            requireContext(),
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
            AccessLevel.values().map { it.role }
        )

        setOnclickListeners()
    }

    private fun setOnclickListeners() {
        binding.btnUserDetailsRegister.setOnClickListener {
            val name = binding.tietUserDetailsName.text?.toString() ?: ""
            if (name.isNotEmpty()) {
                val accessLevelOrdinal = binding.spinnerUserDetailsRoles.selectedItemPosition
                val user = User(name, AccessLevel.values()[accessLevelOrdinal])
                findNavController().previousBackStackEntry?.savedStateHandle?.set("user", user)
                dismiss()
            }
            else {
                showShortToast("Preencha o nome")
            }
        }

        binding.btnUserDetailsCancel.setOnClickListener { dismiss() }
    }
}