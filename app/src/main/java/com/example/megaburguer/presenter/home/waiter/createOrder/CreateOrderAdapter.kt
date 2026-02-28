package com.example.megaburguer.presenter.home.waiter.createOrder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.megaburguer.R
import com.example.megaburguer.data.model.Menu
import com.example.megaburguer.data.model.Table
import com.example.megaburguer.databinding.ItemCreateOrdersBinding
import com.example.megaburguer.util.GetMask

class CreateOrderAdapter(
    private val onAddItemClick: (menu: Menu, position: Int) -> Unit,
    private var quantityMap: Map<String, Int>,
    private val onAddObservationClick: (menu: Menu) -> Unit,
    private val onMoreClick: (menu: Menu, position: Int) -> Unit,
    private val onLessClick: (menu: Menu, position: Int) -> Unit
) : ListAdapter<Menu, CreateOrderAdapter.MyViewHolder>(DIFF_CALLBACK) {

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Menu>() {
            override fun areItemsTheSame(oldItem: Menu, newItem: Menu): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Menu, newItem: Menu): Boolean {
                return oldItem == newItem
            }
        }
    }

    // Cria o "molde" (ViewHolder) para cada item da lista
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            ItemCreateOrdersBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    // Conecta os dados de uma mesa específica à sua representação visual
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val menuItem = getItem(position)
        holder.bind(menuItem)
    }

    // A classe interna que representa o "molde" de cada item
    inner class MyViewHolder(val binding: ItemCreateOrdersBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(menu: Menu) {

            binding.nameItem.text = menu.nameItem
            binding.txtNumber.text = binding.root.context.
            getString(R.string.txt_price_snack_manage_menu,
                GetMask.getFormatedValue(menu.price))

            val qtd = quantityMap[menu.id] ?: 0
            if (qtd > 0) {
                binding.btnAddItem.isVisible = false
                binding.containerQuantity.isVisible = true
                binding.txtQuantityItem.text = qtd.toString()
            } else {
                binding.btnAddItem.isVisible = true
                binding.containerQuantity.isVisible = false
                binding.txtQuantityItem.text = ""
            }

            binding.btnAddItem.setOnClickListener {
                onAddItemClick(menu, adapterPosition)
            }

            binding.btnMoreMenu.setOnClickListener {
                onMoreClick(menu, adapterPosition)
            }

            binding.btnLessMenu.setOnClickListener {
                onLessClick(menu, adapterPosition)
            }

            binding.btnObs.setOnClickListener {
                onAddObservationClick(menu)
            }

        }
    }

    fun updateQuantityMap(newMap: Map<String, Int>) {
        this.quantityMap = newMap
        notifyDataSetChanged()
    }
}