# Add project specific ProGuard rules here.
-keepattributes *Annotation*

# Moshi
-keep class **JsonAdapter
-keepclassmembers class ** {
    @com.squareup.moshi.Json <fields>;
}

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# WireGuard
-keep class com.wireguard.** { *; }
