package com.example.megaburguer.presenter.home.waiter

import android.os.Bundle
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.megaburguer.R
import com.example.megaburguer.data.enum.TableStatus
import com.example.megaburguer.data.model.Table
import com.example.megaburguer.databinding.FragmentHomeWaiterBinding
import com.example.megaburguer.data.model.Menu
import com.example.megaburguer.data.model.OrderItem
import com.example.megaburguer.presenter.home.SharedOrderViewModel
import com.example.megaburguer.presenter.home.waiter.createOrder.CreateOrderAdapter
import com.example.megaburguer.presenter.home.waiter.createOrder.CreateOrderViewModel
import com.example.megaburguer.presenter.home.waiter.createOrder.viewOrder.ViewOrderAdapter
import com.example.megaburguer.presenter.home.waiter.createOrder.viewOrder.ViewOrderViewModel
import com.example.megaburguer.util.GetMask
import com.example.megaburguer.util.SharedPreferencesHelper
import com.example.megaburguer.util.StateView
import com.example.megaburguer.util.showBottomSheet
import com.example.megaburguer.util.showObservationDialog
import com.example.megaburguer.util.showViewObservationDialog
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.getValue

@AndroidEntryPoint
class HomeWaiterFragment : Fragment() {

    private var _binding: FragmentHomeWaiterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeWaiterViewModel by viewModels()
    private val createOrderViewModel: CreateOrderViewModel by viewModels()
    private val viewOrderViewModel: ViewOrderViewModel by viewModels()
    
    private lateinit var homeWaiterAdapter: HomeWaiterAdapter
    private lateinit var createOrderAdapter: CreateOrderAdapter
    private lateinit var viewOrderAdapter: ViewOrderAdapter
    
    private val sharedViewModel: SharedOrderViewModel by activityViewModels()
    private lateinit var nameUser: String
    
