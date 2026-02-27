package com.example.splitwise_project.data.model

import com.google.firebase.database.IgnoreExtraProperties

/** Timeline activity entry for a group. */
@IgnoreExtraProperties
data class ActivityItem(
    var id: String = "",
    var groupId: String = "",
    var type: String = "",
    var actorUid: String = "",
    var targetUid: String = "",
    var expenseId: String = "",
    var description: String = "",
    var amountCents: Long = 0L,
    var createdAt: Long = 0L
)
