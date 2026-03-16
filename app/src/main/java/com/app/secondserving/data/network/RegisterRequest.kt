package com.app.secondserving.data.network

data class RegisterRequest(
    val email: String,
    val full_name: String,
    val password: String
)
