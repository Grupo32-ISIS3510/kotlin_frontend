package com.app.secondserving.ui.login

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.app.secondserving.MainActivity
import com.app.secondserving.R
import com.app.secondserving.SecondServingApp
import com.app.secondserving.data.SessionManager
import com.app.secondserving.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sessionManager: SessionManager

    // Vistas cacheadas como non-null en onCreate
    private lateinit var tabLoginContainer: LinearLayout
    private lateinit var tabSignupContainer: LinearLayout
    private lateinit var tabLoginText: TextView
    private lateinit var tabLoginIndicator: View
    private lateinit var tabSignupText: TextView
    private lateinit var tabSignupIndicator: View
    private lateinit var labelFullName: TextView
    private lateinit var fullNameField: EditText
    private lateinit var fullNameUnderline: View
    private lateinit var forgotPassword: TextView
    private lateinit var loginButton: Button
    private lateinit var loadingBar: ProgressBar
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var authErrorText: TextView
    private var emailUnderline: View? = null
    private var emailServerErrorText: TextView? = null

    private var isRegisterMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar sesión activa ANTES de inflar la UI
        sessionManager = SessionManager(this)
        if (sessionManager.isSessionValid()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Cachear vistas evitando nullables de ViewBinding
        tabLoginContainer  = requireNotNull(binding.tabLoginContainer)
        tabSignupContainer = requireNotNull(binding.tabSignupContainer)
        tabLoginText       = requireNotNull(binding.tabLogin)
        tabLoginIndicator  = requireNotNull(binding.tabLoginIndicator)
        tabSignupText      = requireNotNull(binding.tabSignup)
        tabSignupIndicator = requireNotNull(binding.tabSignupIndicator)
        labelFullName      = requireNotNull(binding.labelFullName)
        fullNameField      = requireNotNull(binding.fullName)
        fullNameUnderline  = requireNotNull(binding.fullNameUnderline)
        forgotPassword     = requireNotNull(binding.forgotPassword)
        loginButton        = binding.login
        loadingBar         = binding.loading
        emailField         = binding.username
        passwordField      = binding.password
        authErrorText      = binding.authErrorText
        emailUnderline     = binding.root.findViewById(R.id.emailUnderline)
        emailServerErrorText = binding.root.findViewById(R.id.emailServerErrorText)

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        fullNameField.filters = arrayOf(InputFilter.LengthFilter(LoginViewModel.MAX_FULL_NAME_LENGTH))
        setupTabSwitching()
        setupFormObservers()
        setupInputListeners()
        setupActionButton()
    }

    private fun setupTabSwitching() {
        tabLoginContainer.setOnClickListener { switchToLoginMode() }
        tabSignupContainer.setOnClickListener { switchToRegisterMode() }
    }

    private fun switchToLoginMode() {
        if (isRegisterMode) {
            isRegisterMode = false
            tabLoginText.setTextColor(getColor(R.color.text_dark))
            tabLoginText.setTypeface(null, Typeface.BOLD)
            tabLoginIndicator.setBackgroundColor(getColor(R.color.orange_primary))
            tabSignupText.setTextColor(getColor(R.color.text_gray))
            tabSignupText.setTypeface(null, Typeface.NORMAL)
            tabSignupIndicator.setBackgroundColor(Color.TRANSPARENT)

            labelFullName.visibility = View.GONE
            fullNameField.visibility = View.GONE
            fullNameUnderline.visibility = View.GONE
            forgotPassword.visibility = View.VISIBLE
            loginButton.text = getString(R.string.action_sign_in)
            clearAuthError()
            clearEmailBackendError()

            loginViewModel.loginDataChanged(
                emailField.text.toString(),
                passwordField.text.toString()
            )
        }
    }

    private fun switchToRegisterMode() {
        if (!isRegisterMode) {
            isRegisterMode = true
            tabSignupText.setTextColor(getColor(R.color.text_dark))
            tabSignupText.setTypeface(null, Typeface.BOLD)
            tabSignupIndicator.setBackgroundColor(getColor(R.color.orange_primary))
            tabLoginText.setTextColor(getColor(R.color.text_gray))
            tabLoginText.setTypeface(null, Typeface.NORMAL)
            tabLoginIndicator.setBackgroundColor(Color.TRANSPARENT)

            labelFullName.visibility = View.VISIBLE
            fullNameField.visibility = View.VISIBLE
            fullNameUnderline.visibility = View.VISIBLE
            forgotPassword.visibility = View.GONE
            loginButton.text = getString(R.string.action_register)
            clearAuthError()
            clearEmailBackendError()

            loginViewModel.registerDataChanged(
                emailField.text.toString(),
                fullNameField.text.toString(),
                passwordField.text.toString()
            )
        }
    }

    private fun setupFormObservers() {
        loginViewModel.loginFormState.observe(this, Observer {
            val state = it ?: return@Observer
            loginButton.isEnabled = state.isDataValid
            emailField.error = if (state.usernameError != null) getString(state.usernameError) else null
            passwordField.error = if (state.passwordError != null) getString(state.passwordError) else null
            fullNameField.error = if (state.nameError != null) getString(state.nameError) else null
            if (state.usernameError != null) {
                clearEmailBackendError()
            }
        })

        loginViewModel.loginResult.observe(this, Observer {
            val result = it ?: return@Observer
            loadingBar.visibility = View.GONE
            clearEmailBackendError()
            if (result.error != null) {
                showError(result.error)
            }
            if (result.emailError != null) {
                showEmailFieldError(result.emailError)
            }
            if (result.success != null) {
                sessionManager.saveSession(
                    token = result.success.token,
                    fullName = result.success.displayName,
                    email = result.success.email,
                    userId = result.success.userId
                )
                clearAuthError()

                (application as SecondServingApp).registerFcmTokenIfLoggedIn()

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        })
    }

    private fun setupInputListeners() {
        emailField.afterTextChanged { notifyFormChanged() }
        fullNameField.afterTextChanged { notifyFormChanged() }
        passwordField.apply {
            afterTextChanged { notifyFormChanged() }
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) submitForm()
                false
            }
        }
    }

    private fun setupActionButton() {
        loginButton.setOnClickListener {
            loadingBar.visibility = View.VISIBLE
            clearAuthError()
            clearEmailBackendError()
            submitForm()
        }
    }

    private fun notifyFormChanged() {
        clearAuthError()
        clearEmailBackendError()
        val email = emailField.text.toString()
        val password = passwordField.text.toString()
        if (isRegisterMode) {
            loginViewModel.registerDataChanged(email, fullNameField.text.toString(), password)
        } else {
            loginViewModel.loginDataChanged(email, password)
        }
    }

    private fun submitForm() {
        val email = emailField.text.toString()
        val password = passwordField.text.toString()
        if (isRegisterMode) {
            loginViewModel.register(email, fullNameField.text.toString(), password)
        } else {
            loginViewModel.login(email, password)
        }
    }

    private fun showError(errorMessage: String) {
        authErrorText.text = errorMessage
        authErrorText.visibility = View.VISIBLE
        authErrorText.setTextColor(getColor(R.color.error_red))
    }

    private fun showEmailFieldError(errorMessage: String) {
        authErrorText.visibility = View.GONE
        emailServerErrorText?.text = errorMessage
        emailServerErrorText?.visibility = View.VISIBLE
        emailUnderline?.setBackgroundColor(getColor(R.color.error_red))
    }

    private fun clearEmailBackendError() {
        emailServerErrorText?.visibility = View.GONE
        emailServerErrorText?.text = ""
        emailUnderline?.setBackgroundColor(getColor(R.color.divider))
    }

    private fun clearAuthError() {
        authErrorText.visibility = View.GONE
        authErrorText.text = ""
    }
}

fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}
