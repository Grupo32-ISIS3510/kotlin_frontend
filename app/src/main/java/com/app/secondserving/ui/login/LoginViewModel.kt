package com.app.secondserving.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Patterns
import com.app.secondserving.data.LoginRepository
import com.app.secondserving.data.Result
import kotlinx.coroutines.launch

import com.app.secondserving.R

class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {
    companion object {
        const val MAX_FULL_NAME_LENGTH = 60
    }

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val result = loginRepository.login(username, password)
            if (result is Result.Success) {
                _loginResult.value = LoginResult(success = result.data.toView())
            } else if (result is Result.Error) {
                val errorMessage = result.exception.message ?: getStringFromRes(R.string.login_failed)
                _loginResult.value = LoginResult(error = errorMessage)
            }
        }
    }

    fun register(email: String, fullName: String, password: String) {
        viewModelScope.launch {
            val result = loginRepository.register(email, fullName, password)
            if (result is Result.Success) {
                _loginResult.value = LoginResult(success = result.data.toView())
            } else if (result is Result.Error) {
                val errorMessage = result.exception.message ?: getStringFromRes(R.string.register_failed)
                if (isEmailConflictError(errorMessage)) {
                    _loginResult.value = LoginResult(emailError = errorMessage)
                } else {
                    _loginResult.value = LoginResult(error = errorMessage)
                }
            }
        }
    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    fun registerDataChanged(email: String, fullName: String, password: String) {
        if (!isUserNameValid(email)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (fullName.isBlank()) {
            _loginForm.value = LoginFormState(nameError = R.string.invalid_name)
        } else if (fullName.length > MAX_FULL_NAME_LENGTH) {
            _loginForm.value = LoginFormState(nameError = R.string.invalid_name_length)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    private fun isUserNameValid(username: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(username).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }

    private fun getStringFromRes(resId: Int): String {
        return when (resId) {
            R.string.login_failed -> "Login failed. Please check your credentials."
            R.string.register_failed -> "Registration failed. Please try again."
            else -> "Unexpected error"
        }
    }

    private fun isEmailConflictError(message: String): Boolean {
        val normalized = message.lowercase()
        val mentionsEmail = normalized.contains("email") || normalized.contains("correo")
        val isAlreadyUsed = normalized.contains("existe") ||
            normalized.contains("registrad") ||
            normalized.contains("already")
        return mentionsEmail && isAlreadyUsed
    }

    private fun com.app.secondserving.data.model.LoggedInUser.toView() = LoggedInUserView(
        displayName = displayName,
        userId = userId,
        token = token,
        email = email
    )
}