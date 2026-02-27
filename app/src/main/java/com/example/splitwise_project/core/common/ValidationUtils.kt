package com.example.splitwise_project.core.common

/** Validation utilities for input checking. */
object ValidationUtils {

    /** Check if email format is valid. */
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    /** Check if amount is positive. */
    fun isValidAmount(amount: Double): Boolean {
        return amount > 0
    }

    /** Check if string is not blank after trimming. */
    fun isNotBlank(value: String): Boolean {
        return value.trim().isNotBlank()
    }
}
