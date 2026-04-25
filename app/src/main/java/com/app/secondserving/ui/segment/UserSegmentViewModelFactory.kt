package com.app.secondserving.ui.segment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.app.secondserving.data.AnalyticsRepository

class UserSegmentViewModelFactory(
    private val repository: AnalyticsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserSegmentViewModel::class.java)) {
            return UserSegmentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
