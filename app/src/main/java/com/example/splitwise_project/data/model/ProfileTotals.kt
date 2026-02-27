package com.example.splitwise_project.data.model

/** Realtime totals shown on profile screen. */
data class ProfileTotals(
    val totalSpentCents: Long = 0L,
    val totalReceivedCents: Long = 0L,
    val totalIOweCents: Long = 0L,
    val totalOwedToMeCents: Long = 0L,
    val netBalanceCents: Long = 0L,
    val monthlySpentCents: Map<String, Long> = emptyMap()
)
