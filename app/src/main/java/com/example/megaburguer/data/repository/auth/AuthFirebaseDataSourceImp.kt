package com.example.megaburguer.data.repository.auth

import com.example.megaburguer.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import jakarta.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AuthFirebaseDataSourceImp @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseDatabase: FirebaseDatabase
): AuthFirebaseDataSource {

    override suspend fun login(email: String, password: String) {
        return suspendCoroutine { continuation ->
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->

                    if (task.isSuccessful) {
                        continuation.resumeWith(Result.success(Unit))

                    } else {
                        continuation.resumeWith(Result.failure(task.exception!!))
                    }
                }
        }
    }

    override suspend fun register(name: String, email: String, password: String, typeUser: String): User {
        val adminId = firebaseAuth.currentUser?.uid ?: throw Exception("Admin not verified")

        return suspendCoroutine { continuation ->
            // Generate a unique ID using push key
            val newUserId = firebaseDatabase.reference
                .child("users")
                .child(adminId)
                .child("funcionarios").push().key

            if (newUserId != null) {
                val user = User(id = newUserId, name = name, email = email, typeUser = typeUser)

                firebaseDatabase.reference
                    .child("users")
                    .child(adminId)
                    .child("funcionarios")
                    .child(newUserId)
                    .setValue(user)
                    .addOnCompleteListener { taskSave ->
                        if (taskSave.isSuccessful) {
                            continuation.resume(Result.success(user).getOrThrow())
                        } else {
                            continuation.resumeWith(Result.failure(taskSave.exception ?: Exception("Error saving user data")))
                        }
                    }


            } else {
                continuation.resumeWith(Result.failure(Exception("Failed to generate user ID")))
            }
        }
    }

    override suspend fun recover(email: String) {
        return suspendCoroutine { continuation ->
            firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resumeWith(Result.success(Unit))
                    } else {
                        continuation.resumeWith(Result.failure(task.exception!!))
                    }
                }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val backendUrl = "https://backend-mega-burguer.onrender.com/deleteUser"

    override suspend fun deleteUser(userId: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val json = JSONObject().apply { put("uid", userId) }
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(backendUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) {
                    if (response.isSuccessful) {
                        continuation.resume(Result.success(Unit))
                    } else {
                        val errorMsg = response.body?.string() ?: "Erro ao deletar usuário"
                        continuation.resume(Result.failure(Exception(errorMsg)))
                    }
                }
            }
        })
    }
}