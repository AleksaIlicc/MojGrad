package com.mojgrad.utils

import android.util.Patterns

object ValidationHelper {
    
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
    
    fun isValidPhone(phone: String): Boolean {
        return phone.isNotBlank() && phone.length >= 8
    }
    
    fun isValidName(name: String): Boolean {
        return name.isNotBlank() && name.trim().length >= 2
    }
    
    fun getPasswordValidationMessage(password: String): String? {
        return when {
            password.isEmpty() -> "Lozinka je obavezna"
            password.length < 6 -> "Lozinka mora imati najmanje 6 karaktera"
            else -> null
        }
    }
    
    fun getEmailValidationMessage(email: String): String? {
        return when {
            email.isEmpty() -> "Email je obavezan"
            !isValidEmail(email) -> "Unesite validan email"
            else -> null
        }
    }
}
