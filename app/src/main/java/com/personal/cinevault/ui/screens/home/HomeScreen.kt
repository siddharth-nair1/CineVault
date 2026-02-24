package com.personal.cinevault.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.personal.cinevault.domain.model.LogEntry
import com.personal.cinevault.domain.model.Movie
import org.koin.androidx.compose.koinViewModel

private const val TMDB_POSTER_W342 = "https://image.tmdb.org/t/p/w342"

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = koinViewModel()
) {
    val trending      by viewModel.trendingMovies.collectAsStateWithLifecycle()
    val recentlyLogged by viewModel.recentlyLogged.collectAsStateWithLifecycle()
    val isLoading     by viewModel.isLoading.collectAsStateWithLifecycle()
    val error         by viewModel.error.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        // ── Greeting header ───────────────────────────────────────────────────
        Text(
            text = "Your Vault",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Text(
            text = "What have you been watching?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(24.dp))

        // ── Trending row ──────────────────────────────────────────────────────
        SectionHeader(title = "Trending This Week")
        Spacer(Modifier.height(8.dp))

        when {
            isLoading -> Box(
                Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            error != null -> Column(
                Modifier.fillMaxWidth().height(200.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Couldn't load trending", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = viewModel::retry) { Text("Retry") }
            }

            trending.isEmpty() -> Box(
                Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) { Text("No trending movies right now") }

            else -> LazyRow(
                contentPadding      = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trending, key = { it.id }) { movie ->
                    TrendingPosterCard(
                        movie   = movie,
                        onClick = { navController.navigate("movie/${movie.id}") }
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── Recently Watched ──────────────────────────────────────────────────
        SectionHeader(title = "Recently Watched")
        Spacer(Modifier.height(8.dp))

        if (recentlyLogged.isEmpty()) {
            RecentlyWatchedEmpty(
                onBrowse = { navController.navigate("search") }
            )
        } else {
            LazyRow(
                contentPadding      = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recentlyLogged, key = { it.id }) { entry ->
                    TrendingPosterCard(
                        movie   = entry.movie,
                        onClick = { navController.navigate("movie/${entry.movie.id}") }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Section header ─────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

// ── Trending poster card ───────────────────────────────────────────────────────
@Composable
private fun TrendingPosterCard(movie: Movie, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = movie.posterPath?.let { "$TMDB_POSTER_W342$it" },
                contentDescription = "${movie.title} poster",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 120.dp, height = 180.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )
            Text(
                text = movie.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                modifier = Modifier.padding(6.dp)
            )
        }
    }
}

// ── Empty state for recently watched ──────────────────────────────────────────
@Composable
private fun RecentlyWatchedEmpty(onBrowse: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No films logged yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onBrowse) {
            Text("Browse films to get started →")
        }
    }
}
