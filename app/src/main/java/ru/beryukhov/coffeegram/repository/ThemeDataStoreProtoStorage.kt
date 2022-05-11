package ru.beryukhov.coffeegram.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import kotlinx.coroutines.flow.firstOrNull
import ru.beryukhov.coffeegram.model.ThemeState
import ru.beryukhov.coffeegram.repository.ThemePreferences.ProtoThemeState
import ru.beryukhov.coffeegram.store_lib.Storage

private const val DATA_STORE_FILE_NAME = "user_prefs.pb"

class ThemeDataStoreProtoStorage(private val context: Context) : Storage<ThemeState> {

    private val Context.dataStore: DataStore<ThemePreferences> by dataStore(
        fileName = DATA_STORE_FILE_NAME,
        serializer = ThemePreferencesSerializer,
        produceMigrations = { context ->
            listOf(
                getSharedPreferencesMigration(context)
            )
        }
    )

    private fun getSharedPreferencesMigration(context: Context) =
        SharedPreferencesMigration(
            context = context, sharedPreferencesName = FILENAME,
            /*, keysToMigrate = MIGRATE_ALL_KEYS*/
            migrate = { sharedPrefs: SharedPreferencesView, currentData: ThemePreferences ->
                getMappingFromSharedPrefs(currentData, sharedPrefs)
            }
        )

    private fun getMappingFromSharedPrefs(
        currentData: ThemePreferences,
        sharedPrefs: SharedPreferencesView
    ) = if (currentData.themeState == ProtoThemeState.SYSTEM) {
        currentData.toBuilder().setThemeState(
            ProtoThemeState.valueOf(
                sharedPrefs.getString(THEME_STATE, ThemeState.SYSTEM.name)!!
            )
        ).build()
    } else {
        currentData
    }

    override suspend fun getState(): ThemeState? {
        // do not confuse with `lastOrNull()`, it will be waiting for completion inside otherwise
        return context.dataStore.data.firstOrNull()
            ?.themeState.mapOrNull()
    }

    override suspend fun saveState(state: ThemeState) {
        context.dataStore.updateData { preferences ->
            preferences.toBuilder().setThemeState(state.map()).build()
        }
    }
}

private fun ThemeState.map(): ProtoThemeState {
    return ProtoThemeState.valueOf(this.name)
}

private fun ProtoThemeState?.mapOrNull(): ThemeState? {
    if (this == null) return null
    return ThemeState.valueOf(this.name)
}
