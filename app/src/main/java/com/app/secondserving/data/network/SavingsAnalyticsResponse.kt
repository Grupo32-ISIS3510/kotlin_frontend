package com.app.secondserving.data.network

data class SavingsAnalyticsResponse(
    val saved_cop: Double,
    val wasted_cop: Double,
    val period: String
)
