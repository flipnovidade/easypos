package com.example.megaburguer.presenter.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.megaburguer.data.enum.TableStatus
import com.example.megaburguer.data.model.OrderItem
import com.example.megaburguer.util.SharedPreferencesHelper
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SharedOrderViewModel @Inject constructor(
    private val sharedPreferencesHelper: SharedPreferencesHelper,
    private val firebaseAuth: FirebaseAuth
): ViewModel() {
    private val _tableStatusEvent = MutableLiveData<Pair<String, TableStatus>?>()
    val tableStatusEvent: LiveData<Pair<String, TableStatus>?> get() = _tableStatusEvent

    private val _currentOrderItems = MutableLiveData<List<OrderItem>>(emptyList())
    val currentOrderItems: LiveData<List<OrderItem>> get() = _currentOrderItems

    private val _itemQuantityMap = MutableLiveData<Map<String, Int>>(emptyMap())
    val itemQuantityMap: LiveData<Map<String, Int>> get() = _itemQuantityMap

    fun setTableStatus(tableId: String, status: TableStatus) {
        _tableStatusEvent.value = Pair(tableId, status)
    }

    fun consumeEvent() {
        _tableStatusEvent.value = null
    }

    fun addItem(menu: com.example.megaburguer.data.model.Menu, tableId: String, tableNumber: String, waiterName: String) {
        val currentItems = _currentOrderItems.value?.toMutableList() ?: mutableListOf()
        val currentMap = _itemQuantityMap.value?.toMutableMap() ?: mutableMapOf()

        val existingItemIndex = currentItems.indexOfFirst { it.idItem == menu.id }
        val newQuantity = (currentMap[menu.id] ?: 0) + 1
        currentMap[menu.id] = newQuantity

        if (existingItemIndex != -1) {
            currentItems[existingItemIndex] = currentItems[existingItemIndex].copy(quantity = newQuantity)
        } else {
            val orderItem = OrderItem(
                id = com.google.firebase.database.FirebaseDatabase.getInstance().reference.push().key ?: "",
                idItem = menu.id,
                idTable = tableId,
                nameTable = tableNumber,
                nameWaiter = waiterName,
                nameItem = menu.nameItem,
                price = menu.price,
                quantity = 1,
                observation = "",
                category = menu.category,
                date = System.currentTimeMillis()
            )
            currentItems.add(orderItem)
        }

        _currentOrderItems.value = currentItems
        _itemQuantityMap.value = currentMap
    }

    fun updateQuantity(idItem: String, delta: Int) {
        val currentItems = _currentOrderItems.value?.toMutableList() ?: return
        val currentMap = _itemQuantityMap.value?.toMutableMap() ?: return

        val index = currentItems.indexOfFirst { it.idItem == idItem }
        if (index != -1) {
            val oldQuantity = currentMap[idItem] ?: currentItems[index].quantity
            val newQuantity = oldQuantity + delta
            if (newQuantity > 0) {
                currentMap[idItem] = newQuantity
                currentItems[index] = currentItems[index].copy(quantity = newQuantity)
                _currentOrderItems.value = currentItems
                _itemQuantityMap.value = currentMap
            } else if (newQuantity == 0) {
                removeItem(idItem)
            }
        }
    }

    fun removeItem(idItem: String) {
        val currentItems = _currentOrderItems.value?.toMutableList() ?: return
        val currentMap = _itemQuantityMap.value?.toMutableMap() ?: return

        currentItems.removeAll { it.idItem == idItem }
        currentMap.remove(idItem)

        _currentOrderItems.value = currentItems
        _itemQuantityMap.value = currentMap
    }

    fun updateObservation(idItem: String, observation: String) {
        val currentItems = _currentOrderItems.value?.toMutableList() ?: return
        val index = currentItems.indexOfFirst { it.idItem == idItem }
        if (index != -1) {
            currentItems[index] = currentItems[index].copy(observation = observation)
            _currentOrderItems.value = currentItems
        }
    }

    fun clearOrder() {
        _currentOrderItems.value = emptyList()
        _itemQuantityMap.value = emptyMap()
    }

    fun logoutApp(){
        sharedPreferencesHelper.clearUser()
        sharedPreferencesHelper.clearSavedCredentials()
        firebaseAuth.signOut()
    }
}