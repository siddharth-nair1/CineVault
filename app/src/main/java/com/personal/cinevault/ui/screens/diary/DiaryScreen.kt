package com.personal.cinevault.ui.screens.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.personal.cinevault.domain.model.LogEntry
import com.personal.cinevault.ui.components.HalfStarRating
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onMovieClick: (tmdbMovieId: Int) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: DiaryViewModel = koinViewModel()
) {
    val items by viewModel.logs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Diary",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onSearchClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search for a film to log",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->

        if (items.isEmpty()) {
            EmptyDiary(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items(
                items = items,
                key = { item ->
                    when (item) {
                        is DiaryListItem.Header -> "header_${item.label}"
                        is DiaryListItem.Entry  -> "entry_${item.log.id}_${item.log.tmdbMovieIdKey}"
                    }
                },
                contentType = { item ->
                    when (item) {
                        is DiaryListItem.Header -> "header"
                        is DiaryListItem.Entry  -> "entry"
                    }
                }
            ) { item ->
                when (item) {
                    is DiaryListItem.Header -> MonthHeader(label = item.label)
                    is DiaryListItem.Entry  -> LogEntryRow(
                        entry = item.log,
                        onClick = { onMovieClick(item.log.movie.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(88.dp)) }  // FAB clearance
        }
    }
}

// ── Composable helpers ────────────────────────────────────────────────────────

@Composable
private fun MonthHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LogEntryRow(
    entry: LogEntry,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Poster thumbnail
        AsyncImage(
            model = entry.movie.posterPath
                ?.let { "https://image.tmdb.org/t/p/w92$it" },
            contentDescription = entry.movie.title,
            modifier = Modifier
                .width(44.dp)
                .height(66.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )

        // Title + metadata
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = entry.movie.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Formatted watch date
            val displayDate = entry.watchedDate
                ?.takeIf { it.length >= 10 }
                ?.let { iso ->
                    runCatching {
                        java.time.LocalDate.parse(iso)
                            .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
                    }.getOrElse { iso }
                }
                ?: "Unknown date"

            Text(
                text = displayDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Star rating (read-only, display-scale: divide stored 0-10 by 2)
            if (entry.rating != null && entry.rating > 0f) {
                HalfStarRating(
                    rating = entry.rating / 2f,       // storage 0-10 → display 0-5
                    onRatingChange = {},
                    starSize = 14.dp,
                    activeColor = MaterialTheme.colorScheme.primary,
                    readOnly = true
                )
            }
        }

        // Liked heart icon
        if (entry.liked) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = "Liked",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EmptyDiary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📽️",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Your diary is empty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tap the search button to find a film to log",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Key helper ────────────────────────────────────────────────────────────────

/** Stable unique key for LazyColumn derived from both entity id and tmdb id. */
private val LogEntry.tmdbMovieIdKey get() = movie.id
