# Almanach android-build §7: keep-Regeln eng halten, aber alles behalten, was per Reflexion
# oder ueber generierte Namen angefasst wird. Nie eine Klasse loeschen, um einen R8-Fehler
# "wegzumachen" - das nimmt der App Funktionalitaet.
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Room-Generate + Entities werden reflektiv angefasst.
-keep class de.frank.claudekompass.data.local.** { *; }
-keep class de.frank.claudekompass.data.model.** { *; }

# OkHttp/Okio bringen eigene Regeln mit; die folgenden Warnungen sind bekannt und harmlos.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Tink (steckt in androidx.security-crypto) verweist auf Annotationen von ErrorProne, die
# ausschliesslich beim Uebersetzen existieren und zur Laufzeit nicht gebraucht werden.
# Almanach android-build §7.4: gezielt fuer dieses eine Paket, kein pauschales -dontwarn ueber
# alles - sonst verschwinden auch echte fehlende Klassen aus der Meldung.
-dontwarn com.google.errorprone.annotations.**
