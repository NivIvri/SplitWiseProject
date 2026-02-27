package com.example.splitwise_project.feature.activity

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.splitwise_project.data.model.ActivityItem
import com.example.splitwise_project.data.repository.AuthRepository
import com.example.splitwise_project.data.repository.GroupRepository
import com.example.splitwise_project.data.repository.UserRepository
import com.google.firebase.database.DatabaseError

/** ViewModel for global activity feed (all user's groups). */
class ActivityViewModel : ViewModel() {

    private val authRepo = AuthRepository()
    private val groupRepo = GroupRepository()
    private val userRepo = UserRepository()

    private val _activities = MutableLiveData<List<ActivityItem>>(emptyList())
    val activities: LiveData<List<ActivityItem>> = _activities

    private val _uiMessage = MutableLiveData<String>()
    val uiMessage: LiveData<String> = _uiMessage

    private val _uidToUsername = MutableLiveData<Map<String, String>>(emptyMap())
    val uidToUsername: LiveData<Map<String, String>> = _uidToUsername

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private var stopObserving: (() -> Unit)? = null
    private var stopUsers: (() -> Unit)? = null

    /** Start observing all activity for current user. Idempotent. */
    fun start() {
        if (stopObserving != null || stopUsers != null) return
        _isLoading.postValue(true)
        val uid = authRepo.getCurrentUser()?.uid
        if (uid.isNullOrBlank()) {
            _uiMessage.postValue("Please login to see activity.")
            _activities.postValue(emptyList())
            _uidToUsername.postValue(emptyMap())
            _isLoading.postValue(false)
            return
        }
        stopObserving = groupRepo.observeUserActivities(
            userUid = uid,
            onResult = {
                _activities.postValue(it)
                _isLoading.postValue(false)
            },
            onError = { e: DatabaseError ->
                _uiMessage.postValue(e.message ?: "Failed to load activity.")
                _isLoading.postValue(false)
            }
        )
        stopUsers = userRepo.observeAllUsers(
            onResult = { users ->
                val map = users.associate { user ->
                    val label = user.username.ifBlank {
                        user.displayName.ifBlank { user.email.substringBefore("@").ifBlank { "Unknown user" } }
                    }
                    user.uid to label
                }
                _uidToUsername.postValue(map)
            },
            onError = { e: DatabaseError -> _uiMessage.postValue(e.message ?: "Failed to load users.") }
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopObserving?.invoke()
        stopUsers?.invoke()
        stopObserving = null
        stopUsers = null
    }
}
