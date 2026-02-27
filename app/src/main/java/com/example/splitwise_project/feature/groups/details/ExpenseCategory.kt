package com.example.splitwise_project.feature.groups.details

import com.example.splitwise_project.R

/** Shared category definitions for add-expense and expense rows. */
object ExpenseCategory {

    const val OTHER = "other"

    data class Option(
        val key: String,
        val label: String,
        val iconRes: Int
    )

    val options: List<Option> = listOf(
        Option("food", "Food", R.drawable.ic_category_food),
        Option("transport", "Transport", R.drawable.ic_category_transport),
        Option("shopping", "Shopping", R.drawable.ic_shopping_cart),
        Option("home", "Home", R.drawable.ic_category_other),
        Option("entertainment", "Entertainment", R.drawable.ic_entertainment),
        Option(OTHER, "Other", R.drawable.ic_category_other)
    )

    fun normalize(raw: String?): String {
        val key = raw?.trim()?.lowercase().orEmpty()
        return if (options.any { it.key == key }) key else OTHER
    }

    fun labelFor(raw: String?): String {
        val key = normalize(raw)
        return options.firstOrNull { it.key == key }?.label ?: "Other"
    }

    fun iconFor(raw: String?): Int {
        val key = normalize(raw)
        return options.firstOrNull { it.key == key }?.iconRes ?: R.drawable.ic_category_other
    }
}
