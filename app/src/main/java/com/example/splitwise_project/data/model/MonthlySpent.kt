package com.example.splitwise_project.data.model

/** UI-friendly monthly spent row (YYYY-MM, cents). */
data class MonthlySpent(
    val month: String = "",
    val amountCents: Long = 0L
)
