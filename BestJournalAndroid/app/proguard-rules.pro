# Moshi
-keep class com.bestjournal.app.data.remote.** { *; }
-keepclassmembers class com.bestjournal.app.data.remote.** { *; }

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Google Drive API + HTTP Client + JSON
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.http.client.** { *; }
-keep class com.google.api.services.** { *; }
-dontwarn com.google.api.client.**
-dontwarn com.google.http.client.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Google Sign-In / Credential Manager
-keep class com.google.android.libraries.identity.googleid.** { *; }
-keep class androidx.credentials.** { *; }

# Sherpa-ONNX (local Whisper — JNI, fields accessed from native C++)
-keep class com.k2fsa.sherpa.onnx.** { *; }

# Moshi (reflection-based adapter factory)
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Google Play Billing (AIDL)
-keep class com.android.billingclient.** { *; }
-keep class com.android.vending.billing.** { *; }

# Security Crypto (EncryptedSharedPreferences)
-keep class androidx.security.crypto.** { *; }

# Biometric
-keep class androidx.biometric.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Google Play In-App Review (IPC interfaces used by ReviewManager)
-keep class com.google.android.play.core.review.** { *; }
-keep class com.google.android.play.core.tasks.** { *; }

# Gson (used by Google API client for JSON parsing)
-keep class com.google.gson.** { *; }
