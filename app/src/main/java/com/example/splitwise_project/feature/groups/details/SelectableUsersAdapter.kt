package com.example.splitwise_project.feature.groups.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.splitwise_project.data.model.User
import com.example.splitwise_project.databinding.ItemSelectableUserBinding

/** Adapter for selectable users list in add-members sheet. */
class SelectableUsersAdapter(
    private val onSelectionChanged: (uid: String, selected: Boolean) -> Unit
) : ListAdapter<User, SelectableUsersAdapter.VH>(DiffCb()) {

    private val selectedUids = mutableSetOf<String>()
    private val existingUids = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSelectableUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    fun setSelectedUids(values: Set<String>) {
        selectedUids.clear()
        selectedUids.addAll(values)
        notifyDataSetChanged()
    }

    fun setExistingUids(values: Set<String>) {
        existingUids.clear()
        existingUids.addAll(values)
        notifyDataSetChanged()
    }

    inner class VH(private val b: ItemSelectableUserBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(user: User) {
            b.tvName.text = user.username.ifBlank { user.displayName.ifBlank { "Unknown user" } }
            b.tvEmail.text = user.email
            val isExisting = existingUids.contains(user.uid)
            b.cbSelect.setOnCheckedChangeListener(null)
            b.cbSelect.isChecked = isExisting || selectedUids.contains(user.uid)
            b.cbSelect.isEnabled = !isExisting
            b.cbSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedUids.add(user.uid) else selectedUids.remove(user.uid)
                onSelectionChanged(user.uid, isChecked)
            }
            b.root.setOnClickListener {
                if (!isExisting) b.cbSelect.toggle()
            }
        }
    }

    private class DiffCb : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
    }
}
