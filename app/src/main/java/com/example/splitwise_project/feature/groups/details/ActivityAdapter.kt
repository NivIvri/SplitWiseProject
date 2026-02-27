package com.example.splitwise_project.feature.groups.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.splitwise_project.data.model.ActivityItem
import com.example.splitwise_project.databinding.ItemActivityBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** RecyclerView adapter for group activity timeline rows. */
class ActivityAdapter : ListAdapter<ActivityItem, ActivityAdapter.VH>(DiffCb()) {

    private val formatter = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    private var uidToUsername: Map<String, String> = emptyMap()

    /** Provide resolved user labels so the UI never shows raw UIDs. */
    fun setUidToUsername(map: Map<String, String>) {
        uidToUsername = map
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val b: ItemActivityBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ActivityItem) {
            val actor = resolveName(item.actorUid)
            val target = resolveName(item.targetUid)
            b.tvActivityTitle.text = when (item.type) {
                "group_created" -> "Group created"
                "member_added" -> "Member added"
                "expense_added" -> "Expense added"
                else -> item.type.ifBlank { "Activity" }
            }
            b.tvActivitySubtitle.text = when (item.type) {
                "member_added" -> {
                    if (actor == "Unknown user") "Added member: $target"
                    else "$actor added $target"
                }
                "expense_added" -> {
                    val amount = String.format(Locale.getDefault(), "%.2f ILS", item.amountCents / 100.0)
                    "${item.description} • $amount • by $actor"
                }
                else -> "By: $actor"
            }
            b.tvActivityTime.text = formatter.format(Date(item.createdAt))
        }
    }

    private fun resolveName(uid: String): String {
        if (uid.isBlank()) return "Unknown user"
        return uidToUsername[uid] ?: "Unknown user"
    }

    private class DiffCb : DiffUtil.ItemCallback<ActivityItem>() {
        override fun areItemsTheSame(oldItem: ActivityItem, newItem: ActivityItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ActivityItem, newItem: ActivityItem): Boolean {
            return oldItem == newItem
        }
    }
}
