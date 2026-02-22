package com.example.megaburguer.presenter.home.admin.choose_user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.megaburguer.databinding.FragmentChooseUserBinding
import com.example.megaburguer.util.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class ChooseUserFragment : BaseFragment() {
    private var _binding: FragmentChooseUserBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChooseUserViewModel by viewModels()
    private val adapter by lazy { ChooseUserAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChooseUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        initObservers()
        viewModel.getUsers().observe(viewLifecycleOwner) { state ->
            when (state) {
                is com.example.megaburguer.util.StateView.Loading -> {
                    // Show loading if needed
                }
                is com.example.megaburguer.util.StateView.Success -> {
                    adapter.submitList(state.data)
                }
                is com.example.megaburguer.util.StateView.Error -> {
                    // Handle error
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerUsers.adapter = adapter
    }

    private fun initObservers() {
        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }

        adapter.setOnClickListener { user ->
            viewModel.saveUser(user)
            when (user.typeUser) {
                "Garçom" -> findNavController().navigate(com.example.megaburguer.R.id.homeWaiter)
                "Cozinha" -> findNavController().navigate(com.example.megaburguer.R.id.homeKitchen)
                "Caixa" -> findNavController().navigate(com.example.megaburguer.R.id.homeStaff)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
