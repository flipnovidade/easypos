package com.example.megaburguer.presenter.home.kitchen

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.megaburguer.data.model.OrderItem
import com.example.megaburguer.databinding.ItemOrderKitchenBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeKitchenAdapter(
    private val onMarkAsPrintedClick: (orderItem: OrderItem) -> Unit,
    private val onPrintItemClick: (orderItem: OrderItem) -> Unit
) : ListAdapter<OrderItem, HomeKitchenAdapter.ViewHolder>(DIFF_CALLBACK) {

    private val expandedPositions = mutableSetOf<String>()

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<OrderItem>() {
            override fun areItemsTheSame(oldItem: OrderItem, newItem: OrderItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: OrderItem, newItem: OrderItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemOrderKitchenBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(private val binding: ItemOrderKitchenBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OrderItem) {
            binding.textTableName.text = "Mesa: ${item.nameTable}"
            binding.textItemName.text = item.nameItem
            binding.textQuantity.text = "x${item.quantity}"
            binding.textWaiterName.text = item.nameWaiter
            binding.textObservation.text = item.observation.ifEmpty { "Nenhuma" }

            val ptBr = Locale.forLanguageTag("pt-BR")
            val time = SimpleDateFormat("HH:mm", ptBr).format(Date(item.date))
            binding.textOrderTime.text = time

            val color = when (item.category) {
                "Hambúrgueres", "Porções", "Combos" -> com.example.megaburguer.R.color.mega_burger_red
                "Bebidas" -> com.example.megaburguer.R.color.mega_burger_blue
                else -> com.example.megaburguer.R.color.mega_burger_orange_strong
            }
            binding.root.setCardBackgroundColor(binding.root.context.getColor(color))
            val isExpanded = expandedPositions.contains(item.id)
            binding.groupExpanded.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.divider.visibility = if (isExpanded) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                if (isExpanded) {
                    expandedPositions.remove(item.id)
                } else {
                    expandedPositions.add(item.id)
                }
                notifyItemChanged(adapterPosition)
            }

            binding.btnPrinted.setOnClickListener {
                onMarkAsPrintedClick(item)
            }

            binding.btnPrintItem.setOnClickListener {
                onPrintItemClick(item)
            }
        }
    }
}