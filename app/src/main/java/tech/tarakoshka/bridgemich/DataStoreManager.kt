package tech.tarakoshka.bridgemich

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

class DataStoreManager(ctx: Context) {
    private val Context.preferences by preferencesDataStore("prefs")
    private val dataStore = ctx.preferences
    private val tokenKey = stringPreferencesKey("token")
    private val instanceKey = stringPreferencesKey("instance")

    val token = dataStore.data.map { it[tokenKey].orEmpty() }
    suspend fun setToken(value: String) = dataStore.edit { it[tokenKey] = value }

    val url = dataStore.data.map { it[instanceKey].orEmpty() }
    suspend fun setUrl(value: String) {
        dataStore.edit { it[instanceKey] = value }
    }
}