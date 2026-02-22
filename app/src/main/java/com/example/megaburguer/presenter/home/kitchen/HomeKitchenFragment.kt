package com.example.megaburguer.presenter.home.kitchen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.megaburguer.R
import com.example.megaburguer.data.model.OrderItem
import com.example.megaburguer.databinding.FragmentHomeKitchenBinding
import com.example.megaburguer.presenter.home.SharedOrderViewModel
import com.example.megaburguer.util.PrinterHelper
import com.example.megaburguer.util.SharedPreferencesHelper
import com.example.megaburguer.util.showBottomSheet
import com.example.megaburguer.util.showKitchenOptionsBottomSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class HomeKitchenFragment : Fragment() {

    private var _binding: FragmentHomeKitchenBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeKitchenViewModel by viewModels()

    private val sharedViewModel: SharedOrderViewModel by activityViewModels()

    @Inject
    lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeKitchenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        initListeners()
        initObservers()
    }

    private val adapter by lazy {
        HomeKitchenAdapter(
            onMarkAsPrintedClick = { item ->
                val ids = if (item.groupedIds.isNotEmpty()) item.groupedIds else listOf(item.id)
                viewModel.markAsPrinted(ids)
            },
            onPrintItemClick = { item ->

                if (item != null) {
                    if (hasBluetoothPermission()) {
                        printIndividualItem(item)
                    } else {
                        showBottomSheet(message = getString(R.string.txt_message_not_permission_bluetooth))
                    }
                } else {
                    showBottomSheet(message = getString(R.string.txt_message_print_bottom_sheet_kitchen_pedido))
                }

            }
        )
    }

    private fun hasBluetoothPermission(): Boolean {
        // Se for Android antigo (< 12), sempre retorna true (permissão é dada na instalação)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }

        // Se for Android novo (12+), verifica se foi concedida
        val connectGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

        val scanGranted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED

        return connectGranted && scanGranted
    }

    private fun printIndividualItem(item: OrderItem) {

        lifecycleScope.launch(Dispatchers.IO) {

            val ptBr = java.util.Locale.forLanguageTag("pt-BR")
            val formattedTime = java.text.SimpleDateFormat("HH:mm", ptBr).format(java.util.Date(item.date))

            var result = PrinterHelper().printKitchenOrder(
                tableName = item.nameTable,
                waiterName = item.nameWaiter,
                items = item.nameItem, // In aggregated mode, this is the multiline summary
                observations = item.observation,
                orderTime = formattedTime
            )

            if (result == "Success") {
                Toast.makeText(requireContext(), getString(R.string.txt_message_send_success), Toast.LENGTH_SHORT).show()
            } else {
                showBottomSheet(message = result)
            }

        }
    }

    private fun setupRecyclerView() {
        binding.textGreeting.text = getString(R.string.txt_greeting_staff, sharedPreferencesHelper.getUserName()!!)
        binding.recyclerView.adapter = adapter
    }

    private fun initObservers() {
        viewModel.observeOrderPrint().observe(viewLifecycleOwner) { state ->
            when (state) {
                is com.example.megaburguer.util.StateView.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is com.example.megaburguer.util.StateView.Success -> {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(state.data)
                }
                is com.example.megaburguer.util.StateView.Error -> {
                    binding.progressBar.visibility = View.GONE
                    // Handle error if needed
                }
            }
        }
    }

    private fun initListeners() {

        binding.btnLogout.setOnClickListener {
            showBottomSheet(
                message = getString(R.string.msg_bottom_sheet_logout),
                titleButton = R.string.btn_bottom_sheet_logout,
                onClick = {
                    sharedViewModel.logoutApp()
                    findNavController().navigate(
                        R.id.loginFragment, null,
                        NavOptions.Builder().setPopUpTo(R.id.homeStaff, true).build()
                    )
                }
            )

        }

        binding.btnConfig.setOnClickListener {
            showKitchenOptionsBottomSheet(
                currentMode = viewModel.displayMode.value,
                onOptionSelected = { mode ->
                    viewModel.setDisplayMode(mode)
                }
            )
        }

    }

    override fun onResume() {
        super.onResume()

    }

    override fun onPause() {
        super.onPause()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}