package com.example.splitwise_project.data.model

/** Per-member balance in a group: positive is owed to them, negative means they owe. */
data class MemberBalance(
    val uid: String = "",
    val name: String = "",
    val balanceCents: Long = 0L
)
