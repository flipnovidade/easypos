package com.example.megaburguer.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SharedPreferencesHelper(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "mega_burguer_pref"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_TYPE = "key_user_type"
    }

    fun saveUser(userId: String, userType: String, userName: String) {
        sharedPreferences.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_TYPE, userType)
            apply()
        }
    }

    fun getUserId(): String? = sharedPreferences.getString(KEY_USER_ID, null)
    fun getUserName(): String? = sharedPreferences.getString(KEY_USER_NAME, null)
    fun getUserType(): String? = sharedPreferences.getString(KEY_USER_TYPE, null)

    fun clearUser() {
        sharedPreferences.edit() { clear() }
    }

    fun saveCredentials(email: String, password: String) {
        sharedPreferences.edit().apply {
            putString("pref_email", email)
            putString("pref_password", password)
            apply()
        }
    }

    fun getSavedEmail(): String? = sharedPreferences.getString("pref_email", null)
    fun getSavedPassword(): String? = sharedPreferences.getString("pref_password", null)

    fun clearSavedCredentials() {
        sharedPreferences.edit().apply {
            remove("pref_email")
            remove("pref_password")
            apply()
        }
    }
}
