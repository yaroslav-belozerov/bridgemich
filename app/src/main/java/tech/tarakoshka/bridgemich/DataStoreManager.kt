package tech.tarakoshka.bridgemich

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

class DataStoreManager(ctx: Context) {
    private val Context.preferences by preferencesDataStore("prefs")
    private val dataStore = ctx.preferences
    private val tokenKey = stringPreferencesKey("token")
    private val instanceKey = stringPreferencesKey("instance")
    private val emailKey = stringPreferencesKey("username")
    private val isApiKeyKey = booleanPreferencesKey("is_api_key")

    val token = dataStore.data.map { it[tokenKey].orEmpty() }
    suspend fun setToken(value: String) = dataStore.edit { it[tokenKey] = value }

    val url = dataStore.data.map { it[instanceKey].orEmpty() }
    suspend fun setUrl(value: String) {
        dataStore.edit { it[instanceKey] = value }
    }

    val email = dataStore.data.map { it[emailKey].orEmpty() }
    suspend fun setEmail(value: String) {
        dataStore.edit { it[emailKey] = value }
    }

    val isApiKey = dataStore.data.map { it[isApiKeyKey] ?: false }
    suspend fun setIsApiKey(value: Boolean) {
        dataStore.edit { it[isApiKeyKey] = value }
    }
}