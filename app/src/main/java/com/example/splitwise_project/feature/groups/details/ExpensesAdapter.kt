package com.example.splitwise_project.feature.groups.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.splitwise_project.R
import com.example.splitwise_project.data.model.Expense
import com.example.splitwise_project.databinding.ItemExpenseBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** ListAdapter for displaying expenses using DiffUtil. */
class ExpensesAdapter(
    private val onClick: (Expense) -> Unit
) : ListAdapter<Expense, ExpensesAdapter.VH>(DiffCb()) {

    private var uidToName: Map<String, String> = emptyMap()
    private var currentUid: String? = null
    private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("dd", Locale.getDefault())

    fun setUidToName(map: Map<String, String>) {
        uidToName = map
        notifyDataSetChanged()
    }

    fun setCurrentUid(uid: String?) {
        currentUid = uid
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemExpenseBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(e: Expense) {
            b.tvExpenseDescription.text = e.description

            val date = Date(e.createdAt)
            b.tvExpenseMonth.text = monthFormat.format(date).uppercase(Locale.getDefault())
            b.tvExpenseDay.text = dayFormat.format(date)

            val me = currentUid
            val paidByName = uidToName[e.paidByUid] ?: "Unknown user"
            val myShare = if (me.isNullOrBlank()) 0L else (e.splits[me] ?: 0L)

            // Left/middle subtitle
            if (!me.isNullOrBlank() && e.paidByUid == me) {
                val paidText = String.format(
                    Locale.getDefault(),
                    "%.2f %s",
                    e.amountCents / 100.0,
                    e.currency
                )
                b.tvExpensePaidBy.text = "You paid $paidText"
            } else {
                b.tvExpensePaidBy.text = "Paid by $paidByName"
            }

            // Net for current user:
            // >0 means you lent (others owe you), <0 means you borrowed (you owe others)
            val net = if (!me.isNullOrBlank() && e.paidByUid == me) {
                e.amountCents - myShare
            } else {
                -myShare
            }

            when {
                net > 0L -> {
                    b.tvExpenseDirection.text = "you lent"
                    b.tvExpenseDirection.setTextColor(
                        ContextCompat.getColor(b.root.context, R.color.amount_lent)
                    )
                    b.tvExpenseAmount.setTextColor(
                        ContextCompat.getColor(b.root.context, R.color.amount_lent)
                    )
                }

                net < 0L -> {
                    b.tvExpenseDirection.text = "you borrowed"
                    b.tvExpenseDirection.setTextColor(
                        ContextCompat.getColor(b.root.context, R.color.amount_borrowed)
                    )
                    b.tvExpenseAmount.setTextColor(
                        ContextCompat.getColor(b.root.context, R.color.amount_borrowed)
                    )
                }

                else -> {
                    b.tvExpenseDirection.text = "SETTLED"
                    b.tvExpenseDirection.setTextColor(
                        ContextCompat.getColor(b.root.context, R.color.text_secondary)
                    )
                    b.tvExpenseAmount.setTextColor(
                        ContextCompat.getColor(b.root.context, R.color.text_primary)
                    )
                }
            }

            // Show the net amount (abs) on the right, like the design
            val shownAmountCents = kotlin.math.abs(net)
            b.tvExpenseAmount.text = String.format(
                Locale.getDefault(),
                "%.2f %s",
                shownAmountCents / 100.0,
                e.currency
            )

            b.root.setOnClickListener { onClick(e) }
        }
    }
    private class DiffCb : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(a: Expense, b: Expense) = a.id == b.id
        override fun areContentsTheSame(a: Expense, b: Expense) = a == b
    }
}
