package com.pixelvault.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pixelvault_settings")

class UserPreferences(
    private val context: Context
) {
    companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val CHARGING_ONLY = booleanPreferencesKey("charging_only")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val LOCK_TIMEOUT_SEC = intPreferencesKey("lock_timeout_sec")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { it[SERVER_URL] ?: "https://photos.yourdomain.com" }
    val wifiOnly: Flow<Boolean> = context.dataStore.data.map { it[WIFI_ONLY] ?: true }
    val chargingOnly: Flow<Boolean> = context.dataStore.data.map { it[CHARGING_ONLY] ?: false }
    val autoBackup: Flow<Boolean> = context.dataStore.data.map { it[AUTO_BACKUP] ?: true }
    val gridColumns: Flow<Int> = context.dataStore.data.map { it[GRID_COLUMNS] ?: 3 }
    val lockTimeoutSec: Flow<Int> = context.dataStore.data.map { it[LOCK_TIMEOUT_SEC] ?: 60 }

    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { it[SERVER_URL] = url }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.dataStore.edit { it[WIFI_ONLY] = enabled }
    }

    suspend fun setChargingOnly(enabled: Boolean) {
        context.dataStore.edit { it[CHARGING_ONLY] = enabled }
    }

    suspend fun setGridColumns(cols: Int) {
        context.dataStore.edit { it[GRID_COLUMNS] = cols }
    }
}
