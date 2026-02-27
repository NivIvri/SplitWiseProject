package com.example.splitwise_project.feature.groups.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.splitwise_project.R
import com.example.splitwise_project.databinding.ItemExpenseCategoryBinding

/** Grid adapter used by the category bottom sheet picker. */
class ExpenseCategoryGridAdapter(
    private val onClick: (ExpenseCategory.Option) -> Unit
) : RecyclerView.Adapter<ExpenseCategoryGridAdapter.VH>() {

    private var items: List<ExpenseCategory.Option> = emptyList()
    private var selectedKey: String = ExpenseCategory.OTHER

    fun submitList(list: List<ExpenseCategory.Option>, currentSelectedKey: String) {
        items = list
        selectedKey = currentSelectedKey
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemExpenseCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    inner class VH(private val b: ItemExpenseCategoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ExpenseCategory.Option) {
            val isSelected = item.key == selectedKey
            b.ivCategoryIcon.setImageResource(item.iconRes)
            b.tvCategoryName.text = item.label
            b.tvSelected.visibility = if (isSelected) android.view.View.VISIBLE else android.view.View.GONE
            b.cardCategoryOption.strokeColor = ContextCompat.getColor(
                b.root.context,
                if (isSelected) R.color.app_primary else R.color.divider
            )
            b.cardCategoryOption.setCardBackgroundColor(
                ContextCompat.getColor(
                    b.root.context,
                    if (isSelected) R.color.green_soft else android.R.color.white
                )
            )
            b.root.setOnClickListener { onClick(item) }
        }
    }
}
