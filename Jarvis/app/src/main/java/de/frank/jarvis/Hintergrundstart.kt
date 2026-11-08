package de.frank.jarvis

import android.content.Context
import android.os.Build
import android.provider.Settings

/** Darf Jarvis eine Activity öffnen? Aus dem Hintergrund nur mit „Über anderen Apps einblenden“ (ab Android 10). */
object Hintergrundstart {
    fun ueberlagernErlaubt(context: Context): Boolean = Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(context)

    fun darf(context: Context): Boolean = ueberlagernErlaubt(context) || MainActivity.sichtbar
}
