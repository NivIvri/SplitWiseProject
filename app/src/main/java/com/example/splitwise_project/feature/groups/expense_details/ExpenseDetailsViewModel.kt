package com.example.splitwise_project.feature.groups.expense_details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.splitwise_project.data.model.Expense
import com.example.splitwise_project.data.model.User
import com.example.splitwise_project.data.repository.ExpenseRepository
import com.example.splitwise_project.data.repository.UserRepository
import com.google.firebase.database.DatabaseError

/** ViewModel for expense details screen. */
class ExpenseDetailsViewModel : ViewModel() {

    private val expenseRepo = ExpenseRepository()
    private val userRepo = UserRepository()

    private val _expense = MutableLiveData<Expense?>()
    val expense: LiveData<Expense?> = _expense

    private val _paidByLabel = MutableLiveData<String>("")
    val paidByLabel: LiveData<String> = _paidByLabel

    private val _splitRows = MutableLiveData<List<Pair<String, Long>>>(emptyList())
    val splitRows: LiveData<List<Pair<String, Long>>> = _splitRows

    private val _uiMessage = MutableLiveData<String>()
    val uiMessage: LiveData<String> = _uiMessage

    private var stopExpense: (() -> Unit)? = null

    /** Start observing expense by id. */
    fun start(groupId: String, expenseId: String) {
        if (stopExpense != null) return
        stopExpense = expenseRepo.observeExpense(
            groupId = groupId,
            expenseId = expenseId,
            onResult = { expense ->
                _expense.postValue(expense)
                if (expense == null) {
                    _paidByLabel.postValue("")
                    _splitRows.postValue(emptyList())
                    return@observeExpense
                }
                resolvePaidBy(expense.paidByUid)
                resolveSplitRows(expense.splits)
            },
            onError = { e: DatabaseError -> _uiMessage.postValue(e.message ?: "Failed to load expense details.") }
        )
    }

    private fun resolvePaidBy(uid: String) {
        if (uid.isBlank()) {
            _paidByLabel.postValue("")
            return
        }
        userRepo.getUserByUid(uid) { user ->
            _paidByLabel.postValue(user.toDisplayName())
        }
    }

    private fun resolveSplitRows(splits: Map<String, Long>) {
        val uids = splits.keys.toList()
        userRepo.getUsersByUids(uids) { users ->
            val byUid = users.associateBy { it.uid }
            val rows = splits.map { (uid, cents) ->
                val label = byUid[uid].toDisplayName()
                label to cents
            }.sortedByDescending { it.second }
            _splitRows.postValue(rows)
        }
    }

    private fun User?.toDisplayName(): String {
        if (this == null) return ""
        return username.ifBlank { displayName.ifBlank { email.ifBlank { uid.take(8) + "…" } } }
    }

    override fun onCleared() {
        super.onCleared()
        stopExpense?.invoke()
        stopExpense = null
    }
}
