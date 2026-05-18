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

# Hilt / Dagger (PS-001: R8 activation — 2026-05-18)
# AGP+KSP auto-generates some rules, but explicit keeps are required for:
#   - @AndroidEntryPoint classes (Activity, Fragment, Service, BroadcastReceiver)
#   - @HiltViewModel annotated ViewModels
#   - Hilt internal component infrastructure
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @androidx.hilt.navigation.compose.HiltViewModelFactory class * { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.lifecycle.HiltViewModel <init>(...);
}
-dontwarn dagger.hilt.**
-dontwarn javax.inject.**

# Kotlin Coroutines (required for reflection-based coroutine context)
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Kotlin serialization / stdlib reflection
-dontwarn kotlin.reflect.jvm.internal.**

# --- DEFENSIVE R8 ADDITIONS (2026-04-18) ---
# Pure additions: -keep rules never break functionality, they only prevent
# shrinking/obfuscation. APK grows marginally (~50-200 KB).

# Kotlin Metadata (required for reflection-based Moshi fallback via
# KotlinJsonAdapterFactory in NetworkModule.kt)
-keep class kotlin.Metadata { *; }
-keepclasseswithmembers class * { @kotlin.Metadata *; }

# Moshi internal utilities (used by KotlinJsonAdapterFactory reflection path)
-keep class com.squareup.moshi.internal.Util { *; }

# Firebase AI content types (Gemini response deserialization: TextPart, etc.)
-keep class com.google.firebase.ai.type.** { *; }

# Native JNI methods (Sherpa-ONNX — belt-and-suspenders; proguard-android-optimize
# already includes this by default, but explicit is safer across AGP versions)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Application & BroadcastReceivers (AGP normally keeps manifest-referenced
# components automatically, but explicit keeps protect against edge cases)
-keep class com.bestjournal.app.BestJournalApp { *; }
-keep class com.bestjournal.app.util.ReminderReceiver { *; }
-keep class com.bestjournal.app.util.WeeklyReviewReceiver { *; }
-keep class com.bestjournal.app.util.MonthlyReviewReceiver { *; }
-keep class com.bestjournal.app.util.YearlyReviewReceiver { *; }
-keep class com.bestjournal.app.util.BootReminderReceiver { *; }

# R8 missing class warnings — Apache HTTP Client transitive dependencies
# (javax.naming and org.ietf.jgss are not available on Android — these classes
# are only used by code paths that Android never executes. Suppressing the
# warnings is the standard fix recommended by AGP itself in missing_rules.txt.)
-dontwarn javax.naming.InvalidNameException
-dontwarn javax.naming.NamingException
-dontwarn javax.naming.directory.Attribute
-dontwarn javax.naming.directory.Attributes
-dontwarn javax.naming.ldap.LdapName
-dontwarn javax.naming.ldap.Rdn
-dontwarn org.ietf.jgss.GSSContext
-dontwarn org.ietf.jgss.GSSCredential
-dontwarn org.ietf.jgss.GSSException
-dontwarn org.ietf.jgss.GSSManager
-dontwarn org.ietf.jgss.GSSName
-dontwarn org.ietf.jgss.Oid
