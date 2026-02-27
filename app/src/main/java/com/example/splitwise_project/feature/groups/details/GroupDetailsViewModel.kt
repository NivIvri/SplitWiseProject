package com.example.splitwise_project.feature.groups.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.splitwise_project.core.common.ValidationUtils
import com.example.splitwise_project.data.model.Expense
import com.example.splitwise_project.data.model.Group
import com.example.splitwise_project.data.model.ActivityItem
import com.example.splitwise_project.data.model.MemberBalance
import com.example.splitwise_project.data.model.Settlement
import com.example.splitwise_project.data.model.User
import com.example.splitwise_project.data.repository.AuthRepository
import com.example.splitwise_project.data.repository.ExpenseRepository
import com.example.splitwise_project.data.repository.GroupRepository
import com.example.splitwise_project.data.repository.UserRepository
import com.google.firebase.database.DatabaseError

/** ViewModel for group details: group info, members, expenses. */
class GroupDetailsViewModel : ViewModel() {

    private val groupRepo = GroupRepository()
    private val expenseRepo = ExpenseRepository()
    private val userRepo = UserRepository()
    private val authRepo = AuthRepository()

    private val _group = MutableLiveData<Group?>()
    val group: LiveData<Group?> = _group

    private val _memberUids = MutableLiveData<List<String>>(emptyList())
    val memberUids: LiveData<List<String>> = _memberUids

    private val _expenses = MutableLiveData<List<Expense>>(emptyList())
    val expenses: LiveData<List<Expense>> = _expenses

    private val _settlements = MutableLiveData<List<Settlement>>(emptyList())
    val settlements: LiveData<List<Settlement>> = _settlements

    private val _activities = MutableLiveData<List<ActivityItem>>(emptyList())
    val activities: LiveData<List<ActivityItem>> = _activities

    private val _allUsers = MutableLiveData<List<User>>(emptyList())
    val allUsers: LiveData<List<User>> = _allUsers

    private val _memberUsers = MutableLiveData<List<User>>(emptyList())
    val memberUsers: LiveData<List<User>> = _memberUsers

    private val _uidToName = MutableLiveData<Map<String, String>>(emptyMap())
    val uidToName: LiveData<Map<String, String>> = _uidToName

    private val _memberBalances = MutableLiveData<List<MemberBalance>>(emptyList())
    val memberBalances: LiveData<List<MemberBalance>> = _memberBalances

    private val _isBalanceLoading = MutableLiveData(true)
    val isBalanceLoading: LiveData<Boolean> = _isBalanceLoading

    private val _uiMessage = MutableLiveData<String>()
    val uiMessage: LiveData<String> = _uiMessage

    private var stopGroup: (() -> Unit)? = null
    private var stopMembers: (() -> Unit)? = null
    private var stopExpenses: (() -> Unit)? = null
    private var stopActivities: (() -> Unit)? = null
    private var stopAllUsers: (() -> Unit)? = null
    private var currentGroupId: String? = null
    private var expensesLoaded = false
    private var membersLoaded = false
    private var latestExpenses: List<Expense> = emptyList()

