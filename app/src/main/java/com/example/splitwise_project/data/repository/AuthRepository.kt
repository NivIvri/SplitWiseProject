package com.example.splitwise_project.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/** Repository for Firebase Authentication operations. */
class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    /** Get current authenticated user, or null if not logged in. */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /** Register a new user with email and password. */
    fun register(
        email: String,
        password: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                result.user?.let { onSuccess(it) } ?: onError("Registration failed")
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Registration failed")
            }
    }

    /** Login with email and password. */
    fun login(
        email: String,
        password: String,
        onSuccess: (FirebaseUser) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                result.user?.let { onSuccess(it) } ?: onError("Login failed")
            }
            .addOnFailureListener { e ->
                onError(e.message ?: "Login failed")
            }
    }

    /** Logout current user. */
    fun logout() {
        auth.signOut()
    }
}
