package com.example.splitwise_project.data.repository

import com.example.splitwise_project.data.model.Expense
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Repository for expenses stored at expenses/{groupId}/{expenseId}.
 * Separate top-level node for cleaner security rules.
 */
class ExpenseRepository {

    private val expensesRef = FirebaseDatabase.getInstance().reference.child("expenses")

    /** Observe expenses for a group in realtime. Returns cleanup function. */
    fun observeExpenses(
        groupId: String,
        onResult: (List<Expense>) -> Unit,
        onError: (DatabaseError) -> Unit
    ): () -> Unit {
        val ref = expensesRef.child(groupId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { parseExpense(it, groupId) }
                onResult(list)
            }
            override fun onCancelled(error: DatabaseError) = onError(error)
        }
        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    /** Observe one expense by id in realtime. Returns cleanup function. */
    fun observeExpense(
        groupId: String,
        expenseId: String,
        onResult: (Expense?) -> Unit,
        onError: (DatabaseError) -> Unit
    ): () -> Unit {
        val ref = expensesRef.child(groupId).child(expenseId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onResult(parseExpense(snapshot, groupId))
            }
            override fun onCancelled(error: DatabaseError) = onError(error)
        }
        ref.addValueEventListener(listener)
        return { ref.removeEventListener(listener) }
    }

    /** Create an expense under expenses/{groupId}. Returns generated expenseId on success. */
    fun createExpense(expense: Expense, onDone: (Boolean, String?) -> Unit) {
        val ref = expensesRef.child(expense.groupId).push()
        val key = ref.key ?: run { onDone(false, null); return }
        val toSave = expense.copy(id = key)
        ref.setValue(expenseToMap(toSave))
            .addOnSuccessListener { onDone(true, key) }
            .addOnFailureListener { onDone(false, null) }
    }

    /** Read all expenses for a group once. */
    fun getExpensesOnce(groupId: String, onResult: (List<Expense>) -> Unit) {
        expensesRef.child(groupId).get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.children.mapNotNull { parseExpense(it, groupId) })
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
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
            createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun expenseToMap(e: Expense): Map<String, Any> = mapOf(
        "id" to e.id,
        "description" to e.description,
        "amountCents" to e.amountCents,
        "currency" to e.currency,
        "paidByUid" to e.paidByUid,
        "splits" to e.splits,
        "createdAt" to e.createdAt
    )
}
