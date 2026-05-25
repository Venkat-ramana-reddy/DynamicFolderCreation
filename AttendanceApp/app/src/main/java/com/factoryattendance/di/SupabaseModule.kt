package com.factoryattendance.di

import com.factoryattendance.BuildConfig
import com.factoryattendance.data.preferences.AppPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides @Singleton
    fun provideSupabaseClient(prefs: AppPreferences): SupabaseClient {
        // Prefer runtime URL/key from DataStore; fall back to BuildConfig
        val (url, key) = runBlocking {
            val u = prefs.supabaseUrl.first().ifBlank { BuildConfig.SUPABASE_URL }
            val k = prefs.supabaseKey.first().ifBlank { BuildConfig.SUPABASE_ANON_KEY }
            Pair(u, k)
        }
        return createSupabaseClient(supabaseUrl = url, supabaseKey = key) {
            install(Postgrest)
            install(Realtime)
        }
    }
}
