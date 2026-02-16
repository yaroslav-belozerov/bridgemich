package tech.tarakoshka.bridgemich

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.tarakoshka.bridgemich.data.dtos.ImmichSearchResponse
import java.io.File
import kotlin.compareTo
import kotlin.text.toFloat

suspend fun downloadAndShare(assetId: String, token: String, onProgress: (Float) -> Unit): File? =
    withContext(Dispatchers.IO) {
        try {
            val file = File(App.app.cacheDir, "shared_photo.jpg")
            App.client.get("https://immich.tarakoshka.tech/api/assets/$assetId/original") {
                header("Authorization", "Bearer $token")
                onDownload { bytesSentTotal, contentLength ->
                    if (contentLength != null) {
                        if (contentLength > 0) {
                            val progress = bytesSentTotal.toFloat() / contentLength
                            onProgress(progress)
                        }
                    }
                }
            }.bodyAsChannel().copyAndClose(file.writeChannel())
            return@withContext file
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

suspend fun loadImages(baseUrl: String, token: String): List<Pair<String, String>>? {
    val ids = coroutineScope {
        App.client.post("$baseUrl/api/search/metadata") {
            header("Authorization", "Bearer $token")
        }.body<ImmichSearchResponse>().assets?.items?.map { asset ->
            asset.id?.let { id ->
                asset.originalMimeType?.let { type ->
                    id to type
                }
            }
        }
    }

    return ids?.filterNotNull()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 0)
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
        }

        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                val token by App.dataStore.token.collectAsState(initial = "")
                val url by App.dataStore.url.collectAsState(initial = "")

                Surface(
                    color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.systemBars),
                    ) {
                        if (token.isBlank()) {
                            val scope = rememberCoroutineScope { Dispatchers.IO }
                            var url by remember { mutableStateOf("") }
                            var email by remember { mutableStateOf("") }
                            var password by remember { mutableStateOf("") }
                            var error by remember { mutableStateOf<String?>(null) }
                            var loggingIn by remember { mutableStateOf(false) }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(
                                    4.dp, Alignment.CenterVertically
                                ),
                                modifier = Modifier
                                    .padding(horizontal = 24.dp)
                                    .imePadding()
                                    .fillMaxSize()
                            ) {
                                OutlinedTextField(
                                    enabled = !loggingIn,
                                    placeholder = {
                                        Text("https://")
                                    },
                                    label = {
                                        Text("Instance URL")
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Next,
                                        keyboardType = KeyboardType.Uri,
                                        autoCorrectEnabled = false
                                    ),
                                    shape = MaterialTheme.shapes.large,
                                    modifier = Modifier.fillMaxWidth(),
                                    value = url,
                                    onValueChange = {
                                        scope.launch { url = it }
                                    })
                                OutlinedTextField(
                                    enabled = !loggingIn,
                                    placeholder = {
                                        Text("user@example.org")
                                    },
                                    label = {
                                        Text("E-Mail")
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Next,
                                        keyboardType = KeyboardType.Email,
                                        autoCorrectEnabled = false
                                    ),
                                    shape = MaterialTheme.shapes.large,
                                    modifier = Modifier.fillMaxWidth(),
                                    value = email,
                                    onValueChange = { email = it })
                                OutlinedTextField(
                                    enabled = !loggingIn,
                                    label = {
                                        Text("Password")
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Done,
                                        keyboardType = KeyboardType.Password,
                                        autoCorrectEnabled = false
                                    ),
                                    keyboardActions = KeyboardActions {
                                        loggingIn = true
                                        scope.launch {
                                            Log.d(
                                                "INFO",
                                                "Logging in to $url with $email and $password"
                                            )
                                            runCatching {
                                                App.client.post("$url/api/auth/login") {
                                                    contentType(ContentType.Application.Json)
                                                    setBody(
                                                        mapOf(
                                                            "email" to email, "password" to password
                                                        )
                                                    )
                                                }.body<Map<String, String>>()["accessToken"]!!
                                            }.onSuccess {
                                                Log.d("INFO", "Login successful")
                                                App.dataStore.setEmail(email)
                                                App.dataStore.setUrl(url)
                                                App.dataStore.setToken(it)
                                            }.onFailure { t ->
                                                error = t.message ?: t.toString()
                                            }
                                            loggingIn = false
                                        }
                                    },
                                    visualTransformation = PasswordVisualTransformation(),
                                    shape = MaterialTheme.shapes.large,
                                    modifier = Modifier.fillMaxWidth(),
                                    value = password,
                                    onValueChange = { password = it })
                                Button(
                                    enabled = !loggingIn,
                                    onClick = {
                                        loggingIn = true
                                        scope.launch {
                                            Log.d(
                                                "INFO",
                                                "Logging in to $url with $email and $password"
                                            )
                                            runCatching {
                                                App.client.post("$url/api/auth/login") {
                                                    contentType(ContentType.Application.Json)
                                                    setBody(
                                                        mapOf(
                                                            "email" to email, "password" to password
                                                        )
                                                    )
                                                }.body<Map<String, String>>()["accessToken"]!!
                                            }.onSuccess {
                                                Log.d("INFO", "Login successful")
                                                App.dataStore.setEmail(email)
                                                App.dataStore.setUrl(url)
                                                App.dataStore.setToken(it)
                                            }.onFailure { t ->
                                                error = t.message ?: t.toString()
                                            }
                                            loggingIn = false
                                        }
                                    },
                                    shape = MaterialTheme.shapes.large,
                                    modifier = Modifier.align(Alignment.End),
                                ) {
                                    if (loggingIn) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    }
                                    Text("Login")
                                }
                                error?.let {
                                    val clip = LocalClipboard.current
                                    Card(
                                        onClick = {
                                            scope.launch {
                                                clip.setClipEntry(
                                                    ClipEntry(
                                                        ClipData.newPlainText(
                                                            "Bredgemich error", error
                                                        )
                                                    )
                                                )
                                            }
                                        }, colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    ) { Text(it, modifier = Modifier.padding(8.dp)) }
                                }
                            }
                        } else {
                            val scope = rememberCoroutineScope { Dispatchers.IO }
                            var images: List<Pair<String, String>>? by remember {
                                mutableStateOf(
                                    null
                                )
                            }
                            val ctx = LocalContext.current
                            val username by App.dataStore.email.collectAsState(initial = "")
                            val loader = remember(token) {
                                ImageLoader.Builder(ctx).components {
                                    add(UrlAuthInterceptor(token))
                                }.build()
                            }
                            var progress by remember { mutableFloatStateOf(0f) }
                            val progressAnim by animateFloatAsState(progress)
                            var clickedId by remember { mutableStateOf<String?>(null) }
                            LaunchedEffect(Unit) {
                                scope.launch {
                                    images = loadImages(url, token).orEmpty()
                                }
                            }
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
                                IconButton(onClick = {
                                    scope.launch {
                                        App.dataStore.setToken("")
                                        App.dataStore.setUrl("")
                                        App.dataStore.setEmail("")
                                    }
                                }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.AutoMirrored.Default.ExitToApp,
                                        contentDescription = null
                                    )
                                }
                            }
                            LinearProgressIndicator(
                                progress = { progressAnim },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (clickedId == null) Modifier.alpha(0f) else Modifier
                                    )
                            )
                            images?.let { images ->
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4)
                                ) {
                                    items(images) { imageId ->
                                        Box {
                                            AsyncImage(
                                                "$url/api/assets/${imageId.first}/thumbnail?size=thumbnail",
                                                imageLoader = loader,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .padding(2.dp)
                                                    .aspectRatio(1f)
                                                    .then(
                                                        if (clickedId == null) {
                                                        Modifier.clickable {
                                                            clickedId = imageId.first
                                                            scope.launch {
                                                                downloadAndShare(
                                                                    imageId.first,
                                                                    token,
                                                                    onProgress = {
                                                                        progress = it
                                                                    })?.let { shared ->
                                                                    withContext(Dispatchers.Main) {
                                                                        setResult(
                                                                            RESULT_OK,
                                                                            Intent().apply {
                                                                                data =
                                                                                    FileProvider.getUriForFile(
                                                                                        this@MainActivity,
                                                                                        "${packageName}.fileprovider",
                                                                                        shared
                                                                                    )
                                                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                            })
                                                                        finish()
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } else Modifier),
                                                contentScale = ContentScale.Crop)
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
                                                    if (clickedId == imageId.first) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(
                                                                24.dp
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } ?: Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) { CircularProgressIndicator(modifier = Modifier.size(32.dp)) }
                        }
                    }
                }
            }
        }
    }
}
