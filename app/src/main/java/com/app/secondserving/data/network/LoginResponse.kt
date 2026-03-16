package com.app.secondserving.data.network

data class TokenResponse(
    val access_token: String,
    val token_type: String,
    val user: UserResponse
)

data class UserResponse(
    val id: String,
    val email: String,
    val full_name: String,
    val is_premium: Boolean,
    val location: String?,
    val created_at: String
)