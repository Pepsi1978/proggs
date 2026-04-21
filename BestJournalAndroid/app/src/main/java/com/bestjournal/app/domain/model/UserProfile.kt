package com.bestjournal.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class UserProfile(
    val displayName: String,
    val email: String,
    val avatarUrl: String? = null,
    val lastSyncTimestamp: Long? = null,
    val isSignedIn: Boolean = false
)
