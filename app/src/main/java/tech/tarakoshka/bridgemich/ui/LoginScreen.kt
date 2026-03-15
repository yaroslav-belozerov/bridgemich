package tech.tarakoshka.bridgemich.ui

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    loggingIn: Boolean,
    error: String?,
    onLogin: (url: String, email: String, pass: String) -> Unit,
    onLoginWithApiKey: (url: String, apiKey: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var useApiKey by remember { mutableStateOf(false) }

    val canLogin = !loggingIn && url.isNotBlank() && if (useApiKey) {
        apiKey.isNotBlank()
    } else {
        email.isNotBlank() && password.isNotBlank()
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            8.dp, Alignment.CenterVertically
        ),
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .imePadding()
            .fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Bridgemich", style = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 6.sp, fontWeight = FontWeight.Bold))
            Text(
                if (useApiKey) "Log in with API key" else "Log in with Immich credentials",
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary)
            )
        }
        OutlinedTextField(
            enabled = !loggingIn,
            placeholder = { Text("https://") },
            label = { Text("Instance URL") },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Uri,
                autoCorrectEnabled = false
            ),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
            value = url,
            onValueChange = { url = it }
        )
        if (useApiKey) {
            OutlinedTextField(
                enabled = !loggingIn,
                label = { Text("API Key") },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(onDone = {
                    if (canLogin) onLoginWithApiKey(url, apiKey)
                }),
                visualTransformation = PasswordVisualTransformation(),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
                value = apiKey,
                onValueChange = { apiKey = it }
            )
        } else {
            OutlinedTextField(
                enabled = !loggingIn,
                placeholder = { Text("user@example.org") },
                label = { Text("E-Mail") },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Email,
                    autoCorrectEnabled = false
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
                value = email,
                onValueChange = { email = it }
            )
            OutlinedTextField(
                enabled = !loggingIn,
                label = { Text("Password") },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Password,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(onDone = {
                    if (canLogin) onLogin(url, email, password)
                }),
                visualTransformation = PasswordVisualTransformation(),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
                value = password,
                onValueChange = { password = it }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                enabled = !loggingIn,
                onClick = { useApiKey = !useApiKey }
            ) {
                Text(if (useApiKey) "Use email & password" else "Use API key")
            }
            Button(
                enabled = canLogin,
                onClick = {
                    if (useApiKey) onLoginWithApiKey(url, apiKey) else onLogin(url, email, password)
                },
                shape = MaterialTheme.shapes.large,
            ) {
                if (loggingIn) {
                    LoadingCircle(
                        size = 16.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                }
                Text("Login", modifier = Modifier.padding(start = 4.dp))
            }
        }
        error?.let {
            val clip = LocalClipboard.current
            Card(
                onClick = {
                    scope.launch {
                        clip.setClipEntry(
                            ClipEntry(
                                ClipData.newPlainText("Bridgemich error", it)
                            )
                        )
                    }
                },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(it)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                        Text("Click to copy", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
