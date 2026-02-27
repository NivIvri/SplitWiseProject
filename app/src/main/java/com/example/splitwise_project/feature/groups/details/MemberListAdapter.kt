package com.example.splitwise_project.feature.groups.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.splitwise_project.data.model.User
import com.example.splitwise_project.databinding.ItemMemberBinding

/** Adapter for showing current group members. */
class MemberListAdapter : ListAdapter<User, MemberListAdapter.VH>(DiffCb()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemMemberBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(user: User) {
            b.tvMemberName.text = user.username.ifBlank { user.displayName.ifBlank { "Unknown user" } }
            b.tvMemberEmail.text = user.email
        }
    }

    private class DiffCb : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
    }
}
