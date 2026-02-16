package tech.tarakoshka.bridgemich

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import coil.ImageLoader
import tech.tarakoshka.bridgemich.ui.AssetGrid
import tech.tarakoshka.bridgemich.ui.LoginScreen
import tech.tarakoshka.bridgemich.ui.theme.BridgemichTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 0)
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
        }

        setContent {
            BridgemichTheme {
                val authState by viewModel.authState.collectAsState()
                val ctx = LocalContext.current

                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.systemBars)
                    ) {
                        AnimatedContent(
                            targetState = authState,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "authTransition"
                        ) { state ->
                            when (state) {
                                is AuthState.Loading -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                                    }
                                }

                                is AuthState.Unauthenticated -> {
                                    LoginScreen(
                                        loggingIn = viewModel.loggingIn,
                                        error = viewModel.loginError,
                                        onLogin = { loginUrl, loginEmail, loginPass ->
                                            viewModel.login(loginUrl, loginEmail, loginPass)
                                        }
                                    )
                                }

                                is AuthState.Authenticated -> {
                                    val loader = remember(state.token) {
                                        ImageLoader.Builder(ctx).components {
                                            add(UrlAuthInterceptor(state.token))
                                        }.build()
                                    }

                                    LaunchedEffect(state.token, state.url) {
                                        viewModel.loadImages(state.url, state.token)
                                    }

                                    AssetGrid(
                                        url = state.url,
                                        username = state.email,
                                        images = viewModel.images,
                                        imageLoader = loader,
                                        clickedId = viewModel.clickedId,
                                        downloadProgress = viewModel.downloadProgress,
                                        onLogout = { viewModel.logout() },
                                        onAssetClick = { assetId ->
                                            viewModel.downloadAndShare(
                                                assetId,
                                                state.url,
                                                state.token
                                            ) { file ->
                                                setResult(RESULT_OK, Intent().apply {
                                                    data = FileProvider.getUriForFile(
                                                        this@MainActivity,
                                                        "${packageName}.fileprovider",
                                                        file
                                                    )
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                })
                                                finish()
                                            }
                                        }
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
