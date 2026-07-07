package com.nems.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nems.app.data.local.dao.StackSectionDao
import com.nems.app.data.local.dao.SupplementDao
import com.nems.app.data.local.dao.SupplementEntryDao
import com.nems.app.data.local.entity.StackSectionEntity
import com.nems.app.data.local.entity.SupplementEntity
import com.nems.app.data.local.entity.SupplementEntryEntity

@Database(
    entities = [
        SupplementEntity::class,
        StackSectionEntity::class,
        SupplementEntryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NmsDatabase : RoomDatabase() {
    abstract fun supplementDao(): SupplementDao
    abstract fun stackSectionDao(): StackSectionDao
    abstract fun supplementEntryDao(): SupplementEntryDao
}
