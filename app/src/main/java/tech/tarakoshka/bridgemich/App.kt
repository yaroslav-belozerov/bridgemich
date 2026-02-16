package tech.tarakoshka.bridgemich

import android.app.Application
import coil.ImageLoader
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.firstOrNull

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
                    requestTimeoutMillis = 20_000
                }
                install(ContentNegotiation) {
                    json()
                }
                expectSuccess = true
            }
        }
    }
}
