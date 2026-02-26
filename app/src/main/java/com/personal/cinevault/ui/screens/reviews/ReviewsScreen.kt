package com.personal.cinevault.ui.screens.reviews

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
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.personal.cinevault.domain.model.Review
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w185"
private val DATE_FORMATTER = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    navController: NavController,
    viewModel: ReviewsViewModel = koinViewModel()
) {
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("My Reviews") },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        if (reviews.isEmpty()) {
            ReviewsEmptyState(Modifier.padding(padding))
        } else {
            LazyColumn(
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(reviews, key = { it.id }) { review ->
                    ReviewCard(
                        review = review,
                        onClick = { navController.navigate("review/${review.id}") }
                    )
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

// ── Review Card ────────────────────────────────────────────────────────────────

@Composable
private fun ReviewCard(
    review: Review,
    onClick: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // ── Poster ─────────────────────────────────────────────────────────
            AsyncImage(
                model = review.posterPath?.let { "$TMDB_IMAGE_BASE$it" },
                contentDescription = "${review.movieTitle} poster",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 64.dp, height = 96.dp)
                    .clip(RoundedCornerShape(10.dp))
            )

            Spacer(Modifier.width(14.dp))

            // ── Right side ─────────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {

                // Movie title
                Text(
                    text = review.movieTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                // Star rating + spoiler badge row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    review.rating?.let { rating ->
                        StarRating(rating = rating)
                    }
                    if (review.containsSpoilers) {
                        SpoilerBadge()
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Review text snippet
                Text(
                    text = review.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis
                )

                // "Read more" / "Show less" toggle — only shown when content is long enough
                if (review.content.length > 120) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.padding(top = 0.dp)
                    ) {
                        Text(
                            text = if (expanded) "Show less" else "Read more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Creation date
                Text(
                    text = DATE_FORMATTER.format(Date(review.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// ── Star Rating ────────────────────────────────────────────────────────────────

@Composable
private fun StarRating(rating: Float) {
    val stars = buildString {
        val full = (rating / 2).toInt().coerceIn(0, 5)
        val hasHalf = (rating / 2) - full >= 0.5f
        repeat(full) { append('★') }
        if (hasHalf && full < 5) append('½')
        val empty = 5 - full - if (hasHalf && full < 5) 1 else 0
        repeat(empty) { append('☆') }
    }
    Text(
        text = stars,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

// ── Spoiler Badge ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpoilerBadge() {
    Badge(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Text(
            text = "SPOILERS",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

// ── Empty State ────────────────────────────────────────────────────────────────

@Composable
private fun ReviewsEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 40.dp)
        ) {
            Text("✍️", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No reviews yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Your written reviews for movies will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
