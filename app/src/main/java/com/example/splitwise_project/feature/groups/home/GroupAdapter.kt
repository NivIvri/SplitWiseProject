package com.example.splitwise_project.feature.groups.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.splitwise_project.data.model.Group
import com.example.splitwise_project.databinding.ItemGroupBinding

/** RecyclerView adapter for displaying groups using ListAdapter + DiffUtil. */
class GroupAdapter(
    private val onClick: (Group) -> Unit
) : ListAdapter<Group, GroupAdapter.GroupViewHolder>(GroupDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        // Use ViewBinding since viewBinding is enabled
        val binding = ItemGroupBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroupViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /** ViewHolder for group items; click handling is delegated to Fragment via callback. */
    inner class GroupViewHolder(
        private val binding: ItemGroupBinding,
        private val onClick: (Group) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(group: Group) {
            binding.tvGroupName.text = group.name
            binding.tvMembersCount.text = group.memberPreview.ifBlank {
                "${group.memberCount} member${if (group.memberCount == 1) "" else "s"}"
            }
            // Delegate click handling to Fragment via callback
            binding.root.setOnClickListener { onClick(group) }
        }
    }

    /** DiffUtil callback to compare Group items for efficient updates. */
    private class GroupDiffCallback : DiffUtil.ItemCallback<Group>() {
        override fun areItemsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean {
            return oldItem == newItem
        }
    }
}
