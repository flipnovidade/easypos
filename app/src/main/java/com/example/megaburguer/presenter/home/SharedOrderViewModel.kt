package com.example.megaburguer.presenter.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.megaburguer.data.enum.TableStatus
import com.example.megaburguer.di.FirebaseModule_ProvidesFirebaseAuthFactory
import com.example.megaburguer.util.FirebaseHelper
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

    fun setTableStatus(tableId: String, status: TableStatus) {
        _tableStatusEvent.value = Pair(tableId, status)
    }

    fun consumeEvent() {
        _tableStatusEvent.value = null
    }

    fun logoutApp(){
        sharedPreferencesHelper.clearUser()
        sharedPreferencesHelper.clearSavedCredentials()
        firebaseAuth.signOut()
    }
}