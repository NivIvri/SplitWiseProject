package com.example.splitwise_project.data.model

import com.google.firebase.database.IgnoreExtraProperties

/** User model for Firebase; uid is populated from snapshot.key when reading. */
@IgnoreExtraProperties
data class User(
    var uid: String = "",
    var email: String = "",
    var username: String = "",
    var displayName: String = "",
    var createdAt: Long = 0L
)
