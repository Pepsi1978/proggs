package de.frank.stacklabor.werftstudio.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.frank.stacklabor.werftstudio.data.local.dao.BestandDao
import de.frank.stacklabor.werftstudio.data.local.dao.BewertungDao
import de.frank.stacklabor.werftstudio.data.local.dao.KatalogDao
import de.frank.stacklabor.werftstudio.data.local.dao.StackDao
import de.frank.stacklabor.werftstudio.data.local.dao.ZielDao
import de.frank.stacklabor.werftstudio.data.local.entity.AlternationsPartnerEntity
import de.frank.stacklabor.werftstudio.data.local.entity.BewertungEntity
import de.frank.stacklabor.werftstudio.data.local.entity.BewertungszelleEntity
import de.frank.stacklabor.werftstudio.data.local.entity.EigeneFrageEntity
import de.frank.stacklabor.werftstudio.data.local.entity.FrageAntwortEntity
import de.frank.stacklabor.werftstudio.data.local.entity.KonkurrenzEntity
import de.frank.stacklabor.werftstudio.data.local.entity.MittelEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackEintragDosisEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackEintragEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackSortieransichtEntity
import de.frank.stacklabor.werftstudio.data.local.entity.StackZielEntity
import de.frank.stacklabor.werftstudio.data.local.entity.WirkstoffkomponenteEntity
import de.frank.stacklabor.werftstudio.data.local.entity.ZielEntity

@Database(
    entities = [
        MittelEntity::class,
        WirkstoffkomponenteEntity::class,
        StackEntity::class,
        StackEintragEntity::class,
        StackEintragDosisEntity::class,
        AlternationsPartnerEntity::class,
        ZielEntity::class,
        StackZielEntity::class,
        EigeneFrageEntity::class,
        BewertungEntity::class,
        BewertungszelleEntity::class,
        KonkurrenzEntity::class,
        FrageAntwortEntity::class,
        StackSortieransichtEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class StackLaborDatabase : RoomDatabase() {
    abstract fun katalogDao(): KatalogDao
    abstract fun stackDao(): StackDao
    abstract fun zielDao(): ZielDao
    abstract fun bewertungDao(): BewertungDao
    abstract fun bestandDao(): BestandDao

    companion object {
        const val DATEINAME: String = "stacklabor.db"

        @Volatile
        private var instance: StackLaborDatabase? = null

        fun getInstance(context: Context): StackLaborDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                StackLaborDatabase::class.java,
                DATEINAME,
            ).build().also { instance = it }
        }
    }
}
