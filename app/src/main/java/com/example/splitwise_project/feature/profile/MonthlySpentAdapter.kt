package com.example.splitwise_project.feature.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.splitwise_project.data.model.MonthlySpent
import com.example.splitwise_project.databinding.ItemMonthlySpentBinding
import java.util.Locale

/** Adapter for monthly spent rows. */
class MonthlySpentAdapter : ListAdapter<MonthlySpent, MonthlySpentAdapter.VH>(DiffCb()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMonthlySpentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemMonthlySpentBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: MonthlySpent) {
            b.tvMonth.text = item.month
            b.tvSpent.text = String.format(Locale.getDefault(), "%.2f ILS", item.amountCents / 100.0)
        }
    }

    private class DiffCb : DiffUtil.ItemCallback<MonthlySpent>() {
        override fun areItemsTheSame(oldItem: MonthlySpent, newItem: MonthlySpent): Boolean {
            return oldItem.month == newItem.month
        }

        override fun areContentsTheSame(oldItem: MonthlySpent, newItem: MonthlySpent): Boolean {
            return oldItem == newItem
        }
    }
}
