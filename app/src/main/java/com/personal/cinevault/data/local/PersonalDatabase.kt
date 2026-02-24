package com.personal.cinevault.data.local

import android.content.Context

/**
 * Stub for the main personal/user database (e.g. watchlist, ratings).
 * Replace with a real @Database Room class when ready.
 */
class PersonalDatabase private constructor() {

    companion object {
        fun create(context: Context): PersonalDatabase = PersonalDatabase()
    }
}
