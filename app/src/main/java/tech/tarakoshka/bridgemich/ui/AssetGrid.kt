package tech.tarakoshka.bridgemich.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage

@Composable
fun AssetGrid(
    url: String,
    username: String,
    images: List<Pair<String, String>>,
    imageLoader: ImageLoader,
    clickedId: String?,
    downloadProgress: Float,
    onLogout: () -> Unit,
    onAssetClick: (String) -> Unit
) {
    val progressAnim by animateFloatAsState(downloadProgress, label = "downloadProgress")
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (clickedId == null) 0f else 1f, label = "indicatorAlpha"
    )
    var menuOpen by remember { mutableStateOf(false) }


    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(12.dp)
        ) {
            Text(buildAnnotatedString {
                pushStyle(
                    MaterialTheme.typography.bodyMedium.toSpanStyle()
                )
                append("Logged in as  ")
                pushStyle(
                    MaterialTheme.typography.bodyMedium.toSpanStyle()
                        .copy(color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                )
                append(username)
                pop()
                append("  on ")
                pushStyle(
                    MaterialTheme.typography.bodyLarge.toSpanStyle()
                        .copy(color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                )
                pushLink(LinkAnnotation.Url(url))
                append(url)
                pop()
            }, modifier = Modifier.weight(1f), lineHeight = 18.sp)
            IconButton(onClick = {
                menuOpen = true
            }, modifier = Modifier.size(48.dp)) {
                Icon(
                    Icons.Default.MoreVert, contentDescription = "Options"
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    shape = MaterialTheme.shapes.large,
                    offset = DpOffset(0.dp, 8.dp)
                ) {
                    val ctx = LocalContext.current
                    val versionName = remember(ctx) {
                        val packageManager = ctx.packageManager
                        val packageName = ctx.packageName
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
                        } else {
                            packageManager.getPackageInfo(packageName, 0)
                        }.versionName
                    }
                    versionName?.let { version ->
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        ) {
                            Text("Bridgemich", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.tertiary))
                            Text("v$version", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.tertiary))
                        }
                    }
                    val uriHandler = LocalUriHandler.current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.clickable {
                            uriHandler.openUri("https://fj.tarakoshka.tech/tarakoshka/bridgemich")
                        }.padding(horizontal = 24.dp, vertical = 8.dp).fillMaxWidth()
                    ) {
                        Text("Source")
                        Icon(
                            Icons.Default.Code, contentDescription = "Repository"
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.clickable {
                            onLogout()
                        }.padding(horizontal = 24.dp, vertical = 8.dp).fillMaxWidth()
                    ) {
                        Text("Log out")
                        Icon(
                            Icons.AutoMirrored.Default.ExitToApp, contentDescription = "Logout"
                        )
                    }
                }
            }
        }

        LinearProgressIndicator(
            progress = { progressAnim }, modifier = Modifier
                .fillMaxWidth()
                .alpha(indicatorAlpha)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4), modifier = Modifier.fillMaxSize()
        ) {
            items(images, key = { it.first }) { imageInfo ->
                val assetId = imageInfo.first
                val isDownloading = clickedId == assetId

                val overlayAlpha by animateFloatAsState(
                    targetValue = when {
                        clickedId == null -> 0f
                        isDownloading -> 0.7f
                        else -> 0.3f
                    }, label = "overlayAlpha"
                )

                val itemScale by animateFloatAsState(
                    targetValue = if (isDownloading) 0.95f else 1f, label = "itemScale"
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
                            LoadingCircle(size = 24.dp)
                        }
                    }
                }
            }
        }
    }
}
