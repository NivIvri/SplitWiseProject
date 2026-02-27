package com.example.splitwise_project.feature.groups.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.splitwise_project.data.model.Settlement
import com.example.splitwise_project.databinding.ItemSettlementBinding
import java.util.Locale

/** RecyclerView adapter for optimized "who owes whom" rows. */
class SettlementAdapter : ListAdapter<Settlement, SettlementAdapter.VH>(DiffCb()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSettlementBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemSettlementBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: Settlement) {
            b.tvSettlementFrom.text = "From: ${item.fromUid.take(8)}…"
            b.tvSettlementTo.text = "To: ${item.toUid.take(8)}…"
            b.tvSettlementAmount.text = String.format(Locale.getDefault(), "%.2f ILS", item.amountCents / 100.0)
        }
    }

    private class DiffCb : DiffUtil.ItemCallback<Settlement>() {
        override fun areItemsTheSame(oldItem: Settlement, newItem: Settlement): Boolean {
            return oldItem.fromUid == newItem.fromUid &&
                oldItem.toUid == newItem.toUid &&
                oldItem.amountCents == newItem.amountCents
        }

        override fun areContentsTheSame(oldItem: Settlement, newItem: Settlement): Boolean {
            return oldItem == newItem
        }
    }
}
