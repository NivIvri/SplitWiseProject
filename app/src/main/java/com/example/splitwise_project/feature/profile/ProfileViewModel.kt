package com.example.splitwise_project.feature.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.splitwise_project.data.model.MonthlySpent
import com.example.splitwise_project.data.model.ProfileTotals
import com.example.splitwise_project.data.repository.AuthRepository
import com.example.splitwise_project.data.repository.ProfileRepository
import com.google.firebase.database.DatabaseError

/** ViewModel for profile summary screen. */
class ProfileViewModel : ViewModel() {

    private val authRepo = AuthRepository()
    private val profileRepo = ProfileRepository()

    private val _totals = MutableLiveData(ProfileTotals())
    val totals: LiveData<ProfileTotals> = _totals

    private val _monthly = MutableLiveData<List<MonthlySpent>>(emptyList())
    val monthly: LiveData<List<MonthlySpent>> = _monthly

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _stateText = MutableLiveData<String?>(null)
    val stateText: LiveData<String?> = _stateText

    private val _uiMessage = MutableLiveData<String>()
    val uiMessage: LiveData<String> = _uiMessage

    private var stopObserving: (() -> Unit)? = null

    /** Start profile summary observation (idempotent). */
    fun start() {
        if (stopObserving != null) return
        val uid = authRepo.getCurrentUser()?.uid
        if (uid.isNullOrBlank()) {
            _stateText.postValue("Please login to view your profile summary.")
            return
        }

        _isLoading.postValue(true)
        _stateText.postValue(null)

        stopObserving = profileRepo.observeUserGroupsAndExpenses(
            userId = uid,
            onResult = { totals ->
                _isLoading.postValue(false)
                _totals.postValue(totals)
                _monthly.postValue(
                    totals.monthlySpentCents.map { (month, cents) ->
                        MonthlySpent(month = month, amountCents = cents)
                    }
                )
                val isEmpty = totals.totalSpentCents == 0L &&
                    totals.totalIOweCents == 0L &&
                    totals.totalOwedToMeCents == 0L &&
                    totals.monthlySpentCents.isEmpty()
                _stateText.postValue(if (isEmpty) "No expenses yet." else null)
            },
            onError = { e: DatabaseError ->
                _isLoading.postValue(false)
                val message = e.message ?: "Failed to load profile summary."
                _stateText.postValue(message)
                _uiMessage.postValue(message)
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopObserving?.invoke()
        stopObserving = null
    }
}
