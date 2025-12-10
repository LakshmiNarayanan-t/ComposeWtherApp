package com.example.weatherapp.data.repository

import android.util.Base64
import android.util.Patterns
import com.example.weatherapp.domain.auth.AuthRepository
import com.example.weatherapp.domain.auth.AuthResult
import com.example.weatherapp.domain.auth.User
import com.example.weatherapp.data.local.UserDao
import com.example.weatherapp.data.local.UserEntity
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : AuthRepository {

    override suspend fun register(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): AuthResult {
        // Basic validation
        if (username.isBlank()) {
            return AuthResult.ValidationError(usernameError = "Username is required")
        }
        if (email.isBlank()) {
            return AuthResult.ValidationError(emailError = "Email is required")
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return AuthResult.ValidationError(emailError = "Invalid email")
        }
        if (password.isBlank()) {
            return AuthResult.ValidationError(passwordError = "Password is required")
        }
        if (password.length < 6) {
            return AuthResult.ValidationError(passwordError = "Password must be at least 6 characters")
        }
        if (confirmPassword.isBlank()) {
            return AuthResult.ValidationError(confirmPasswordError = "Confirm password is required")
        }
        if (password != confirmPassword) {
            return AuthResult.ValidationError(confirmPasswordError = "Passwords do not match")
        }

        // Duplicate email check
        val existing = runCatching { userDao.getUserByEmail(email.trim()) }.getOrNull()
        if (existing != null) {
            return AuthResult.ValidationError(emailError = "Email already registered")
        }

        return runCatching {
            val salt = generateSalt()
            val hash = hashPassword(password, salt)

            // Only one logged in user at a time
            userDao.clearLoggedIn()

            val entity = UserEntity(
                username = username.trim(),
                email = email.trim(),
                passwordHash = hash,
                salt = salt,
                createdAt = System.currentTimeMillis(),
                isLoggedIn = true
            )

            userDao.insertUser(entity)
            AuthResult.Success
        }.getOrElse { e ->
            AuthResult.ValidationError(
                generalError = "Failed to register: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    override suspend fun login(email: String, password: String): AuthResult {
        if (email.isBlank()) {
            return AuthResult.ValidationError(emailError = "Email is required")
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return AuthResult.ValidationError(emailError = "Invalid email")
        }
        if (password.isBlank()) {
            return AuthResult.ValidationError(passwordError = "Password is required")
        }

        val user = runCatching { userDao.getUserByEmail(email.trim()) }.getOrNull()
        if (user == null) {
            return AuthResult.ValidationError(emailError = "Account not found, please register")
        }

        val actualHash = hashPassword(password, user.salt)
        if (actualHash != user.passwordHash) {
            return AuthResult.ValidationError(passwordError = "Incorrect password")
        }

        return runCatching {
            userDao.clearLoggedIn()
            userDao.setLoggedIn(user.email)
            AuthResult.Success
        }.getOrElse { e ->
            AuthResult.ValidationError(
                generalError = "Failed to login: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    override suspend fun logout() {
        runCatching { userDao.clearLoggedIn() }
    }

    override suspend fun getLoggedInUser(): User? {
        val entity = runCatching { userDao.getLoggedInUser() }.getOrNull()
        return entity?.let { User(it.id, it.username, it.email) }
    }

    // Helpers

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = (password + salt).toByteArray(Charsets.UTF_8)
        val hash = md.digest(bytes)
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
}