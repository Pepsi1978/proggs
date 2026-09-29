package de.frank.longevity

import android.app.Application
import de.frank.longevity.ki.KiDienst

class LongevityApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KiDienst.kanalAnlegen(this)
    }
}
