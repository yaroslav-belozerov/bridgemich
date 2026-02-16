package tech.tarakoshka.bridgemich.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage

@Composable
fun AssetGrid(
    url: String,
    username: String,
    images: List<Pair<String, String>>?,
    imageLoader: ImageLoader,
    clickedId: String?,
    downloadProgress: Float,
    onLogout: () -> Unit,
    onAssetClick: (String) -> Unit
) {
    val progressAnim by animateFloatAsState(downloadProgress, label = "downloadProgress")
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (clickedId == null) 0f else 1f,
        label = "indicatorAlpha"
    )

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp)
        ) {
            Text(buildAnnotatedString {
                append("Logged in as ")
                pushStyle(
                    MaterialTheme.typography.bodyLarge.toSpanStyle()
                        .copy(color = MaterialTheme.colorScheme.primary)
                )
                append(username)
                pop()
                append(" on ")
                pushStyle(
                    MaterialTheme.typography.bodyLarge.toSpanStyle()
                        .copy(color = MaterialTheme.colorScheme.primary)
                )
                append(url)
                pop()
            }, modifier = Modifier.weight(1f), lineHeight = 18.sp)
            IconButton(onClick = onLogout, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.AutoMirrored.Default.ExitToApp,
                    contentDescription = "Logout"
                )
            }
        }

        LinearProgressIndicator(
            progress = { progressAnim },
            modifier = Modifier
                .fillMaxWidth()
                .alpha(indicatorAlpha)
        )

        AnimatedContent(
            targetState = images,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "gridContent"
        ) { currentImages ->
            if (currentImages == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(currentImages, key = { it.first }) { imageInfo ->
                        val assetId = imageInfo.first
                        val isDownloading = clickedId == assetId
                        
                        val overlayAlpha by animateFloatAsState(
                            targetValue = when {
                                clickedId == null -> 0f
                                isDownloading -> 0.7f
                                else -> 0.3f
                            },
                            label = "overlayAlpha"
                        )
                        
                        val itemScale by animateFloatAsState(
                            targetValue = if (isDownloading) 0.95f else 1f,
                            label = "itemScale"
                        )

                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .aspectRatio(1f)
                                .scale(itemScale)
                        ) {
                            AsyncImage(
                                model = "$url/api/assets/$assetId/thumbnail?size=thumbnail",
                                imageLoader = imageLoader,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(enabled = clickedId == null) {
                                        onAssetClick(assetId)
                                    },
                                contentScale = ContentScale.Crop
                            )
                            
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = overlayAlpha)),
                                contentAlignment = Alignment.Center
                            ) {
                                this@Column.AnimatedVisibility(
                                    visible = isDownloading,
                                    enter = fadeIn() + scaleIn(),
                                    exit = fadeOut() + scaleOut()
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
