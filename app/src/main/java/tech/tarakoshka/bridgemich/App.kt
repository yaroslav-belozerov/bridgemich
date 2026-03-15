package tech.tarakoshka.bridgemich

import android.app.Application
import coil.ImageLoader
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        app = this
    }

    companion object {
        lateinit var app: Application

        val dataStore by lazy { DataStoreManager(app) }
        val client by lazy {
            HttpClient(Android) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 15_000
                }
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
                expectSuccess = true
            }
        }
    }
}
