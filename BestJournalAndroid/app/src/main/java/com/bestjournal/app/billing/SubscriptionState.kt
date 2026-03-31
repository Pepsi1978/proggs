package com.bestjournal.app.billing

sealed class SubscriptionState {
    data object Free : SubscriptionState()
    data object Subscribed : SubscriptionState()
    data object Expired : SubscriptionState()
}
