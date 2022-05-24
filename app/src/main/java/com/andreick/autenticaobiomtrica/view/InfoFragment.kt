package com.andreick.autenticaobiomtrica.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.andreick.autenticaobiomtrica.databinding.FragmentInfoBinding
import com.andreick.autenticaobiomtrica.viewmodel.InfoViewModel

class InfoFragment : Fragment() {

    private lateinit var binding: FragmentInfoBinding
    private val infoViewModel: InfoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvInfo.layoutManager = LinearLayoutManager(requireContext())
        binding.rvInfo.adapter = InfoAdapter()
        setObservers()
    }

    private fun setObservers() {
        infoViewModel.infos.observe(viewLifecycleOwner) {
            binding.rvInfo.adapter = InfoAdapter(it)
        }
    }
}