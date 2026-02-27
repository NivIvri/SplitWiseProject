package com.example.splitwise_project.feature.groups.expense_details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.splitwise_project.databinding.ItemSplitDetailBinding
import java.util.Locale

/** Adapter for split participants and their individual amounts. */
class SplitDetailsAdapter : ListAdapter<Pair<String, Long>, SplitDetailsAdapter.VH>(DiffCb()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSplitDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemSplitDetailBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: Pair<String, Long>) {
            b.tvParticipant.text = item.first
            b.tvAmount.text = String.format(Locale.getDefault(), "%.2f ILS", item.second / 100.0)
        }
    }

    private class DiffCb : DiffUtil.ItemCallback<Pair<String, Long>>() {
        override fun areItemsTheSame(oldItem: Pair<String, Long>, newItem: Pair<String, Long>): Boolean {
            return oldItem.first == newItem.first
        }

        override fun areContentsTheSame(oldItem: Pair<String, Long>, newItem: Pair<String, Long>): Boolean {
            return oldItem == newItem
        }
    }
}
