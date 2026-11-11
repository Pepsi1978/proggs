package de.frank.jarvis.faehigkeit

import android.content.ContentProvider
import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Process

/**
 * Nur Apps mit Franks Signierschlüssel kommen an Jarvis' Schnittstellen. Die Prüfung steht im Code und nicht als
 * `android:permission` im Manifest: Die geteilte Erlaubnis `de.frank.jarvis.permission.BRUECKE` gehört im System der App,
 * die sie zuerst deklariert hat, und verschwindet mit deren Deinstallation; dann wären alle Brücken zu
 * (11.11.2026, Entropy Reducer; Almanach `bugs/android/android-platform.md` §2.12).
 */
object BrueckenSchutz {
    /** Text der [SecurityException] bei fremdem Schlüssel; [AppBruecke] erkennt die Ursache daran auch bei Partner-Apps. */
    const val FREMDER_SCHLUESSEL = "Nur Apps mit demselben Signierschlüssel."

    fun eigeneSignatur(context: Context, uid: Int): Boolean =
        uid == Process.myUid() || context.packageManager.checkSignatures(uid, Process.myUid()) == PackageManager.SIGNATURE_MATCH
}

/** Am Anfang jedes Provider-Einstiegs mit Daten: wirft [SecurityException], wenn der Aufrufer einen anderen Schlüssel trägt. */
fun ContentProvider.nurEigeneSignatur() {
    val ctx = context ?: throw SecurityException("App nicht bereit.")
    if (!BrueckenSchutz.eigeneSignatur(ctx, Binder.getCallingUid())) throw SecurityException(BrueckenSchutz.FREMDER_SCHLUESSEL)
}
