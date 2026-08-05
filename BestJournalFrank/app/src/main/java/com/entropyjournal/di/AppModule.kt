package com.entropyjournal.di

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.entropyjournal.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.KeyStore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val TAG = "AppModule"

    /** Keystore alias that MasterKeys.AES256_GCM_SPEC creates and reuses. */
    private const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences = try {
        createEncryptedPrefs(context)
    } catch (e: Exception) {
        // The keyset cannot be decrypted with this device's master key. Happens after a
        // device-to-device transfer or a Keystore reset: the prefs file travels with the
        // backup, the hardware-bound master key does not. The old values are unrecoverable
        // either way, so drop the unreadable keyset and start a fresh encrypted store
        // instead of crashing in Activity.onCreate().
        Log.w(TAG, "Encrypted prefs unreadable, recreating from scratch", e)
        resetEncryptedPrefs(context)
        createEncryptedPrefs(context)
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            Constants.ENCRYPTED_PREFS_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun resetEncryptedPrefs(context: Context) {
        context.deleteSharedPreferences(Constants.ENCRYPTED_PREFS_NAME)
        try {
            KeyStore.getInstance("AndroidKeyStore")
                .apply { load(null) }
                .deleteEntry(MASTER_KEY_ALIAS)
        } catch (e: Exception) {
            Log.w(TAG, "Could not delete master key, a new keyset is still created", e)
        }
    }
}
