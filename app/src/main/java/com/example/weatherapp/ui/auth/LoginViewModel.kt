package com.example.weatherapp.ui.auth


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.domain.auth.AuthRepository
import com.example.weatherapp.domain.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

   /* init {
        // Auto-login if someone is already logged in
        viewModelScope.launch {
            val user = authRepository.getLoggedInUser()
            if (user != null) {
                _uiState.update { it.copy(loginSuccess = true) }
            }
        }
    }*/

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null, generalError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null, generalError = null) }
    }

    fun onLoginClick() {
        val current = _uiState.value
        if (current.isLoading) return

        _uiState.update { it.copy(isLoading = true, generalError = null, loginSuccess = false) }

        viewModelScope.launch {
            val result = authRepository.login(
                email = _uiState.value.email,
                password = _uiState.value.password
            )

            when (result) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                }
                is AuthResult.ValidationError -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            emailError = result.emailError,
                            passwordError = result.passwordError,
                            generalError = result.generalError,
                            loginSuccess = false
                        )
                    }
                }
            }
        }
    }

    fun consumeLoginSuccess() {
        _uiState.update { it.copy(loginSuccess = false) }
    }
}
