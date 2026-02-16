package tech.tarakoshka.bridgemich.ui

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    loggingIn: Boolean,
    error: String?,
    onLogin: (url: String, email: String, pass: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
                onLogin(url, email, password)
            }),
            visualTransformation = PasswordVisualTransformation(),
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
            value = password,
            onValueChange = { password = it }
        )
        Button(
            enabled = !loggingIn,
            onClick = { onLogin(url, email, password) },
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
                                ClipData.newPlainText("Bridgemich error", it)
                            )
                        )
                    }
                },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text(it, modifier = Modifier.padding(8.dp))
            }
        }
    }
}
