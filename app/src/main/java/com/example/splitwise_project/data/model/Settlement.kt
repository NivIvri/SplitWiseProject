package com.example.splitwise_project.data.model

/** Optimized transfer recommendation between two members. */
data class Settlement(
    val fromUid: String = "",
    val toUid: String = "",
    val amountCents: Long = 0L
)
