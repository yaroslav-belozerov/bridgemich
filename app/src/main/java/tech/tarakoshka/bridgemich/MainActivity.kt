package tech.tarakoshka.bridgemich

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                val token by viewModel.token.collectAsState()
                val url by viewModel.url.collectAsState()
                val email by viewModel.email.collectAsState()
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
                        if (token.isBlank()) {
                            LoginScreen(
                                loggingIn = viewModel.loggingIn,
                                error = viewModel.loginError,
                                onLogin = { loginUrl, loginEmail, loginPass ->
                                    viewModel.login(loginUrl, loginEmail, loginPass)
                                }
                            )
                        } else {
                            val loader = remember(token) {
                                ImageLoader.Builder(ctx).components {
                                    add(UrlAuthInterceptor(token))
                                }.build()
                            }

                            LaunchedEffect(token, url) {
                                viewModel.loadImages()
                            }

                            AssetGrid(
                                url = url,
                                username = email,
                                images = viewModel.images,
                                imageLoader = loader,
                                clickedId = viewModel.clickedId,
                                downloadProgress = viewModel.downloadProgress,
                                onLogout = { viewModel.logout() },
                                onAssetClick = { assetId ->
                                    viewModel.downloadAndShare(assetId) { file ->
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
