package com.example.megaburguer.presenter.home.admin.choose_user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.megaburguer.data.model.User
import com.example.megaburguer.databinding.ItemViewUsersBinding

class ChooseUserAdapter : ListAdapter<User, ChooseUserAdapter.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemViewUsersBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

    inner class ViewHolder(
        private val binding: ItemViewUsersBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            // Using textChangePasswordTitle as it corresponds to the user name in the XML
            binding.textChangePasswordTitle.text = user.name
            // Added textUserType to the XML for binding user type
            binding.textUserType.text = user.typeUser

            binding.root.setOnClickListener {
                itemClickListener?.invoke(user)
            }
        }
    }

    private var itemClickListener: ((User) -> Unit)? = null

    fun setOnClickListener(listener: (User) -> Unit) {
        itemClickListener = listener
    }
}