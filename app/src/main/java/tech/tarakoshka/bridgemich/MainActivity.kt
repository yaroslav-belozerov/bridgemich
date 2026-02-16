package tech.tarakoshka.bridgemich

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import tech.tarakoshka.bridgemich.data.dtos.ImmichSearchResponse
import java.io.File
import kotlin.collections.emptyList

suspend fun downloadAndShare(assetId: String, token: String): File? = withContext(Dispatchers.IO) {
    try {
        val file = File(App.app.cacheDir, "shared_photo.jpg")
        App.client.get("https://immich.tarakoshka.tech/api/assets/$assetId/original") {
            header("Authorization", "Bearer $token")
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
            MaterialTheme() {
                Scaffold { padding ->
                    val token by App.dataStore.token.collectAsState(initial = "")
                    val context = rememberCoroutineScope()
                    ImagePickerScreen(
                        modifier = Modifier.padding(padding), onImageSelected = { imageId ->
                            context.launch {
                                val shared = downloadAndShare(imageId.first, token)
                                if (shared != null) {
                                    withContext(Dispatchers.Main) {
                                        val result = Intent().apply {
                                            data = FileProvider.getUriForFile(
                                                this@MainActivity,
                                                "${packageName}.fileprovider",
                                                shared
                                            )
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        setResult(RESULT_OK, result)
                                        finish()
                                    }
                                }
                            }
                        })
                }
            }
        }
    }
}

@Composable
fun ImagePickerScreen(
    modifier: Modifier = Modifier, onImageSelected: (Pair<String, String>) -> Unit
) {
    val token by App.dataStore.token.collectAsState(initial = "")
    val url by App.dataStore.url.collectAsState(initial = "")

    if (token.isBlank()) {
        val scope = rememberCoroutineScope { Dispatchers.IO }
        var url by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                OutlinedTextField(
                    shape = MaterialTheme.shapes.large, value = url, onValueChange = {
                        scope.launch { url = it }
                    })
                OutlinedTextField(
                    shape = MaterialTheme.shapes.large,
                    value = email,
                    onValueChange = { email = it })
                OutlinedTextField(
                    shape = MaterialTheme.shapes.large,
                    value = password,
                    onValueChange = { password = it })
                Button(onClick = {
                    scope.launch {
                        Log.d("INFO", "Logging in to $url with $email and $password")
                        runCatching {
                            App.client.post("$url/api/auth/login") {
                                contentType(ContentType.Application.Json)
                                setBody(mapOf("email" to email, "password" to password))
                            }.body<Map<String, String>>()["accessToken"]!!
                        }.onSuccess {
                            Log.d("INFO", "Login successful")
                            App.dataStore.setUrl(url)
                            App.dataStore.setToken(it)
                        }.onFailure {
                            error = it.message ?: it.toString()
                        }
                    }
                }) {
                    Text("Login")
                }
                error?.let {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) { Text(it, modifier = Modifier.padding(8.dp)) }
                }
            }
        }
    } else {
        val scope = rememberCoroutineScope { Dispatchers.IO }
        var images: List<Pair<String, String>> by remember { mutableStateOf(emptyList()) }
        val ctx = LocalContext.current
        val loader = remember(token) {
            ImageLoader.Builder(ctx).components {
                add(UrlAuthInterceptor(token))
            }.build()
        }
        LaunchedEffect(Unit) {
            scope.launch {
                images = loadImages(url, token).orEmpty()
            }
        }
        LazyVerticalGrid(
            modifier = modifier,
            columns = GridCells.Adaptive(100.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(images) { imageId ->
                var visible by remember { mutableStateOf(false) }
                Box {
                    AsyncImage(
                        "$url/api/assets/${imageId.first}/thumbnail?size=thumbnail",
                        imageLoader = loader,
                        contentDescription = null,
                        onState = {
                            visible = when (it) {
                                AsyncImagePainter.State.Empty, is AsyncImagePainter.State.Loading, is AsyncImagePainter.State.Error -> true
                                is AsyncImagePainter.State.Success -> false
                            }
                        },

                        modifier = Modifier
                            .padding(4.dp)
                            .size(100.dp)
                            .clickable {
                                onImageSelected(imageId)
                            },
                        contentScale = ContentScale.Crop
                    )
                    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.onBackground.copy(0.5f))
                        )
                    }
                }
            }
        }
    }
}
