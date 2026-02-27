package com.example.splitwise_project.data.model

import com.google.firebase.database.IgnoreExtraProperties

/** Group metadata stored at groups/{groupId}. Membership is in a separate node. */
@IgnoreExtraProperties
data class Group(
    var id: String = "",
    var name: String = "",
    var createdByUid: String = "",
    var createdAt: Long = 0L,
    var memberCount: Int = 0,
    var myBalanceCents: Long = 0L,
    var memberUids: List<String> = emptyList(),
    var memberPreview: String = ""
)