    // Tablet State
    private var isTablet = false
    private var selectedTable: Table? = null
    private val fullMenuList = mutableListOf<Menu>()
    private val itemQuantityMap = mutableMapOf<String, Int>() // id do item -> quantidade
    private val currentOrderItems = mutableListOf<OrderItem>()
    private val observationSaveMap = mutableMapOf<String, String>()
    private var typeCategory: String = "Hambúrgueres"
    val tenMinutes: Long = 10L * 60L * 1000L
    private val handler = android.os.Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {

            val currentList = homeWaiterAdapter.currentList

            releaseTrappedTables(currentList)
            handler.postDelayed(this, 10000) // a cada 10 segundos
        }
    }

    @Inject
    lateinit var sharedPreferencesHelper: SharedPreferencesHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        _binding = FragmentHomeWaiterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isTablet = binding.root.findViewById<View>(R.id.container_menu_tablet) != null

        initListeners()

        getUser()
        
        configRecycleView()

        getTables()

        observeTables()

        openTable()

        if (isTablet) {
            configTabletMenu()
            getMenus()
        }
    }

    private fun initListeners() {

        binding.btnLogout.setOnClickListener {
            showBottomSheet(
                message = getString(R.string.msg_bottom_sheet_logout),
                titleButton = R.string.btn_bottom_sheet_logout,
                onClick = {
                    sharedViewModel.logoutApp()
                    findNavController().navigate(R.id.loginFragment, null,
                        NavOptions.Builder().setPopUpTo(R.id.homeWaiter, true).build())
                }
            )

        }

        if (isTablet) {
            binding.root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chip_group_categories)?.setOnCheckedStateChangeListener { _, checkedIds ->
                if (checkedIds.isNotEmpty()) {
                    updateFilteredList()
                }
            }

            binding.root.findViewById<View>(R.id.btn_send_kitchen_tablet)?.setOnClickListener {
                if (currentOrderItems.isNotEmpty()) {
                    saveOrderItemList(currentOrderItems)
                } else {
                    showBottomSheet(message = getString(R.string.message_empty_order))
                }
            }
        }

    }

    private fun getUser() {
        viewModel.getUser().observe(viewLifecycleOwner) { stateView ->
            when(stateView) {
                is StateView.Loading -> {

                }

                is StateView.Success -> {
                    binding.textGreeting.text = getString(R.string.txt_greeting_waiter, stateView.data?.name)
                    nameUser = stateView.data?.name.toString()
                }

                is StateView.Error -> {
                    binding.textGreeting.text = getString(R.string.txt_greeting_waiter_sub)
                    showBottomSheet(message = stateView.message ?: getString(R.string.error_generic))
                }

            }
        }

        // Escuta se veio algum resultado da tela anterior
        setFragmentResultListener("close_request") { _, bundle ->
            val orderSend = bundle.getBoolean("send_success")
            val orderMessage = bundle.getString("order_print_message")

            if (orderSend) {
                Toast.makeText(requireContext(), orderMessage, Toast.LENGTH_SHORT).show()
            }

            val extractClean = bundle.getBoolean("extract_clean")
            val extractMessage = bundle.getString("extract_message")
            if (extractClean) {
                Toast.makeText(requireContext(), extractMessage, Toast.LENGTH_SHORT).show()
            }

        }

    }
    
    private fun configRecycleView() {
        homeWaiterAdapter = HomeWaiterAdapter (
            onTableClick = { table, position ->
                if (table.status == TableStatus.OPEN || table.lockedBy == sharedPreferencesHelper.getUserId()!!) {
                    if (isTablet) {
                        selectTableTablet(table)
                    } else {
                        homeWaiterAdapter.notifyItemChanged(position)
                        updateTableStatus(table.id, TableStatus.CLOSED, sharedPreferencesHelper.getUserId()!!)
                        val action = HomeWaiterFragmentDirections.actionHomeWaiterFragmentToCreateOrderFragment(table, nameUser)
                        findNavController().navigate(action)
                    }
                } else {
                    Toast.makeText(requireContext(), getString(R.string.txt_table_busy_home_waiter), Toast.LENGTH_SHORT).show()
                }

            }

        )

        with(binding.recyclerView){
            setHasFixedSize(true)
            adapter = homeWaiterAdapter
        }

    }
    
    private fun getTables() {
        viewModel.getTables().observe(viewLifecycleOwner) { stateView ->
            when(stateView) {
                
                is StateView.Loading -> {
                    binding.progressBar.isVisible = true
                }
                
                is StateView.Success -> {
                    binding.progressBar.isVisible = false
                    homeWaiterAdapter.submitList(stateView.data)
                }

                is StateView.Error -> {
                    binding.progressBar.isVisible = false
                    showBottomSheet(message = stateView.message ?: getString(R.string.error_generic))
                }
                
            }

        }
    }

    private fun releaseTrappedTables(tables: List<Table>) {
        val now = System.currentTimeMillis()
        tables.forEach { table ->
            if (table.status == TableStatus.CLOSED && (now - table.lastUpdated > tenMinutes)) {
                updateTableStatus(table.id, TableStatus.OPEN)
            }
        }
    }

    private fun updateTableStatus(tableId: String, newStatus: TableStatus, userId: String = "") {
        viewModel.updateTableStatus(tableId, newStatus, userId).observe(viewLifecycleOwner) { stateView ->
            when(stateView) {
                is StateView.Loading -> {

                }

                is StateView.Success -> {

                }

                is StateView.Error -> {
                    showBottomSheet(message = stateView.message ?: getString(R.string.error_generic))
                }

            }
        }
    }

    private fun observeTables() {
        viewModel.observeTables().observe(viewLifecycleOwner){ stateView ->
            when(stateView) {
                is StateView.Loading -> {

                }

                is StateView.Success -> {
                    homeWaiterAdapter.submitList(stateView.data)

                }

                is StateView.Error -> {
                    showBottomSheet(message = stateView.message ?: getString(R.string.error_generic))
                }
            }

        }
    }

    private fun openTable() {
        sharedViewModel.tableStatusEvent.observe(viewLifecycleOwner) { pair ->
            pair?.let { (tableId, status) ->
                updateTableStatus(tableId, status)
                sharedViewModel.consumeEvent() // Limpa o evento após consumir
            }
        }
    }

    private fun selectTableTablet(table: Table) {
        selectedTable = table
        updateTableStatus(table.id, TableStatus.CLOSED, sharedPreferencesHelper.getUserId()!!)
        
        binding.root.findViewById<View>(R.id.container_menu_tablet)?.isVisible = true
        binding.root.findViewById<View>(R.id.container_order_tablet)?.isVisible = true
        binding.root.findViewById<TextView>(R.id.txt_tablet_table_title)?.text = getString(R.string.txt_title_table, table.number)
        
        // Clear previous order state
        itemQuantityMap.clear()
        currentOrderItems.clear()
        observationSaveMap.clear()
        createOrderAdapter.notifyDataSetChanged()
        viewOrderAdapter.submitList(emptyList())
        updateTotals()
    }

    private fun configTabletMenu() {
        createOrderAdapter = CreateOrderAdapter(
            onAddItemClick = { menu, position -> onAddItem(menu, position) },
            quantityMap = itemQuantityMap,
            onAddObservationClick = { menu ->
                val quant = itemQuantityMap[menu.id] ?: 0
                if (quant > 0) {
                    showObservationDialog(
                        nameItem = menu.nameItem,
                        priceItem = menu.price,
                        onSaveClick = { observation ->
                            if (observation.isNotEmpty()) {
                                observationSaveMap[menu.id] = observation
                            } else {
                                observationSaveMap.remove(menu.id)
                            }
                            
                            // Update existing OrderItem if it's already in the list
                            val index = currentOrderItems.indexOfFirst { it.idItem == menu.id }
                            if (index != -1) {
                                currentOrderItems[index] = currentOrderItems[index].copy(observation = observation)
                                viewOrderAdapter.submitList(currentOrderItems.toList())
                            }
                        }
                    )
                } else {
                    showBottomSheet(message = getString(R.string.message_add_empty_order))
                }
            }
        )

        binding.root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycleViewMenu)?.apply {
            setHasFixedSize(true)
            adapter = createOrderAdapter
        }

        viewOrderAdapter = ViewOrderAdapter(
            onRemoveItemClick = { orderItem, position ->
                itemQuantityMap.remove(orderItem.idItem) // Note: using idItem here
                currentOrderItems.removeAt(position)
                viewOrderAdapter.submitList(currentOrderItems.toList())
                createOrderAdapter.notifyDataSetChanged()
                updateTotals()
            },
            onViewObservationClick = { orderItem ->
                showViewObservationDialog(
                    nameItem = orderItem.nameItem,
                    priceItem = orderItem.price,
                    observationItem = orderItem.observation,
                )
            },
            onMoreClick = { orderItem, position ->
                val currentQtd = itemQuantityMap[orderItem.idItem] ?: orderItem.quantity
                itemQuantityMap[orderItem.idItem] = currentQtd + 1
                currentOrderItems[position] = orderItem.copy(quantity = currentQtd + 1)
                viewOrderAdapter.notifyItemChanged(position)
                createOrderAdapter.notifyDataSetChanged()
                updateTotals()
            },
            onLessClick = { orderItem, position ->
                val currentQtd = itemQuantityMap[orderItem.idItem] ?: orderItem.quantity
                if (currentQtd > 1) {
                    itemQuantityMap[orderItem.idItem] = currentQtd - 1
                    currentOrderItems[position] = orderItem.copy(quantity = currentQtd - 1)
                    viewOrderAdapter.notifyItemChanged(position)
                    createOrderAdapter.notifyDataSetChanged()
                    updateTotals()
                }
            },
            quantityMap = itemQuantityMap
        )

        binding.root.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycleViewOrder)?.apply {
            setHasFixedSize(true)
            adapter = viewOrderAdapter
        }
    }

    private fun onAddItem(menu: Menu, position: Int) {
        val currentQtd = itemQuantityMap[menu.id] ?: 0
        itemQuantityMap[menu.id] = currentQtd + 1
        createOrderAdapter.notifyItemChanged(position)

        val existingItemIndex = currentOrderItems.indexOfFirst { it.idItem == menu.id }
        if (existingItemIndex != -1) {
            currentOrderItems[existingItemIndex] = currentOrderItems[existingItemIndex].copy(quantity = currentQtd + 1)
            viewOrderAdapter.notifyItemChanged(existingItemIndex)
        } else {
            val orderItem = OrderItem(
                id = FirebaseDatabase.getInstance().reference.push().key ?: "",
                idItem = menu.id,
                idTable = selectedTable?.id ?: "",
                nameTable = selectedTable?.number ?: "",
                nameWaiter = nameUser,
                nameItem = menu.nameItem,
                price = menu.price,
                quantity = 1,
                observation = observationSaveMap[menu.id] ?: "",
                category = menu.category,
                date = System.currentTimeMillis()
            )
            currentOrderItems.add(orderItem)
            viewOrderAdapter.submitList(currentOrderItems.toList())
        }
        updateTotals()
    }

    private fun updateTotals() {
        val totalItems = currentOrderItems.sumOf { it.quantity }
        val totalPrice = currentOrderItems.sumOf { (it.price * it.quantity).toDouble() }

        binding.root.findViewById<TextView>(R.id.txt_total_items_tablet)?.text = totalItems.toString()
        binding.root.findViewById<TextView>(R.id.txt_total_value_tablet)?.text = 
            getString(R.string.txt_price_snack_manage_menu, GetMask.getFormatedValue(totalPrice.toFloat()))
    }

    private fun getMenus() {
        createOrderViewModel.getMenus().observe(viewLifecycleOwner) { stateView ->
            val progressBarMenu = binding.root.findViewById<ProgressBar>(R.id.progressBarMenu)
            when (stateView) {
                is StateView.Loading -> {
                    progressBarMenu?.isVisible = true
                }
                is StateView.Success -> {
                    progressBarMenu?.isVisible = false
                    fullMenuList.clear()
                    fullMenuList.addAll(stateView.data ?: emptyList())
                    updateFilteredList()
                }
                is StateView.Error -> {
                    progressBarMenu?.isVisible = false
                    showBottomSheet(message = stateView.message ?: getString(R.string.error_generic))
                }
            }
        }
    }

    private fun updateFilteredList() {
        val chipGroup = binding.root.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chip_group_categories)
        val selectedChipId = chipGroup?.checkedChipId

        when (selectedChipId) {
            R.id.chip_burgers -> typeCategory = "Hambúrgueres"
            R.id.chip_portions -> typeCategory = "Porções"
            R.id.chip_drinks -> typeCategory = "Bebidas"
            R.id.chip_combos -> typeCategory = "Combos"
        }

        val filteredList = fullMenuList.filter { menu ->
            menu.category.equals(typeCategory, ignoreCase = true)
        }
        createOrderAdapter.submitList(filteredList)
    }

    private fun saveOrderItemList(orderItemList: List<OrderItem>) {
        viewOrderViewModel.saveOrderItemList(orderItemList).observe(viewLifecycleOwner) { stateView ->
            when (stateView) {
                is StateView.Loading -> {}
                is StateView.Success -> saveOrderPrint(orderItemList)
                is StateView.Error -> showBottomSheet(message = stateView.message ?: getString(R.string.error_generic))
            }
        }
    }

    private fun saveOrderPrint(orderPrintList: List<OrderItem>) {
        viewOrderViewModel.saveOrderPrintList(orderPrintList).observe(viewLifecycleOwner) { stateView ->
            when (stateView) {
                is StateView.Loading -> {}
                is StateView.Success -> {
                    Toast.makeText(requireContext(), getString(R.string.txt_message_order_send_success), Toast.LENGTH_SHORT).show()
                    selectedTable?.let { sharedViewModel.setTableStatus(it.id, TableStatus.OPEN) }
                    
                    // Reset Tablet UI
                    binding.root.findViewById<View>(R.id.container_menu_tablet)?.isVisible = false
                    binding.root.findViewById<View>(R.id.container_order_tablet)?.isVisible = false
                    selectedTable = null
                }
                is StateView.Error -> showBottomSheet(message = stateView.message ?: getString(R.string.error_generic))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroyView() {
        handler.removeCallbacks(refreshRunnable)
        if (isTablet && selectedTable != null) {
            sharedViewModel.setTableStatus(selectedTable!!.id, TableStatus.OPEN)
        }
        super.onDestroyView()
        _binding = null
    }

}