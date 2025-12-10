package com.example.weatherapp.domain.auth


sealed class AuthResult {
    object Success : AuthResult()
    data class ValidationError(
        val usernameError: String? = null,
        val emailError: String? = null,
        val passwordError: String? = null,
        val confirmPasswordError: String? = null,
        val generalError: String? = null
    ) : AuthResult()
}

interface AuthRepository {
    suspend fun register(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): AuthResult

    suspend fun login(
        email: String,
        password: String
    ): AuthResult

    suspend fun logout()

    suspend fun getLoggedInUser(): User?
}
