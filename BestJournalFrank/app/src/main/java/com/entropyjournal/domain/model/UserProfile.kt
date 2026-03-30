package com.entropyjournal.domain.model

data class UserProfile(
    val displayName: String,
    val email: String,
    val avatarUrl: String? = null,
    val lastSyncTimestamp: Long? = null,
    val isSignedIn: Boolean = false
)
