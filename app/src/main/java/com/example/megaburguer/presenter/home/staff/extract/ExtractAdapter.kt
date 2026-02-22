package com.example.megaburguer.presenter.home.staff.extract

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.megaburguer.R
import com.example.megaburguer.data.model.OrderItem
import com.example.megaburguer.databinding.ItemExtractHeaderBinding
import com.example.megaburguer.databinding.ItemExtractLineBinding
import com.example.megaburguer.util.GetMask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ExtractItem {
    data class Header(val tableName: String, val date: Long) : ExtractItem()
    data class Item(val orderItem: OrderItem) : ExtractItem()
}

class ExtractAdapter : ListAdapter<ExtractItem, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ExtractItem>() {
            override fun areItemsTheSame(oldItem: ExtractItem, newItem: ExtractItem): Boolean {
                return when {
                    oldItem is ExtractItem.Header && newItem is ExtractItem.Header -> 
                        oldItem.tableName == newItem.tableName && oldItem.date == newItem.date
                    oldItem is ExtractItem.Item && newItem is ExtractItem.Item -> 
                        oldItem.orderItem.id == newItem.orderItem.id
                    else -> false
                }
            }

            override fun areContentsTheSame(oldItem: ExtractItem, newItem: ExtractItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ExtractItem.Header -> TYPE_HEADER
            is ExtractItem.Item -> TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(ItemExtractHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            ItemViewHolder(ItemExtractLineBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is HeaderViewHolder && item is ExtractItem.Header) {
            holder.bind(item)
        } else if (holder is ItemViewHolder && item is ExtractItem.Item) {
            holder.bind(item)
        }
    }

    inner class HeaderViewHolder(private val binding: ItemExtractHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: ExtractItem.Header) {
            val ptBr = Locale.forLanguageTag("pt-BR")
            val time = SimpleDateFormat("HH:mm", ptBr).format(Date(header.date))
            val tableText = binding.root.context.getString(R.string.txt_title_table, header.tableName)
            binding.txtHeaderTable.text = "$tableText - $time"
        }
    }

    inner class ItemViewHolder(private val binding: ItemExtractLineBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ExtractItem.Item) {
            val orderItem = item.orderItem
            binding.txtQtyAndName.text = binding.root.context.getString(
                R.string.txt_quantity_item_extract_line,
                orderItem.quantity.toString(), orderItem.nameItem
            )

            binding.txtUnitPrice.text = binding.root.context.getString(
                R.string.txt_value_each_extract_line,
                GetMask.getFormatedValue(orderItem.price)
            )

            binding.txtLineTotal.text = binding.root.context.getString(
                R.string.txt_value_sub_total_extract_line,
                GetMask.getFormatedValue(orderItem.price * orderItem.quantity)
            )
        }
    }
}