    /** Start observing group, members, expenses. Idempotent. */
    fun start(groupId: String) {
        if (stopGroup != null) return
        currentGroupId = groupId
        expensesLoaded = false
        membersLoaded = false
        latestExpenses = emptyList()
        _isBalanceLoading.postValue(true)

        stopGroup = groupRepo.observeGroup(
            groupId = groupId,
            onResult = { _group.postValue(it) },
            onError = { e: DatabaseError -> postMessage(e.message ?: "Error") }
        )

        stopMembers = groupRepo.observeGroupMembers(
            groupId = groupId,
            onResult = { uids ->
                membersLoaded = true
                _memberUids.postValue(uids)
                refreshMemberUsers(uids)
                recomputeDerivedState()
            },
            onError = { e: DatabaseError -> postMessage(e.message ?: "Error") }
        )

        stopExpenses = expenseRepo.observeExpenses(
            groupId = groupId,
            onResult = { list ->
                val sorted = list.sortedByDescending { it.createdAt }
                expensesLoaded = true
                latestExpenses = sorted
                _expenses.postValue(sorted)
                recomputeDerivedState()
            },
            onError = { e: DatabaseError -> postMessage(e.message ?: "Error") }
        )

        stopActivities = groupRepo.observeGroupActivities(
            groupId = groupId,
            onResult = { _activities.postValue(it) },
            onError = { e: DatabaseError -> postMessage(e.message ?: "Error") }
        )

        stopAllUsers = userRepo.observeAllUsers(
            onResult = { users ->
                val currentUid = authRepo.getCurrentUser()?.uid
                _allUsers.postValue(users.filter { it.uid != currentUid })
            },
            onError = { e: DatabaseError -> postMessage(e.message ?: "Error") }
        )
    }

    /** Add multiple selected users to the group. */
    fun addMembersToGroup(groupId: String, selectedUids: List<String>) {
        if (groupId.isBlank()) {
            postMessage("Group not loaded")
            return
        }
        if (selectedUids.isEmpty()) {
            postMessage("Please select at least one user.")
            return
        }

        val existing = _memberUids.value?.toSet() ?: emptySet()
        val toAdd = selectedUids.filterNot { existing.contains(it) }
        if (toAdd.isEmpty()) {
            postMessage("Selected users are already members.")
            return
        }

        val actorUid = authRepo.getCurrentUser()?.uid
        groupRepo.addMembers(groupId, toAdd, actorUid) { ok ->
            postMessage(if (ok) "Members added successfully." else "Failed to add members.")
        }
    }

    /** Add an expense with equal split among selected members. */
    fun addExpense(
        description: String,
        amount: Double,
        splitMemberUids: List<String>,
        category: String = ExpenseCategory.OTHER
    ) {
        val desc = description.trim()
        if (desc.isEmpty()) { postMessage("Description required"); return }
        if (!ValidationUtils.isValidAmount(amount)) { postMessage("Amount must be > 0"); return }

        val uid = authRepo.getCurrentUser()?.uid ?: run { postMessage("Not logged in"); return }
        val groupId = currentGroupId ?: run { postMessage("Group not loaded"); return }
        val members = splitMemberUids.distinct()
        if (members.isEmpty()) { postMessage("No members to split with"); return }

        val amountCents = (amount * 100).toLong()
        val perPerson = amountCents / members.size
        val remainder = amountCents % members.size
        val splits = members.mapIndexed { i, m ->
            m to (perPerson + if (i == 0) remainder else 0L)
        }.toMap()

        val expense = Expense(
            groupId = groupId,
            description = desc,
            amountCents = amountCents,
            currency = "ILS",
            paidByUid = uid,
            splits = splits,
            createdAt = System.currentTimeMillis(),
            category = ExpenseCategory.normalize(category)
        )
        expenseRepo.createExpense(expense) { ok, expenseId ->
            if (!ok) {
                postMessage("Failed to add expense")
                return@createExpense
            }
            val activity = ActivityItem(
                groupId = groupId,
                type = "expense_added",
                actorUid = uid,
                expenseId = expenseId.orEmpty(),
                description = desc,
                amountCents = amountCents,
                createdAt = System.currentTimeMillis()
            )
            groupRepo.addGroupActivity(activity) { /* best-effort timeline logging */ }
            postMessage("Expense added")
        }
    }

    private fun postMessage(message: String) {
        _uiMessage.postValue(message)
    }

    private fun refreshMemberUsers(uids: List<String>) {
        userRepo.getUsersByUids(uids) { users ->
            val sorted = users.sortedBy { it.username.ifBlank { it.displayName } }
            val namesByUid = sorted.associate { user ->
                user.uid to user.username.ifBlank { user.displayName.ifBlank { user.email } }
            }
            _memberUsers.postValue(sorted)
            _uidToName.postValue(namesByUid)
            recomputeDerivedState(namesByUid)
        }
    }

