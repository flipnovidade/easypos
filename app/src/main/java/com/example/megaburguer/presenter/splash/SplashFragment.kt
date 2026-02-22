package com.example.megaburguer.presenter.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.megaburguer.R
import com.example.megaburguer.databinding.FragmentSplashBinding
import com.example.megaburguer.util.FirebaseHelper
import com.example.megaburguer.util.SharedPreferencesHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashFragment : Fragment() {
    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            delay(3000)
            checkAuth()
        }
    }

    private suspend fun checkAuth() {

        val userType = sharedPreferencesHelper.getUserType()

        when(userType) {
            // If user is saved locally, navigate based on that
            "Garçom" ->  findNavController().navigate(R.id.action_splashFragment_to_homeWaiterFragment)
            "Cozinha" -> findNavController().navigate(R.id.action_splashFragment_to_homeKitchenFragment)
            "Caixa" -> findNavController().navigate(R.id.action_splashFragment_to_homeStaff)
            else -> {

                if (FirebaseHelper.isAuthenticated()) {
                    // Fallback to Firebase check if not found locally or if type is Admin/other
                    when(FirebaseHelper.getUserType()) {
                        "Administrador" -> findNavController().navigate(R.id.action_splashFragment_to_homeAdminFragment)
                        "Garçom" ->  findNavController().navigate(R.id.action_splashFragment_to_homeWaiterFragment)
                        "Cozinha" -> findNavController().navigate(R.id.action_splashFragment_to_homeKitchenFragment)
                        "Caixa" -> findNavController().navigate(R.id.action_splashFragment_to_homeStaff)
                    }
                } else {
                    findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
                }

            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}