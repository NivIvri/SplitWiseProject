package com.example.splitwise_project.feature.groups.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.splitwise_project.data.model.MemberBalance
import com.example.splitwise_project.databinding.ItemMemberBalanceBinding
import java.util.Locale

/** Adapter for per-member balance summary rows. */
class MemberBalanceAdapter : ListAdapter<MemberBalance, MemberBalanceAdapter.VH>(DiffCb()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemMemberBalanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemMemberBalanceBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: MemberBalance) {
            b.tvName.text = item.name.ifBlank { item.name }
            val amount = String.format(Locale.getDefault(), "%.2f ILS", kotlin.math.abs(item.balanceCents) / 100.0)
            b.tvBalance.text = when {
                item.balanceCents > 0L -> "Gets $amount"
                item.balanceCents < 0L -> "Owes $amount"
                else -> "Settled"
            }
        }
    }

    private class DiffCb : DiffUtil.ItemCallback<MemberBalance>() {
        override fun areItemsTheSame(oldItem: MemberBalance, newItem: MemberBalance): Boolean = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: MemberBalance, newItem: MemberBalance): Boolean = oldItem == newItem
    }
}