    /** Recompute all derived balance outputs from latest canonical snapshots. */
    private fun recomputeDerivedState(namesOverride: Map<String, String>? = null) {
        if (!membersLoaded || !expensesLoaded) {
            _isBalanceLoading.postValue(true)
            return
        }
        _isBalanceLoading.postValue(false)
        _settlements.postValue(buildOptimizedSettlements(latestExpenses))
        recomputeMemberBalances(latestExpenses, namesOverride)
    }

    private fun recomputeMemberBalances(
        expenses: List<Expense>,
        namesOverride: Map<String, String>? = null
    ) {
        val netByUid = mutableMapOf<String, Long>()

        // Ensure every current member appears even if they have zero balance.
        (_memberUids.value ?: emptyList()).forEach { uid -> netByUid[uid] = 0L }

        expenses.forEach { expense ->
            if (expense.paidByUid.isBlank() || expense.amountCents <= 0L) return@forEach
            netByUid[expense.paidByUid] = (netByUid[expense.paidByUid] ?: 0L) + expense.amountCents
            expense.splits.forEach { (uid, split) ->
                netByUid[uid] = (netByUid[uid] ?: 0L) - split
            }
        }

        val namesByUid = namesOverride ?: (_memberUsers.value ?: emptyList()).associateBy({ it.uid }) {
            it.username.ifBlank { it.displayName.ifBlank { it.email } }
        }
        val rows = netByUid.map { (uid, cents) ->
            MemberBalance(uid = uid, name = namesByUid[uid].orEmpty(), balanceCents = cents)
        }.sortedByDescending { it.balanceCents }
        _memberBalances.postValue(rows)
    }

    /**
     * Convert raw expense splits into optimized debtor -> creditor transfers.
     * Positive net means user should receive; negative net means user should pay.
     */
    private fun buildOptimizedSettlements(expenses: List<Expense>): List<Settlement> {
        val netByUid = mutableMapOf<String, Long>()

        expenses.forEach { expense ->
            if (expense.amountCents <= 0L || expense.paidByUid.isBlank()) return@forEach

            netByUid[expense.paidByUid] = (netByUid[expense.paidByUid] ?: 0L) + expense.amountCents
            expense.splits.forEach { (uid, splitCents) ->
                netByUid[uid] = (netByUid[uid] ?: 0L) - splitCents
            }
        }

        val debtors = netByUid
            .filter { it.value < 0L }
            .map { it.key to -it.value } // amount to pay
            .toMutableList()
        val creditors = netByUid
            .filter { it.value > 0L }
            .map { it.key to it.value } // amount to receive
            .toMutableList()

        val result = mutableListOf<Settlement>()
        var i = 0
        var j = 0
        while (i < debtors.size && j < creditors.size) {
            val (debtorUid, debtorAmount) = debtors[i]
            val (creditorUid, creditorAmount) = creditors[j]
            val transfer = minOf(debtorAmount, creditorAmount)
            if (transfer > 0L) {
                result.add(Settlement(fromUid = debtorUid, toUid = creditorUid, amountCents = transfer))
            }

            val remainingDebtor = debtorAmount - transfer
            val remainingCreditor = creditorAmount - transfer
            debtors[i] = debtorUid to remainingDebtor
            creditors[j] = creditorUid to remainingCreditor
            if (remainingDebtor == 0L) i++
            if (remainingCreditor == 0L) j++
        }
        return result
    }

    override fun onCleared() {
        super.onCleared()
        stopGroup?.invoke()
        stopMembers?.invoke()
        stopExpenses?.invoke()
        stopActivities?.invoke()
        stopAllUsers?.invoke()
    }
}
