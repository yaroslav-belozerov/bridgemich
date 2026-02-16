package tech.tarakoshka.bridgemich

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tech.tarakoshka.bridgemich.data.ImmichRepository
import java.io.File

class MainViewModel : ViewModel() {
    private val repository = ImmichRepository()
    private val dataStore = App.dataStore

    val token: StateFlow<String> = dataStore.token.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val url: StateFlow<String> = dataStore.url.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val email: StateFlow<String> = dataStore.email.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    var images by mutableStateOf<List<Pair<String, String>>?>(null)
        private set

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
            val token = repository.login(url, email, pass)
            if (token != null) {
                dataStore.setUrl(url)
                dataStore.setEmail(email)
                dataStore.setToken(token)
            } else {
                loginError = "Login failed. Please check your credentials and URL."
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

    fun loadImages() {
        val currentUrl = url.value
        val currentToken = token.value
        if (currentUrl.isNotBlank() && currentToken.isNotBlank()) {
            viewModelScope.launch {
                images = repository.loadImages(currentUrl, currentToken).orEmpty()
            }
        }
    }

    fun downloadAndShare(assetId: String, onFinished: (File) -> Unit) {
        clickedId = assetId
        downloadProgress = 0f
        viewModelScope.launch {
            repository.downloadAsset(assetId, url.value, token.value) {
                downloadProgress = it
            }?.let {
                onFinished(it)
            }
            clickedId = null
        }
    }
}
