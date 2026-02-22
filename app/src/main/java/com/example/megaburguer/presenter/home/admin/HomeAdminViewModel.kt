package com.example.megaburguer.presenter.home.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.megaburguer.data.model.User
import com.example.megaburguer.domain.users.GetUserUseCase
import com.example.megaburguer.domain.users.GetUsersUseCase
import com.example.megaburguer.util.StateView
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers

@HiltViewModel
class HomeAdminViewModel @Inject constructor(
    private val getUserUseCase: GetUserUseCase,
    private val getUsersUseCase: GetUsersUseCase,
) : ViewModel() {

    fun getUser(userId: String) = liveData(Dispatchers.IO) {
        emit(StateView.Loading())

        try {
            //val users = getUserUseCase.invoke(userId)
            val users = getUserUseCase.getUserAdmin(userId)
            emit(StateView.Success(users))

        } catch (ex: Exception) {
            emit(StateView.Error(ex.message.toString()))
        }

    }

    fun getUsers() = liveData(Dispatchers.IO) {
        emit(StateView.Loading())
        try {
            val users = getUsersUseCase.invoke()
            emit(StateView.Success(users))
        } catch (ex: Exception) {
            emit(StateView.Error(ex.message.toString()))
        }
    }

}