package com.andreick.autenticaobiomtrica.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.andreick.autenticaobiomtrica.UserAction
import com.andreick.autenticaobiomtrica.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        binding.btnHomeRegister.setOnClickListener {
            val action = HomeFragmentDirections.takeFingerprint(UserAction.REGISTER)
            findNavController().navigate(action)
        }
        binding.btnHomeLogin.setOnClickListener {
            val action = HomeFragmentDirections.takeFingerprint(UserAction.LOGIN)
            findNavController().navigate(action)
        }
    }
}