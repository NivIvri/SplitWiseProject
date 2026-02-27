package com.example.splitwise_project.data.repository

import com.example.splitwise_project.data.model.Expense
import com.example.splitwise_project.data.model.ProfileTotals
import com.example.splitwise_project.data.model.ProfileSummary
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Repository for user-level profile summary aggregation. */
class ProfileRepository {

    private val db = FirebaseDatabase.getInstance().reference
    private val userGroupsRef = db.child("userGroups")
    private val expensesRef = db.child("expenses")
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    /**
     * Observe all expenses from all groups where user is a member and recompute totals
     * in realtime whenever group membership or any expense changes.
     */
    fun observeUserGroupsAndExpenses(
        userId: String,
        onResult: (ProfileTotals) -> Unit,
        onError: (DatabaseError) -> Unit
    ): () -> Unit {
        val userGroupsNode = userGroupsRef.child(userId)
        val expenseListenersByGroup = mutableMapOf<String, ValueEventListener>()
        val expensesByGroup = mutableMapOf<String, List<Expense>>()

        fun emitTotals() {
            val allExpenses = expensesByGroup.values.flatten()
            onResult(computeTotals(userId, allExpenses))
        }

        val userGroupsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val activeGroupIds = snapshot.children.mapNotNull { it.key }.toSet()

                val removed = expenseListenersByGroup.keys - activeGroupIds
                removed.forEach { groupId ->
                    expenseListenersByGroup[groupId]?.let { listener ->
                        expensesRef.child(groupId).removeEventListener(listener)
                    }
                    expenseListenersByGroup.remove(groupId)
                    expensesByGroup.remove(groupId)
                }

                val added = activeGroupIds - expenseListenersByGroup.keys
                added.forEach { groupId ->
                    val listener = object : ValueEventListener {
                        override fun onDataChange(groupSnapshot: DataSnapshot) {
                            expensesByGroup[groupId] = groupSnapshot.children
                                .mapNotNull { parseExpense(it, groupId) }
                            emitTotals()
                        }

                        override fun onCancelled(error: DatabaseError) = onError(error)
                    }
                    expenseListenersByGroup[groupId] = listener
                    expensesRef.child(groupId).addValueEventListener(listener)
                }

                if (activeGroupIds.isEmpty()) {
                    expensesByGroup.clear()
                }
                emitTotals()
            }

            override fun onCancelled(error: DatabaseError) = onError(error)
        }

        userGroupsNode.addValueEventListener(userGroupsListener)

        return {
            userGroupsNode.removeEventListener(userGroupsListener)
            expenseListenersByGroup.forEach { (groupId, listener) ->
                expensesRef.child(groupId).removeEventListener(listener)
            }
            expenseListenersByGroup.clear()
            expensesByGroup.clear()
        }
    }

    private fun computeTotals(userId: String, expenses: List<Expense>): ProfileTotals {
        var totalSpentCents = 0L
        var totalReceivedCents = 0L
        var totalIOweCents = 0L
        val monthlySpent = mutableMapOf<String, Long>()

        expenses.forEach { expense ->
            val ownShare = expense.splits[userId] ?: 0L
            if (expense.paidByUid == userId) {
                totalSpentCents += expense.amountCents
                totalReceivedCents += (expense.amountCents - ownShare)
                val monthKey = monthFormat.format(Date(expense.createdAt))
                monthlySpent[monthKey] = (monthlySpent[monthKey] ?: 0L) + expense.amountCents
            } else {
                totalIOweCents += ownShare
            }
        }

        val totalOwedToMe = totalReceivedCents
        return ProfileTotals(
            totalSpentCents = totalSpentCents,
            totalReceivedCents = totalReceivedCents,
            totalIOweCents = totalIOweCents,
            totalOwedToMeCents = totalOwedToMe,
            netBalanceCents = totalOwedToMe - totalIOweCents,
            monthlySpentCents = monthlySpent.toSortedMap(compareByDescending { it })
        )
    }

    /**
     * Observe live profile summary for one user.
     * Recomputes when membership index changes.
     */
    fun observeProfileSummary(
        uid: String,
        onResult: (ProfileSummary) -> Unit,
        onError: (DatabaseError) -> Unit
    ): () -> Unit {
        return observeUserGroupsAndExpenses(
            userId = uid,
            onResult = { totals ->
                onResult(
                    ProfileSummary(
                        totalSpentCents = totals.totalSpentCents,
                        totalReceivedCents = totals.totalReceivedCents,
                        totalIOweCents = totals.totalIOweCents,
                        totalOwedToMeCents = totals.totalOwedToMeCents,
                        netBalanceCents = totals.netBalanceCents,
                        monthlySpentCents = totals.monthlySpentCents
                    )
                )
            },
            onError = onError
        )
    }

    private fun parseExpense(snapshot: DataSnapshot, groupId: String): Expense? {
        val id = snapshot.key ?: return null
        val map = snapshot.value as? Map<*, *> ?: return null
        val splitsRaw = map["splits"]
        val splits = when (splitsRaw) {
            is Map<*, *> -> splitsRaw.mapKeys { it.key.toString() }
                .mapValues { (it.value as? Number)?.toLong() ?: 0L }
            else -> emptyMap()
        }
        return Expense(
            id = id,
            groupId = groupId,
            description = (map["description"] as? String).orEmpty(),
            amountCents = (map["amountCents"] as? Number)?.toLong() ?: 0L,
            currency = (map["currency"] as? String) ?: "ILS",
            paidByUid = (map["paidByUid"] as? String).orEmpty(),
            splits = splits,
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L,
            category = (map["category"] as? String).orEmpty()
        )
    }
}
