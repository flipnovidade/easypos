package com.example.megaburguer.presenter.auth.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.megaburguer.R
import com.example.megaburguer.databinding.FragmentLoginBinding
import com.example.megaburguer.util.BaseFragment
import com.example.megaburguer.util.FirebaseHelper
import com.example.megaburguer.util.StateView
import com.example.megaburguer.util.showBottomSheet
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.megaburguer.databinding.LayoutBottomSheetRecoverPasswordBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : BaseFragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: LoginViewModel by viewModels()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initListeners()
        
    }
    
    private fun initListeners() {
        binding.btnLogin.setOnClickListener {
            validateData()
        }

        binding.btnRecoverPassword.setOnClickListener {
            showRecoverPasswordBottomSheet()
        }
    }
    
    private fun validateData() {
        val email = binding.editEmail.text.toString().trim()
        val password = binding.editPassword.text.toString().trim()
        
        if (email.isNotEmpty()) {
            if (password.isNotEmpty()) {
                hideKeyboard()
                login(email,password)
            } else {
                showBottomSheet(message = getString(R.string.txt_password_empty))
            }
        } else {
            showBottomSheet(message = getString(R.string.txt_email_empty))
        }
    }

    private fun login(email: String, password: String) {
        viewModel.login(email, password).observe(viewLifecycleOwner) { stateView ->
            when (stateView) {
                is StateView.Loading -> {
                    binding.progressBar.isVisible = true
                }

                is StateView.Success -> {
                    binding.progressBar.isVisible = false

                    viewLifecycleOwner.lifecycleScope.launch {
                        checkUserType()
                    }

                }
                
                is StateView.Error -> {
                    binding.progressBar.isVisible = false
                    showBottomSheet(message = getString((stateView.stringResId ?: R.string.error_generic)))
                }    
            }
        }

    
    }

    private suspend fun checkUserType() {
       when(FirebaseHelper.getUserType()) {
           "Administrador" -> findNavController().navigate(R.id.action_loginFragment_to_homeAdmin)
           "Garçom" -> findNavController().navigate(R.id.action_loginFragment_to_homeWaiterFragment)
           "Cozinha" -> findNavController().navigate(R.id.action_loginFragment_to_homeStaff)
           "Caixa" -> findNavController().navigate(R.id.action_loginFragment_to_homeStaff)
       }
    }

    private fun showRecoverPasswordBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialog)
        val bottomSheetBinding: LayoutBottomSheetRecoverPasswordBinding = LayoutBottomSheetRecoverPasswordBinding.inflate(layoutInflater, null, false)

        bottomSheetBinding.btnSend.setOnClickListener {
            val email = bottomSheetBinding.editEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                bottomSheetBinding.progressBar.isVisible = true
                bottomSheetBinding.btnSend.text = ""
                bottomSheetBinding.btnSend.isEnabled = false

                viewModel.recover(email).observe(viewLifecycleOwner) { stateView ->
                    when (stateView) {
                        is StateView.Loading -> {
                        }
                        is StateView.Success -> {
                            bottomSheetDialog.dismiss()
                            showBottomSheet(message = getString(R.string.link_recover_password))
                        }
                        is StateView.Error -> {
                            bottomSheetBinding.progressBar.isVisible = false
                            bottomSheetBinding.btnSend.text = getString(R.string.btn_send)
                            bottomSheetBinding.btnSend.isEnabled = true
                            showBottomSheet(message = stateView.message ?: getString(R.string.error_generic))
                        }
                    }
                }
            } else {
                showBottomSheet(message = getString(R.string.txt_email_empty))
            }
        }

        bottomSheetDialog.setContentView(bottomSheetBinding.root)
        bottomSheetDialog.show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}