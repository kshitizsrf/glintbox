package com.glintbox.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.glintbox.app.data.model.AccentPalette
import com.glintbox.app.data.model.AppSettings
import com.glintbox.app.data.model.ThemeMode
import com.glintbox.app.data.model.WaSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "glintbox_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PALETTE = stringPreferencesKey("palette")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AMOLED = booleanPreferencesKey("amoled")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val HAPTICS = booleanPreferencesKey("haptics")
        val CONFIRM_DELETE = booleanPreferencesKey("confirm_delete")
        val ENABLED_SOURCES = stringSetPreferencesKey("enabled_sources")
        val ACTIVE_SOURCE = stringPreferencesKey("active_source")
        val DEFAULT_TREE = stringPreferencesKey("default_tree_uri")
        val CUSTOM_TREE = stringPreferencesKey("custom_tree_uri")
        val USE_CUSTOM = booleanPreferencesKey("use_custom_location")
        val AUTO_SAVE = booleanPreferencesKey("auto_save")
        val AUTO_SAVE_VIDEOS = booleanPreferencesKey("auto_save_videos")
        val TOTAL_SAVED = intPreferencesKey("total_saved")
        fun statusTree(source: WaSource) = stringPreferencesKey("status_tree_${source.name.lowercase()}")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { it.toSettings() }
        .distinctUntilChanged()

    private fun Preferences.toSettings(): AppSettings {
        val enabled = this[Keys.ENABLED_SOURCES]
            ?.mapNotNull { name -> WaSource.entries.firstOrNull { it.name == name } }
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
            ?: WaSource.entries.toSet()
        val active = this[Keys.ACTIVE_SOURCE]
            ?.let { name -> WaSource.entries.firstOrNull { it.name == name } }
            ?.takeIf { it in enabled }
            ?: enabled.minBy { it.ordinal }
        return AppSettings(
            onboardingDone = this[Keys.ONBOARDING_DONE] ?: false,
            themeMode = enumOr(this[Keys.THEME_MODE], ThemeMode.SYSTEM),
            palette = enumOr(this[Keys.PALETTE], AccentPalette.AURORA),
            dynamicColor = this[Keys.DYNAMIC_COLOR] ?: false,
            amoled = this[Keys.AMOLED] ?: false,
            gridColumns = (this[Keys.GRID_COLUMNS] ?: 3).coerceIn(2, 4),
            haptics = this[Keys.HAPTICS] ?: true,
            confirmDelete = this[Keys.CONFIRM_DELETE] ?: true,
            enabledSources = enabled,
            activeSource = active,
            statusTreeUris = WaSource.entries.mapNotNull { src ->
                this[Keys.statusTree(src)]?.let { src to it }
            }.toMap(),
            defaultTreeUri = this[Keys.DEFAULT_TREE],
            customTreeUri = this[Keys.CUSTOM_TREE],
            useCustomLocation = this[Keys.USE_CUSTOM] ?: false,
            autoSave = this[Keys.AUTO_SAVE] ?: false,
            autoSaveVideos = this[Keys.AUTO_SAVE_VIDEOS] ?: true,
            totalSaved = this[Keys.TOTAL_SAVED] ?: 0,
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(value: String?, fallback: T): T =
        value?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: fallback

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    suspend fun setOnboardingDone() = edit { it[Keys.ONBOARDING_DONE] = true }
    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setPalette(palette: AccentPalette) = edit { it[Keys.PALETTE] = palette.name }
    suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }
    suspend fun setAmoled(enabled: Boolean) = edit { it[Keys.AMOLED] = enabled }
    suspend fun setGridColumns(columns: Int) = edit { it[Keys.GRID_COLUMNS] = columns.coerceIn(2, 4) }
    suspend fun setHaptics(enabled: Boolean) = edit { it[Keys.HAPTICS] = enabled }
    suspend fun setConfirmDelete(enabled: Boolean) = edit { it[Keys.CONFIRM_DELETE] = enabled }
    suspend fun setActiveSource(source: WaSource) = edit { it[Keys.ACTIVE_SOURCE] = source.name }
    suspend fun setAutoSave(enabled: Boolean) = edit { it[Keys.AUTO_SAVE] = enabled }
    suspend fun setAutoSaveVideos(enabled: Boolean) = edit { it[Keys.AUTO_SAVE_VIDEOS] = enabled }

    suspend fun setEnabledSources(sources: Set<WaSource>) = edit { prefs ->
        prefs[Keys.ENABLED_SOURCES] = sources.map { it.name }.toSet()
    }

    suspend fun setStatusTree(source: WaSource, uri: String?) = edit { prefs ->
        if (uri == null) prefs.remove(Keys.statusTree(source)) else prefs[Keys.statusTree(source)] = uri
    }

    suspend fun setDefaultTree(uri: String?) = edit { prefs ->
        if (uri == null) prefs.remove(Keys.DEFAULT_TREE) else prefs[Keys.DEFAULT_TREE] = uri
        prefs[Keys.USE_CUSTOM] = false
    }

    suspend fun setCustomTree(uri: String?) = edit { prefs ->
        if (uri == null) {
            prefs.remove(Keys.CUSTOM_TREE)
            prefs[Keys.USE_CUSTOM] = false
        } else {
            prefs[Keys.CUSTOM_TREE] = uri
            prefs[Keys.USE_CUSTOM] = true
        }
    }

    suspend fun addSaved(count: Int) = edit { prefs ->
        prefs[Keys.TOTAL_SAVED] = (prefs[Keys.TOTAL_SAVED] ?: 0) + count
    }
}
