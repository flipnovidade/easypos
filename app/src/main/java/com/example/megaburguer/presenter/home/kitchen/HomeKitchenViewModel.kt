package com.example.megaburguer.presenter.home.kitchen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.megaburguer.data.model.OrderItem
import com.example.megaburguer.domain.orderPrint.DeleteOrderPrintUseCase
import com.example.megaburguer.domain.orderPrint.ObserveOrderPrintUseCase
import com.example.megaburguer.util.StateView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DisplayMode {
    ITEM_BY_ITEM,
    BY_CATEGORY
}

@HiltViewModel
class HomeKitchenViewModel @Inject constructor(
    private val observeOrderPrintUseCase: ObserveOrderPrintUseCase,
    private val deleteOrderPrintUseCase: DeleteOrderPrintUseCase
) : ViewModel() {

    private val _displayMode = kotlinx.coroutines.flow.MutableStateFlow(DisplayMode.ITEM_BY_ITEM)
    val displayMode: kotlinx.coroutines.flow.StateFlow<DisplayMode> = _displayMode

    fun setDisplayMode(mode: DisplayMode) {
        _displayMode.value = mode
    }

    fun observeOrderPrint() = liveData(Dispatchers.IO) {
        try {
            kotlinx.coroutines.flow.combine(
                observeOrderPrintUseCase(),
                _displayMode
            ) { items, mode ->
                val nonPrintedItems = items.filter { !it.printed }
                
                if (mode == DisplayMode.BY_CATEGORY) {
                    aggregateItems(nonPrintedItems)
                } else {
                    nonPrintedItems
                }
            }.collect { processedItems ->
                emit(StateView.Success(processedItems))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(StateView.Error(message = e.message))
        }
    }

    private fun aggregateItems(items: List<OrderItem>): List<OrderItem> {
        val groupedMap = items.groupBy { Triple(it.nameTable, it.date, getCategoryGroup(it.category)) }
        
        return groupedMap.map { (key, groupItems) ->
            val (tableName, date, categoryGroup) = key
            
            // Agrupar itens por nome dentro da categoria para somar quantidades
            val itemQuantitySummary = groupItems.groupBy { it.nameItem }
                .map { (itemName, itemsWithName) ->
                    val totalQtd = itemsWithName.sumOf { it.quantity }
                    "${totalQtd}x $itemName"
                }.joinToString("\n")
 
            val observations = groupItems.filter { it.observation.isNotEmpty() }
                .joinToString("\n") { "${it.nameItem}: ${it.observation}" }
 
            OrderItem(
                id = groupItems.joinToString(",") { it.id }, // ID para o DIFF (hacky)
                nameTable = tableName,
                nameItem = itemQuantitySummary,
                category = categoryGroup, // Usamos o grupo para a cor
                observation = observations,
                quantity = groupItems.sumOf { it.quantity },
                nameWaiter = groupItems.map { it.nameWaiter }.distinct().joinToString(", "),
                groupedIds = groupItems.map { it.id },
                date = date
            )
        }.sortedWith(
            compareBy<OrderItem> { it.date }
                .thenBy { it.nameTable }
                .thenBy { getCategorySortOrder(it.category) }
        )
    }

    private fun getCategoryGroup(category: String): String {
        return when (category) {
            "Hambúrgueres", "Porções", "Combos" -> "Hambúrgueres" // Agrupamos como Food
            else -> "Bebidas"
        }
    }

    private fun getCategorySortOrder(category: String): Int {
        return when (category) {
            "Hambúrgueres", "Porções", "Combos" -> 1 // Food
            "Bebidas" -> 2 // Drinks
            else -> 3
        }
    }

    fun markAsPrinted(itemIds: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                deleteOrderPrintUseCase(itemIds)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}