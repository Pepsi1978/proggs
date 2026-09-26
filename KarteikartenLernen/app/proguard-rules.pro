-keep class de.frank.karteikartenlernen.data.** { *; }
# Tink (security-crypto) referenziert Errorprone-Annotationen, die zur Laufzeit nicht gebraucht werden.
-dontwarn com.google.errorprone.annotations.**
