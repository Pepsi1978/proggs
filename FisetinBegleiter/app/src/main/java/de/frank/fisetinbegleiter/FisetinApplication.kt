package de.frank.fisetinbegleiter

import android.app.Application
import de.frank.fisetinbegleiter.alarm.AlarmScheduler
import de.frank.fisetinbegleiter.alarm.NotificationHelper
import de.frank.fisetinbegleiter.data.FisetinDatabase
import de.frank.fisetinbegleiter.data.FisetinRepository

class FisetinApplication : Application() {
    val database by lazy { FisetinDatabase.create(this) }
    val repository by lazy { FisetinRepository(database) }
    val alarmScheduler by lazy { AlarmScheduler(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}
