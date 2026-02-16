package tech.tarakoshka.bridgemich.data

import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import tech.tarakoshka.bridgemich.App
import tech.tarakoshka.bridgemich.data.dtos.ImmichSearchResponse
import java.io.File

class ImmichRepository {
    private val client = App.client

    suspend fun login(url: String, email: String, password: String): String? {
        return runCatching {
            client.post("$url/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("email" to email, "password" to password))
            }.body<Map<String, String>>()["accessToken"]
        }.getOrNull()
    }

    suspend fun loadImages(baseUrl: String, token: String): List<Pair<String, String>>? {
        val ids = coroutineScope {
            client.post("$baseUrl/api/search/metadata") {
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

    suspend fun downloadAsset(assetId: String, baseUrl: String, token: String, onProgress: (Float) -> Unit): File? =
        withContext(Dispatchers.IO) {
            try {
                val file = File(App.app.cacheDir, "shared_photo.jpg")
                client.get("$baseUrl/api/assets/$assetId/original") {
                    header("Authorization", "Bearer $token")
                    onDownload { bytesSentTotal, contentLength ->
                        if (contentLength != null && contentLength > 0) {
                            onProgress(bytesSentTotal.toFloat() / contentLength)
                        }
                    }
                }.bodyAsChannel().copyAndClose(file.writeChannel())
                file
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
}
