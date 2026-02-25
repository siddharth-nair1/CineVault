package com.personal.cinevault.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A 5-star rating bar that supports half-star increments (0.5 … 5.0).
 *
 * Tap detection distinguishes the left half (half-star) from the right half
 * (full star) of each icon using [detectTapGestures] on the star's measured
 * pixel width. Tapping the currently selected value clears the rating to 0.
 *
 * @param rating         Current rating on a 0.5–5.0 scale. 0.0 means unrated.
 * @param onRatingChange Callback with the new rating value, or 0f if cleared.
 * @param modifier       Applied to the outer [Row].
 * @param starCount      Total number of stars (default 5).
 * @param starSize       Width and height of each star icon (default 36.dp).
 * @param activeColor    Colour used for filled and half-filled stars.
 * @param inactiveColor  Colour used for empty (outline) stars.
 * @param readOnly       When true, disables tap gestures (display-only mode).
 */
@Composable
fun HalfStarRating(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    starCount: Int = 5,
    starSize: Dp = 36.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
    readOnly: Boolean = false
) {
    val ratingDescription = if (rating == 0f) "Unrated" else "${"%.1f".format(rating)} out of 5 stars"

    Row(
        modifier = modifier.semantics {
            contentDescription = "Star rating: $ratingDescription"
        },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(starCount) { starIndex ->
            // The pixel width of this star icon, measured after layout.
            var starWidthPx by remember { mutableFloatStateOf(0f) }

            val halfStarValue = starIndex + 0.5f
            val fullStarValue = starIndex + 1.0f

            val icon = when {
                rating >= fullStarValue  -> Icons.Filled.Star
                rating >= halfStarValue  -> Icons.Filled.StarHalf
                else                     -> Icons.Outlined.StarOutline
            }
            val tint = when {
                rating >= halfStarValue  -> activeColor
                else                     -> inactiveColor
            }

            val starAccessibility = when {
                rating >= fullStarValue  -> "$fullStarValue stars"
                rating >= halfStarValue  -> "$halfStarValue stars"
                else                     -> "No rating for star ${starIndex + 1}"
            }

            Icon(
                imageVector = icon,
                contentDescription = null,   // Row-level semantics cover accessibility
                tint = tint,
                modifier = Modifier
                    .size(starSize)
                    // Capture the actual rendered width so tap math is pixel-accurate.
                    .onSizeChanged { starWidthPx = it.width.toFloat() }
                    .then(
                        if (readOnly) Modifier
                        else Modifier
                            .semantics(mergeDescendants = true) {
                                contentDescription = starAccessibility
                                onClick(label = "Set rating to $halfStarValue or $fullStarValue") {
                                    onRatingChange(fullStarValue)
                                    true
                                }
                            }
                            .pointerInput(rating, starWidthPx) {
                                detectTapGestures { tapOffset ->
                                    val isLeftHalf = tapOffset.x < starWidthPx / 2f
                                    val tappedValue =
                                        if (isLeftHalf) halfStarValue else fullStarValue

                                    // Tapping the exact current value clears the rating.
                                    onRatingChange(
                                        if (rating == tappedValue) 0f else tappedValue
                                    )
                                }
                            }
                    )
            )
        }
    }
}
