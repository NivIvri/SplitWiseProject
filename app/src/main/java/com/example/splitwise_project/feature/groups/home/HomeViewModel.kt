package com.example.splitwise_project.feature.groups.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.splitwise_project.data.model.Expense
import com.example.splitwise_project.data.model.Group
import com.example.splitwise_project.data.repository.AuthRepository
import com.example.splitwise_project.data.repository.ExpenseRepository
import com.example.splitwise_project.data.repository.GroupRepository
import com.example.splitwise_project.data.repository.UserRepository
import com.google.firebase.database.DatabaseError

/** ViewModel for home screen: observes user's groups and supports creating a new group. */
class HomeViewModel : ViewModel() {

    private val groupRepo = GroupRepository()
    private val authRepo = AuthRepository()
    private val expenseRepo = ExpenseRepository()
    private val userRepo = UserRepository()

    private val _groups = MutableLiveData<List<Group>>(emptyList())
    val groups: LiveData<List<Group>> = _groups

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    /** Callback set by Fragment; cleared in onDestroyView. */
    var onMessage: ((String) -> Unit)? = null

    private var stopObserving: (() -> Unit)? = null
    private var stopUsers: (() -> Unit)? = null
    private var cachedGroups: List<Group> = emptyList()
    private var uidToName: Map<String, String> = emptyMap()

    /** Start observing groups via userGroups index. Idempotent. */
    fun start() {
        if (stopObserving != null || stopUsers != null) return
        _isLoading.postValue(true)
        val uid = authRepo.getCurrentUser()?.uid
        if (uid == null) {
            onMessage?.invoke("Please login to see your groups")
            _isLoading.postValue(false)
            return
        }

        stopObserving = groupRepo.observeUserGroups(
            uid = uid,
            onResult = { list ->
                val sorted = list.sortedByDescending { it.createdAt }
                enrichBalances(uid, sorted)
            },
            onError = { error: DatabaseError ->
                onMessage?.invoke(error.message ?: "Error loading groups")
                _isLoading.postValue(false)
            }
        )

        stopUsers = userRepo.observeAllUsers(
            onResult = { users ->
                uidToName = users.associate { u ->
                    u.uid to u.username.ifBlank { u.displayName.ifBlank { u.email.substringBefore("@") } }
                }
                applyMemberPreviews()
            },
            onError = { /* Member names preview is best-effort; do not fail list loading. */ }
        )
    }

    /** Stop current observation so it can be restarted (e.g. after login). */
    fun stop() {
        stopObserving?.invoke()
        stopUsers?.invoke()
        stopObserving = null
        stopUsers = null
        cachedGroups = emptyList()
        uidToName = emptyMap()
        _groups.value = emptyList()
        _isLoading.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopObserving?.invoke()
        stopUsers?.invoke()
        stopObserving = null
        stopUsers = null
    }

    /** Create a group with the given name; writes atomically to 3 nodes. */
    fun createGroup(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            onMessage?.invoke("Group name cannot be empty")
            return
        }
        val uid = authRepo.getCurrentUser()?.uid
        if (uid == null) {
            onMessage?.invoke("Please login to create a group")
            return
        }
        groupRepo.createGroup(trimmed, uid) { success ->
            onMessage?.invoke(if (success) "Group created" else "Failed to create group")
        }
    }

    private fun enrichBalances(currentUid: String, groups: List<Group>) {
        if (groups.isEmpty()) {
            cachedGroups = emptyList()
            _groups.postValue(emptyList())
            _isLoading.postValue(false)
            return
        }
        val updated = groups.toMutableList()
        var remaining = groups.size
        groups.forEachIndexed { index, group ->
            expenseRepo.getExpensesOnce(group.id) { expenses ->
                val net = computeMyNetBalance(expenses, currentUid)
                updated[index] = group.copy(myBalanceCents = net)
                remaining--
                if (remaining == 0) {
                    cachedGroups = updated.sortedByDescending { it.createdAt }
                    applyMemberPreviews()
                    _isLoading.postValue(false)
                }
            }
        }
    }

    private fun applyMemberPreviews() {
        val previewed = cachedGroups.map { group ->
            group.copy(memberPreview = buildMemberPreview(group.memberUids, group.memberCount))
        }
        _groups.postValue(previewed)
    }

    private fun buildMemberPreview(memberUids: List<String>, memberCount: Int): String {
        val names = memberUids
            .mapNotNull { uidToName[it] }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        return when {
            names.isEmpty() -> "$memberCount member${if (memberCount == 1) "" else "s"}"
            names.size <= 3 -> names.joinToString(", ")
            else -> names.take(3).joinToString(", ") + " +${names.size - 3}"
        }
    }

    private fun computeMyNetBalance(expenses: List<Expense>, currentUid: String): Long {
        var net = 0L
        expenses.forEach { e ->
            if (e.paidByUid == currentUid) net += e.amountCents
            net -= e.splits[currentUid] ?: 0L
        }
        return net
    }
}
