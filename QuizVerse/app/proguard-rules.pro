# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.quizverse.app.data.** { *; }

# Google Ads
-keep class com.google.android.gms.ads.** { *; }

# Compose
-dontwarn androidx.compose.**
