package sa.factory.turbineindustriali.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [TurbineIndustrialEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun industrial_iotDao(): TurbineIndustrialDao
}
