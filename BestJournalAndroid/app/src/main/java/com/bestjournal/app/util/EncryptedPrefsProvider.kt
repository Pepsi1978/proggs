package com.bestjournal.app.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Central cached accessor for the encrypted SharedPreferences instance. Prevents repeated
 * Keystore-IO on the main thread (ANR risk). The first call on process start pays the cost; every
 * subsequent call returns the cached singleton.
 *
 * For ViewModels/Singletons inject SharedPreferences via Hilt (AppModule already provides it). This
 * provider is for Composables, BroadcastReceivers and other places where Hilt-injection is awkward.
 */
object EncryptedPrefsProvider {
    @Volatile private var cached: SharedPreferences? = null
    private val lock = Any()

    fun get(context: Context): SharedPreferences {
        cached?.let {
            return it
        }
        return synchronized(lock) {
            cached
                ?: run {
                    val mk = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                    EncryptedSharedPreferences.create(
                            Constants.ENCRYPTED_PREFS_NAME,
                            mk,
                            context.applicationContext,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                        )
                        .also { cached = it }
                }
        }
    }
}
