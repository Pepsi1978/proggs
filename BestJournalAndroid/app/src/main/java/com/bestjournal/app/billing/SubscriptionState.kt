package com.bestjournal.app.billing

sealed class SubscriptionState {
    data object Free : SubscriptionState()
    data object Subscribed : SubscriptionState()
}

enum class SubscriptionType {
    NONE,
    MONTHLY,
    YEARLY,
    LIFETIME,
}
