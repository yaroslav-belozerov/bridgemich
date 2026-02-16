package tech.tarakoshka.bridgemich

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.tarakoshka.bridgemich.data.ImmichRepository
import tech.tarakoshka.bridgemich.data.dtos.ImmichError
import java.io.File

sealed class AuthState {
    data object Loading : AuthState()
    data class Authenticated(
        val token: String,
        val url: String,
        val email: String,
        val images: List<Pair<String, String>>
    ) : AuthState()
    data object Unauthenticated : AuthState()
}

class MainViewModel : ViewModel() {
    private val repository = ImmichRepository()
    private val dataStore = App.dataStore

    var images by mutableStateOf<List<Pair<String, String>>?>(null)
        private set

    val authState: StateFlow<AuthState> = combine(
        dataStore.token, dataStore.url, dataStore.email, snapshotFlow { images }
    ) { token, url, email, currentImages ->
        if (token.isBlank()) {
            AuthState.Unauthenticated
        } else if (currentImages == null) {
            AuthState.Loading
        } else {
            AuthState.Authenticated(token, url, email, currentImages)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Loading)

    init {
        viewModelScope.launch {
            combine(dataStore.token, dataStore.url) { token, url ->
                token to url
            }.collect { (token, url) ->
                if (token.isNotBlank() && images == null) {
                    loadImages(url, token)
                }
            }
        }
    }

    var loggingIn by mutableStateOf(false)
        private set

    var loginError by mutableStateOf<String?>(null)
        private set

    var downloadProgress by mutableFloatStateOf(0f)
        private set

    var clickedId by mutableStateOf<String?>(null)

    fun login(url: String, email: String, pass: String) {
        loggingIn = true
        loginError = null
        viewModelScope.launch {
            repository.login(url, email, pass).onSuccess {
                dataStore.setUrl(url)
                dataStore.setEmail(email)
                dataStore.setToken(it)
            }.onFailure {
                loginError = when (it) {
                    is ClientRequestException, is ServerResponseException -> {
                        runCatching {
                            it.response.body<ImmichError>().run {
                                "$message ($statusCode): $error"
                            }
                        }.getOrNull() ?: "Error fetching data: ${it.message ?: it}"
                    }
                    else -> "Error on client: ${it.message ?: it}"
                }
            }
            loggingIn = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            dataStore.setToken("")
            dataStore.setUrl("")
            dataStore.setEmail("")
            images = null
        }
    }

    fun loadImages(url: String, token: String) {
        viewModelScope.launch {
            images = repository.loadImages(url, token).orEmpty()
        }
    }

    fun downloadAndShare(assetId: String, url: String, token: String, onFinished: (File) -> Unit) {
        clickedId = assetId
        downloadProgress = 0f
        viewModelScope.launch {
            repository.downloadAsset(assetId, url, token) {
                downloadProgress = it
            }?.let {
                onFinished(it)
            }
            clickedId = null
        }
    }
}
