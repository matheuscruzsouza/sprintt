package io.github.matheuscruzsouza.sprintt.ui.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.matheuscruzsouza.sprintt.data.model.User
import io.github.matheuscruzsouza.sprintt.databinding.ItemUserCardBinding

class UserAdapter(
    private val onUserClick: (User) -> Unit = {}
) : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemUserCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            val initial = if (user.fullName.isNotBlank()) user.fullName.take(1).uppercase() else user.username.take(1).uppercase()
            binding.tvUserAvatar.text = initial

            try {
                val bg = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(user.avatarColor))
                }
                binding.tvUserAvatar.background = bg
            } catch (e: Exception) {
                // fallback para estilo padrao
            }

            binding.tvUserFullName.text = user.fullName
            binding.tvUserHandle.text = "@${user.username} • ${user.email}"
            binding.tvUserRole.text = user.role

            binding.root.setOnClickListener { onUserClick(user) }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean =
            oldItem == newItem
    }
}
