package com.example.megaburguer.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OrderItem(
    val id: String = "",
    val idItem: String = "",
    val idTable: String = "",
    val nameTable: String = "",
    val nameWaiter: String = "",
    val nameItem: String = "",
    val price: Float = 0f,
    var quantity: Int = 0,
    var observation: String = "",
    var printed: Boolean = false,
    val category: String = "",
    val groupedIds: List<String> = emptyList(),
    val date: Long = 0
): Parcelable
