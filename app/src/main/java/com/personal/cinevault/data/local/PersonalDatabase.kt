package com.personal.cinevault.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.personal.cinevault.data.local.dao.LogEntryDao
import com.personal.cinevault.data.local.dao.ReviewDao
import com.personal.cinevault.data.local.dao.WatchlistDao
import com.personal.cinevault.data.local.entity.CineListEntity
import com.personal.cinevault.data.local.entity.ListMovieEntity
import com.personal.cinevault.data.local.entity.LogEntryEntity
import com.personal.cinevault.data.local.entity.ReviewEntity
import com.personal.cinevault.data.local.entity.WatchlistEntity

/**
 * Main Room database that stores all user-generated personal data:
 * watch logs, reviews, watchlist, and custom lists.
 *
 * Schema is exported to `schemas/` for migration tracking.
 * Increment [version] and provide a [androidx.room.migration.Migration]
 * whenever the schema changes.
 */
@Database(
    entities = [
        LogEntryEntity::class,
        ReviewEntity::class,
        WatchlistEntity::class,
        CineListEntity::class,
        ListMovieEntity::class,
    ],
    version = 1,
    exportSchema = true
)
abstract class PersonalDatabase : RoomDatabase() {

    abstract fun logEntryDao(): LogEntryDao
    abstract fun reviewDao(): ReviewDao
    abstract fun watchlistDao(): WatchlistDao

    companion object {

        const val DB_NAME = "cinevault_personal.db"

        @Volatile
        private var INSTANCE: PersonalDatabase? = null

        /**
         * Return the singleton [PersonalDatabase], creating it if needed.
         *
         * Thread-safe via double-checked locking with [INSTANCE] marked
         * [@Volatile].
         */
        fun getInstance(context: Context): PersonalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): PersonalDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                PersonalDatabase::class.java,
                DB_NAME
            )
                // Add migrations here as the schema evolves:
                // .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
    }
}
