package com.personal.cinevault.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.personal.cinevault.data.local.dao.MovieCacheDao
import com.personal.cinevault.data.local.entity.MovieCacheEntity

/**
 * Ephemeral cache database that stores serialised TMDB API responses.
 *
 * Schema export is deliberately disabled ([exportSchema] = false) because
 * cache data is disposable — it is never migrated and can always be rebuilt
 * by re-fetching from the network.
 *
 * Managed by [MovieCacheManager], which handles LRU eviction and TTL cleanup.
 */
@Database(
    entities = [MovieCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CacheDatabase : RoomDatabase() {

    abstract fun movieCacheDao(): MovieCacheDao

    companion object {

        const val DB_NAME = "cinevault_cache.db"

        @Volatile
        private var INSTANCE: CacheDatabase? = null

        /**
         * Return the singleton [CacheDatabase], creating it if needed.
         *
         * Thread-safe via double-checked locking with [INSTANCE] marked
         * [@Volatile].
         */
        fun getInstance(context: Context): CacheDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): CacheDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                CacheDatabase::class.java,
                DB_NAME
            )
                // Cache is disposable — recreate rather than migrate.
                .fallbackToDestructiveMigration()
                .build()
    }
}
