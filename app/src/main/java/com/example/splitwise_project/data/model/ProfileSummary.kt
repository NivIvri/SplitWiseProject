package com.example.splitwise_project.data.model

/** Aggregated profile metrics for the logged-in user. */
data class ProfileSummary(
    val totalSpentCents: Long = 0L,
    val totalReceivedCents: Long = 0L,
    val totalIOweCents: Long = 0L,
    val totalOwedToMeCents: Long = 0L,
    val netBalanceCents: Long = 0L,
    val monthlySpentCents: Map<String, Long> = emptyMap()
)
