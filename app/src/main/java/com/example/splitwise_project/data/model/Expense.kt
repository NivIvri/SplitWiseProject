package com.example.splitwise_project.data.model

import com.google.firebase.database.IgnoreExtraProperties

/** Expense model for Firebase; id is populated from snapshot.key when reading. */
@IgnoreExtraProperties
data class Expense(
    var id: String = "",
    var groupId: String = "",
    var description: String = "",
    var amountCents: Long = 0L,
    var currency: String = "ILS",
    var paidByUid: String = "",
    var splits: Map<String, Long> = emptyMap(),
    var createdAt: Long = 0L
)
