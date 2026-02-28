package com.example.megaburguer.presenter.home.waiter.createOrder.viewOrder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.megaburguer.R
import com.example.megaburguer.data.enum.TableStatus
import com.example.megaburguer.data.model.OrderItem
import com.example.megaburguer.databinding.FragmentViewOrderBinding
import com.example.megaburguer.presenter.home.SharedOrderViewModel
import com.example.megaburguer.util.FirebaseHelper
import com.example.megaburguer.util.GetMask
import com.example.megaburguer.util.StateView
import com.example.megaburguer.util.showBottomSheet
import com.example.megaburguer.util.showViewObservationDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

@AndroidEntryPoint
class ViewOrderFragment : Fragment() {
    private var _binding: FragmentViewOrderBinding? = null
    private val binding get() = _binding!!
    private val args: ViewOrderFragmentArgs by navArgs()
    private val viewModel: ViewOrderViewModel by viewModels()
    private val sharedViewModel: SharedOrderViewModel by activityViewModels()
    private lateinit var viewOrderAdapter: ViewOrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentViewOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()

        configRecyclerView()

        configInformation()
    }

    private fun initListeners() {

        binding.back.setOnClickListener {
            findNavController().popBackStack()

        }

        binding.btnSendKitchen.setOnClickListener {
            val itemsUpdate = sharedViewModel.currentOrderItems.value ?: emptyList()

            saveOrderItemList(itemsUpdate)
        }

    }



    private fun getUser() {
        viewModel.getUser().observe(viewLifecycleOwner) { stateView ->
            when(stateView) {
                is StateView.Loading -> {

                }

                is StateView.Success -> {
                    binding.txtNameWaiter.text = stateView.data?.name
                }

                is StateView.Error -> {
                    showBottomSheet(message = stateView.message ?: getString(R.string.error_generic))
                }

            }
        }

    }

    private fun saveOrderItemList(orderItemList: List<OrderItem>) {
        viewModel.saveOrderItemList(orderItemList).observe(viewLifecycleOwner) { stateView ->
            when(stateView) {
                is StateView.Loading -> {

                }

                is StateView.Success -> {
                    saveOrderPrint(orderItemList)
                }

                is StateView.Error -> {
                    showBottomSheet(message = stateView.message ?: getString(R.string.error_generic))
                }
            }
        }
    }

    private fun saveOrderPrint(orderPrintList: List<OrderItem>) {
        viewModel.saveOrderPrintList(orderPrintList).observe(viewLifecycleOwner) { stateView ->
            when(stateView) {
                is StateView.Loading -> {

                }

                is StateView.Success -> {

                    val bundle = Bundle().apply {
                        putBoolean("send_success", true)
                        putString("order_print_message", getString(R.string.txt_message_order_send_success))
                    }

                    // Define o resultado para a tela anterior pegar
                    setFragmentResult("close_request", bundle)

                    sharedViewModel.setTableStatus(args.table.id, TableStatus.OPEN)
                    sharedViewModel.clearOrder()

                    findNavController().navigate(R.id.homeWaiter, null,
                        NavOptions.Builder().setPopUpTo(R.id.homeWaiter, false).build())
                }

                is StateView.Error -> {
                    showBottomSheet(message = stateView.message ?: getString(R.string.error_generic))
                }

            }
        }
    }

    private fun configInformation() {
        binding.txtSubtitle.text = getString(R.string.txt_title_table, args.table.number)
        getUser()
        
        sharedViewModel.currentOrderItems.observe(viewLifecycleOwner) { items ->
            viewOrderAdapter.submitList(items)
            updateTotals(items)
        }
    }

    private fun configRecyclerView() {
        viewOrderAdapter = ViewOrderAdapter(
            onRemoveItemClick = { orderItem, _ ->
                sharedViewModel.removeItem(orderItem.idItem)
            },
            onViewObservationClick = { orderItem ->
                showViewObservationDialog(
                    nameItem = orderItem.nameItem,
                    priceItem = orderItem.price,
                    observationItem = orderItem.observation,
                )
            },
            onMoreClick = { orderItem, _ ->
                sharedViewModel.updateQuantity(orderItem.idItem, 1)
            },
            onLessClick = { orderItem, _ ->
                sharedViewModel.updateQuantity(orderItem.idItem, -1)
            }
        )

        with(binding.recycleView) {
            setHasFixedSize(true)
            adapter = viewOrderAdapter
        }
    }

    private fun updateTotals(items: List<OrderItem>) {
        val totalItems = items.sumOf { it.quantity }
        val totalPrice = items.sumOf { (it.price * it.quantity).toDouble() }

        binding.txtTotalItemsNumber.text = totalItems.toString()
        binding.txtTotalValueReal.text = getString(R.string.txt_price_snack_manage_menu, GetMask.getFormatedValue(totalPrice.toFloat()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
}