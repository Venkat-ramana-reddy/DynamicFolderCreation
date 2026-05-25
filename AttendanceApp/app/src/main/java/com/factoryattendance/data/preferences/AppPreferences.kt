package com.factoryattendance.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ds = context.dataStore

    companion object {
        val KEY_ADMIN_PIN_HASH  = stringPreferencesKey("admin_pin_hash")
        val KEY_DARK_MODE       = booleanPreferencesKey("dark_mode")
        val KEY_SUPABASE_URL    = stringPreferencesKey("supabase_url")
        val KEY_SUPABASE_KEY    = stringPreferencesKey("supabase_key")
        val KEY_IS_ADMIN        = booleanPreferencesKey("is_admin")
        val KEY_CURRENT_WORKER  = stringPreferencesKey("current_worker_id")
        val KEY_LAST_SYNC       = longPreferencesKey("last_sync_ts")
        val KEY_FACTORY_LAT     = doublePreferencesKey("factory_lat")
        val KEY_FACTORY_LNG     = doublePreferencesKey("factory_lng")
        val KEY_FENCE_RADIUS    = intPreferencesKey("fence_radius")
        // Default admin PIN is "1234" — SHA-256 hash
        const val DEFAULT_PIN_HASH = "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"
    }

    val adminPinHash: Flow<String> = ds.data.map {
        it[KEY_ADMIN_PIN_HASH] ?: DEFAULT_PIN_HASH
    }
    val isDarkMode: Flow<Boolean>  = ds.data.map { it[KEY_DARK_MODE] ?: false }
    val supabaseUrl: Flow<String>  = ds.data.map { it[KEY_SUPABASE_URL] ?: "" }
    val supabaseKey: Flow<String>  = ds.data.map { it[KEY_SUPABASE_KEY] ?: "" }
    val isAdmin: Flow<Boolean>     = ds.data.map { it[KEY_IS_ADMIN] ?: false }
    val currentWorkerId: Flow<String?> = ds.data.map { it[KEY_CURRENT_WORKER] }
    val factoryLat: Flow<Double>   = ds.data.map { it[KEY_FACTORY_LAT] ?: 0.0 }
    val factoryLng: Flow<Double>   = ds.data.map { it[KEY_FACTORY_LNG] ?: 0.0 }
    val fenceRadius: Flow<Int>     = ds.data.map { it[KEY_FENCE_RADIUS] ?: 200 }

    suspend fun setAdminPinHash(hash: String) = ds.edit { it[KEY_ADMIN_PIN_HASH] = hash }
    suspend fun setDarkMode(dark: Boolean)     = ds.edit { it[KEY_DARK_MODE] = dark }
    suspend fun setSupabaseUrl(url: String)    = ds.edit { it[KEY_SUPABASE_URL] = url }
    suspend fun setSupabaseKey(key: String)    = ds.edit { it[KEY_SUPABASE_KEY] = key }
    suspend fun setAdmin(admin: Boolean)       = ds.edit { it[KEY_IS_ADMIN] = admin }
    suspend fun setCurrentWorker(id: String?)  = ds.edit {
        if (id == null) it.remove(KEY_CURRENT_WORKER) else it[KEY_CURRENT_WORKER] = id
    }
    suspend fun setFactoryLocation(lat: Double, lng: Double) = ds.edit {
        it[KEY_FACTORY_LAT] = lat
        it[KEY_FACTORY_LNG] = lng
    }
    suspend fun setFenceRadius(r: Int) = ds.edit { it[KEY_FENCE_RADIUS] = r }
    suspend fun setLastSync(ts: Long)  = ds.edit { it[KEY_LAST_SYNC] = ts }
}
