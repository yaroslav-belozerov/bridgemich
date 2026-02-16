package tech.tarakoshka.bridgemich.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
                .alpha(if (clickedId == null) 0f else 1f)
        )

        if (images == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4)
            ) {
                items(images) { imageInfo ->
                    val assetId = imageInfo.first
                    Box {
                        AsyncImage(
                            model = "$url/api/assets/$assetId/thumbnail?size=thumbnail",
                            imageLoader = imageLoader,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(2.dp)
                                .aspectRatio(1f)
                                .clickable(enabled = clickedId == null) {
                                    onAssetClick(assetId)
                                },
                            contentScale = ContentScale.Crop
                        )
                        if (clickedId != null) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(
                                            alpha = 0.5f
                                        )
                                    ), contentAlignment = Alignment.Center
                            ) {
                                if (clickedId == assetId) {
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
