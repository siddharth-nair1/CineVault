package com.personal.cinevault.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("home",      "Home",      Icons.Default.Home),
    BottomNavItem("search",    "Films",     Icons.Default.Search),
    BottomNavItem("diary",     "Diary",     Icons.Default.Book),
    BottomNavItem("watchlist", "Watchlist", Icons.Default.Bookmarks),
    BottomNavItem("profile",   "Profile",   Icons.Default.Person)
)
