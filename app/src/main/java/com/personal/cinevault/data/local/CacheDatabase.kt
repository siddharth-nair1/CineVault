package com.personal.cinevault.data.local

import android.content.Context

/**
 * Stub for the cache database (e.g. cached API responses).
 * Replace with a real @Database Room class when ready.
 */
class CacheDatabase private constructor() {

    companion object {
        fun create(context: Context): CacheDatabase = CacheDatabase()
    }
}
