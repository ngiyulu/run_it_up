package com.example.runitup.mobile.service

import org.springframework.stereotype.Service

@Service
class PasswordValidator: BaseService() {
    private val lengthRegex = ".{8,}".toRegex()
    private val uppercaseRegex = "[A-Z]".toRegex()
    private val lowercaseRegex = "[a-z]".toRegex()
    private val digitRegex     = "\\d".toRegex()
    private val specialRegex   = "[^A-Za-z0-9]".toRegex()
    /**
     * Returns true if the password meets all criteria.
     */
    fun isValid(password: String): Boolean {
        return lengthRegex.containsMatchIn(password)
                && uppercaseRegex.containsMatchIn(password)
                && lowercaseRegex.containsMatchIn(password)
                && digitRegex.containsMatchIn(password)
                && specialRegex.containsMatchIn(password)
    }
    /**
     * Returns a list of human-readable errors; empty if valid.
     */
    fun validate(password: String): List<String> {
        val errors = mutableListOf<String>()
        if (!lengthRegex.containsMatchIn(password))   errors += "Must be at least 8 characters long"
        if (!uppercaseRegex.containsMatchIn(password))errors += "Must include at least one uppercase letter"
        if (!lowercaseRegex.containsMatchIn(password))errors += "Must include at least one lowercase letter"
        if (!digitRegex.containsMatchIn(password))    errors += "Must include at least one digit"
        if (!specialRegex.containsMatchIn(password))  errors += "Must include at least one special character"
        return errors
    }

}