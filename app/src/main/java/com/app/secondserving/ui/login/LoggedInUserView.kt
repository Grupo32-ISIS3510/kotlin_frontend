package com.app.secondserving.ui.login

/**
 * User details post authentication that is exposed to the UI
 */
data class LoggedInUserView(
    val displayName: String,
    val userId: String,
    val token: String,
    val email: String
)