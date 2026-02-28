package com.example.megaburguer.presenter.home.waiter.createOrder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.megaburguer.R
import com.example.megaburguer.data.enum.TableStatus
import com.example.megaburguer.data.model.Menu
import com.example.megaburguer.data.model.OrderItem
import com.example.megaburguer.databinding.FragmentCreateOrderBinding
import com.example.megaburguer.presenter.home.SharedOrderViewModel
import com.example.megaburguer.util.StateView
import com.example.megaburguer.util.showBottomSheet
import com.example.megaburguer.util.showObservationDialog
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateOrderFragment : Fragment() {

    private var _binding: FragmentCreateOrderBinding? = null
    private val binding get() = _binding!!
    private val args: CreateOrderFragmentArgs by navArgs()
    private lateinit var createOrderAdapter: CreateOrderAdapter
    private val viewModel: CreateOrderViewModel by viewModels()
    private val fullMenuList = mutableListOf<Menu>()
    private val sharedViewModel: SharedOrderViewModel by activityViewModels()
    private var orderSent = false
    private lateinit var typeCategory: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initListeners()

        configTitleOrder()

        configRecycleView()

        getMenus()
    }

    private fun initListeners() {
        binding.back.setOnClickListener {
            findNavController().popBackStack()
        }

        // Listener para as categorias
        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            // Como é singleSelection, a lista terá apenas um ID
            if (checkedIds.isNotEmpty()) {
                updateFilteredList()
            }
        }

        binding.btnViewOrders.setOnClickListener {
            val orderItems = sharedViewModel.currentOrderItems.value ?: emptyList()

            if (orderItems.isNotEmpty()) {
                orderSent = true
                val action =
                    CreateOrderFragmentDirections.actionCreateOrderFragmentToViewOrderFragment(
                        orderItems.toTypedArray(),
                        args.table
                    )
                findNavController().navigate(action)
            } else {
                showBottomSheet(message = getString(R.string.message_empty_order))
            }
        }
    }

    private fun configTitleOrder() {
        binding.txtTitle.text = getString(R.string.txt_title_table, args.table.number)
    }

    private fun configRecycleView() {
        createOrderAdapter = CreateOrderAdapter(
            onAddItemClick = { menu, _ -> 
                sharedViewModel.addItem(menu, args.table.id, args.table.number, args.nameUser)
            },
            quantityMap = emptyMap(),
            onAddObservationClick = { menu ->
                val quant = sharedViewModel.itemQuantityMap.value?.get(menu.id) ?: 0
                if (quant > 0) {
                    showObservationDialog(
                        nameItem = menu.nameItem,
                        priceItem = menu.price,
                        onSaveClick = { observation ->
                            sharedViewModel.updateObservation(menu.id, observation)
                        }
                    )
                } else {
                    showBottomSheet(message = getString(R.string.message_add_empty_order))
                }
            },
            onMoreClick = { menu, _ ->
                sharedViewModel.updateQuantity(menu.id, 1)
            },
            onLessClick = { menu, _ ->
                sharedViewModel.updateQuantity(menu.id, -1)
            }
        )

        with(binding.recycleView) {
            setHasFixedSize(true)
            adapter = createOrderAdapter
        }

        sharedViewModel.itemQuantityMap.observe(viewLifecycleOwner) { quantityMap ->
            createOrderAdapter.updateQuantityMap(quantityMap)
        }
    }

    private fun getMenus() {
        viewModel.getMenus().observe(viewLifecycleOwner) { stateView ->
            when (stateView) {
                is StateView.Loading -> {
                    binding.progressBar.isVisible = true
                }

                is StateView.Success -> {
                    binding.progressBar.isVisible = false

                    fullMenuList.clear()
                    fullMenuList.addAll(stateView.data ?: emptyList())

                    updateFilteredList()


                }

                is StateView.Error -> {
                    binding.progressBar.isVisible = false
                    showBottomSheet(message = stateView.message ?: getString(R.string.error_generic))
                }

            }
        }
    }

    private fun updateFilteredList() {

        val selectedChipId = binding.chipGroupCategories.checkedChipId

        when (selectedChipId) {

            R.id.chip_burgers -> {
                typeCategory = "Hambúrgueres"
            }

            R.id.chip_portions -> {
                typeCategory = "Porções"
            }

            R.id.chip_drinks -> {
                typeCategory = "Bebidas"
            }

            R.id.chip_combos -> {
                typeCategory = "Combos"
            }

        }

        // Filtra a lista completa de itens
        val filteredList = fullMenuList.filter { menu ->
            menu.category.equals(typeCategory, ignoreCase = true)
        }

        // Envia a nova lista filtrada para o adapter
        createOrderAdapter.submitList(filteredList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!orderSent) {
            sharedViewModel.setTableStatus(args.table.id, TableStatus.OPEN)
        }
        _binding = null
    }


}