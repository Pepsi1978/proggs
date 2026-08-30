-keep class androidx.room.** { *; }
-dontwarn org.conscrypt.**

# Tink (in security-crypto) verweist auf Annotationen von ErrorProne, die nur beim
# Übersetzen gebraucht werden und zur Laufzeit fehlen dürfen. Sie kamen bisher als
# Beifang über play-services-auth herein; seit die Drive-Anmeldung raus ist, fehlen sie.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
