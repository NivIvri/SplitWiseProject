package com.example.splitwise_project.feature.groups.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.splitwise_project.R
import com.example.splitwise_project.databinding.ItemSummaryBreakdownBinding
import java.util.Locale
import kotlin.math.abs

/** Adapter for "You owe X / X owes you" summary rows. */
class SummaryBreakdownAdapter : ListAdapter<SummaryBreakdownAdapter.Row, SummaryBreakdownAdapter.VH>(DiffCb()) {

    data class Row(
        val otherUid: String,
        val otherName: String,
        val netCentsFromMe: Long // >0 means other owes me, <0 means I owe other
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemSummaryBreakdownBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemSummaryBreakdownBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: Row) {
            val amount = String.format(Locale.getDefault(), "\u20aa%.0f", abs(item.netCentsFromMe) / 100.0)
            if (item.netCentsFromMe > 0L) {
                b.tvSummaryLine.text = "${item.otherName} owes you $amount"
                b.tvSummaryLine.setTextColor(ContextCompat.getColor(b.root.context, R.color.amount_lent))
            } else {
                b.tvSummaryLine.text = "You owe ${item.otherName} $amount"
                b.tvSummaryLine.setTextColor(ContextCompat.getColor(b.root.context, R.color.amount_borrowed))
            }
        }
    }

    private class DiffCb : DiffUtil.ItemCallback<Row>() {
        override fun areItemsTheSame(oldItem: Row, newItem: Row): Boolean = oldItem.otherUid == newItem.otherUid
        override fun areContentsTheSame(oldItem: Row, newItem: Row): Boolean = oldItem == newItem
    }
}
