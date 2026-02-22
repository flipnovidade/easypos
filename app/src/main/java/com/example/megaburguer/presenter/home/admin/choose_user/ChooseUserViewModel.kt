package com.example.megaburguer.presenter.home.admin.choose_user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.megaburguer.data.model.User
import com.example.megaburguer.domain.users.GetUsersUseCase
import com.example.megaburguer.util.SharedPreferencesHelper
import com.example.megaburguer.util.StateView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class ChooseUserViewModel @Inject constructor(
    private val getUsersUseCase: GetUsersUseCase,
    private val sharedPreferencesHelper: SharedPreferencesHelper
) : ViewModel() {

    fun getUsers() = liveData(Dispatchers.IO) {
        try {
            emit(StateView.Loading())
            val users = getUsersUseCase()
            emit(StateView.Success(users))
        } catch (e: Exception) {
            e.printStackTrace()
            emit(StateView.Error(message = e.message))
        }
    }

    fun saveUser(user: User) {
        sharedPreferencesHelper.saveUser(user.id, user.typeUser, user.name)
    }
}