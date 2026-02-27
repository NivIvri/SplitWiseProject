package com.example.splitwise_project.data.repository

import com.example.splitwise_project.data.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/** Repository for user profile operations in Firebase Realtime Database. */
class UserRepository {

    private val usersRef = FirebaseDatabase.getInstance().reference.child("users")
    private val usersByEmailRef = FirebaseDatabase.getInstance().reference.child("usersByEmail")

    /** Save/update users/{uid} and usersByEmail/{encodedEmail} atomically. */
    fun saveUser(user: User, onDone: (Boolean) -> Unit) {
        val normalizedEmail = user.email.trim().lowercase()
        val encodedEmail = encodeEmailKey(normalizedEmail)
        val updates = mapOf<String, Any>(
            "users/${user.uid}" to userToMap(user.copy(email = normalizedEmail)),
            "usersByEmail/$encodedEmail" to user.uid
        )

        FirebaseDatabase.getInstance().reference.updateChildren(updates)
            .addOnSuccessListener { onDone(true) }
            .addOnFailureListener { onDone(false) }
    }

    /** Get user by uid; calls onResult with User or null if not found. */
    fun getUserByUid(uid: String, onResult: (User?) -> Unit) {
        usersRef.child(uid).get()
            .addOnSuccessListener { snapshot ->
                onResult(parseUser(snapshot))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    /** Get user by email via usersByEmail/{encodedEmail} index. */
    fun getUserByEmail(email: String, onResult: (User?) -> Unit) {
        getUidByEmail(email) { uid ->
            if (uid.isNullOrBlank()) {
                onResult(null)
            } else {
                getUserByUid(uid, onResult)
            }
        }
    }

    /** Observe all users in realtime. Returns cleanup function. */
    fun observeAllUsers(
        onResult: (List<User>) -> Unit,
        onError: (DatabaseError) -> Unit
    ): () -> Unit {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.mapNotNull { parseUser(it) }
                onResult(users)
            }

            override fun onCancelled(error: DatabaseError) = onError(error)
        }
        usersRef.addValueEventListener(listener)
        return { usersRef.removeEventListener(listener) }
    }

    /** Read multiple users by uid; returns only users that exist. */
    fun getUsersByUids(uids: List<String>, onResult: (List<User>) -> Unit) {
        if (uids.isEmpty()) {
            onResult(emptyList())
            return
        }
        val result = mutableListOf<User>()
        var remaining = uids.size
        uids.forEach { uid ->
            getUserByUid(uid) { user ->
                user?.let { result.add(it) }
                remaining--
                if (remaining == 0) onResult(result)
            }
        }
    }

    /** Resolve uid by email from usersByEmail/{encodedEmail}; returns null if not found. */
    fun getUidByEmail(email: String, onResult: (String?) -> Unit) {
        val encodedEmail = encodeEmailKey(email.trim().lowercase())
        usersByEmailRef.child(encodedEmail).get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.getValue(String::class.java))
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    /**
     * Encode email to a Firebase-safe key for usersByEmail index.
     * We replace '.' with ',' because Firebase keys cannot contain '.'.
     */
    private fun encodeEmailKey(email: String): String {
        return email.replace(".", ",")
    }

    private fun parseUser(snapshot: DataSnapshot): User? {
        val uid = snapshot.key ?: return null
        val map = snapshot.value as? Map<*, *> ?: return null
        val email = (map["email"] as? String).orEmpty()
        val username = (map["username"] as? String)
            ?: (map["displayName"] as? String).orEmpty()
        val displayName = (map["displayName"] as? String)
            ?: (map["username"] as? String).orEmpty()
        val createdAt = (map["createdAt"] as? Number)?.toLong() ?: 0L
        return User(
            uid = uid,
            email = email,
            username = username,
            displayName = displayName,
            createdAt = createdAt
        )
    }

    private fun userToMap(user: User): Map<String, Any> = mapOf(
        "uid" to user.uid,
        "email" to user.email.lowercase(),
        // Always persist a non-empty username for UI rendering (activity, members, etc.).
        "username" to user.username.ifBlank {
            user.displayName.ifBlank { user.email.substringBefore("@").ifBlank { "user" } }
        },
        "displayName" to user.displayName,
        "createdAt" to user.createdAt
    )
}
