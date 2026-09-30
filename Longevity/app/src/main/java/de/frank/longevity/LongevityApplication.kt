package de.frank.longevity

import android.app.Application
import de.frank.longevity.ki.KiDienst
import de.frank.longevity.ki.KiLog

class LongevityApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        KiLog.init(this)
        KiDienst.kanalAnlegen(this)
    }
}
