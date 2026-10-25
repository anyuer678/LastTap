package com.lasttap.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ActivityEntity::class, RecordEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun recordDao(): RecordDao

    companion object {
        /** v1 -> v2：活动表新增连击合并窗口字段，老数据默认不合并。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE activities ADD COLUMN mergeWindowSeconds INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lasttap.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }

        /** 仅供测试：单测（Robolectric）之间需要隔离数据库状态。 */
        internal fun resetForTests() {
            synchronized(this) {
                instance?.close()
                instance = null
            }
        }
    }
}
