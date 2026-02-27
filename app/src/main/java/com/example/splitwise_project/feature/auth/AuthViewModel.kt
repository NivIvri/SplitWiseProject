package com.example.splitwise_project.feature.auth

import androidx.lifecycle.ViewModel
import com.example.splitwise_project.core.common.ValidationUtils
import com.example.splitwise_project.data.repository.AuthRepository
import com.example.splitwise_project.data.repository.UserRepository
import com.example.splitwise_project.data.model.User
import com.google.firebase.auth.FirebaseUser

/** ViewModel for authentication: login and registration. */
class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    /** Callback set by Fragment to show messages; cleared in onDestroyView to avoid leaks. */
    var onMessage: ((String) -> Unit)? = null

    /** Callback for successful authentication; passes FirebaseUser. */
    var onAuthSuccess: ((FirebaseUser) -> Unit)? = null

    /** Register a new user with email, password, and display name. */
    fun register(email: String, password: String, displayName: String) {
        val trimmedEmail = email.trim()
        val trimmedName = displayName.trim()

        if (!ValidationUtils.isNotBlank(trimmedEmail)) {
            onMessage?.invoke("Email cannot be empty")
            return
        }
        if (!ValidationUtils.isValidEmail(trimmedEmail)) {
            onMessage?.invoke("Invalid email format")
            return
        }
        if (password.length < 6) {
            onMessage?.invoke("Password must be at least 6 characters")
            return
        }
        if (!ValidationUtils.isNotBlank(trimmedName)) {
            onMessage?.invoke("Display name cannot be empty")
            return
        }

        authRepository.register(
            email = trimmedEmail,
            password = password,
            onSuccess = { firebaseUser ->
                val user = User(
                    uid = firebaseUser.uid,
                    email = trimmedEmail,
                    username = trimmedName,
                    displayName = trimmedName,
                    createdAt = System.currentTimeMillis()
                )
                userRepository.saveUser(user) { success ->
                    if (success) {
                        onAuthSuccess?.invoke(firebaseUser)
                    } else {
                        onMessage?.invoke("Failed to save user profile")
                    }
                }
            },
            onError = { error ->
                onMessage?.invoke(error)
            }
        )
    }

    /** Login with email and password. */
    fun login(email: String, password: String) {
        val trimmedEmail = email.trim()

        if (!ValidationUtils.isNotBlank(trimmedEmail)) {
            onMessage?.invoke("Email cannot be empty")
            return
        }
        if (!ValidationUtils.isValidEmail(trimmedEmail)) {
            onMessage?.invoke("Invalid email format")
            return
        }
        if (password.isEmpty()) {
            onMessage?.invoke("Password cannot be empty")
            return
        }

        authRepository.login(
            email = trimmedEmail,
            password = password,
            onSuccess = { firebaseUser ->
                onAuthSuccess?.invoke(firebaseUser)
            },
            onError = { error ->
                onMessage?.invoke(error)
            }
        )
    }
}
