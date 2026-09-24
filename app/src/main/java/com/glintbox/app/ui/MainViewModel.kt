package com.glintbox.app.ui

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glintbox.app.R
import com.glintbox.app.appContainer
import com.glintbox.app.data.StatusFolderMissingException
import com.glintbox.app.data.model.AccentPalette
import com.glintbox.app.data.model.AccessState
import com.glintbox.app.data.model.AppSettings
import com.glintbox.app.data.model.SaveResult
import com.glintbox.app.data.model.SaveTarget
import com.glintbox.app.data.model.StatusMedia
import com.glintbox.app.data.model.ThemeMode
import com.glintbox.app.data.model.WaSource
import com.glintbox.app.data.storage.Saf
import com.glintbox.app.work.AutoSaveWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class StatusesState(
    val loading: Boolean = true,
    val source: WaSource = WaSource.WHATSAPP,
    val access: AccessState = AccessState.GRANTED,
    val installed: Boolean = true,
    val items: List<StatusMedia> = emptyList(),
)

@Immutable
data class SavedState(
    val loading: Boolean = true,
    val target: SaveTarget? = null,
    val pathLabel: String = "",
    val items: List<StatusMedia> = emptyList(),
) {
    val names: Set<String> = items.mapTo(HashSet()) { it.name }
}

@Immutable
data class UiMessage(@param:StringRes val res: Int, val args: List<Any> = emptyList())

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val container = app.appContainer
    private val settingsRepo = container.settings
    private val statusRepo = container.statuses
    private val saveRepo = container.saves

    val settings: StateFlow<AppSettings?> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _statuses = MutableStateFlow(StatusesState())
    val statuses: StateFlow<StatusesState> = _statuses.asStateFlow()

    private val _saved = MutableStateFlow(SavedState())
    val saved: StateFlow<SavedState> = _saved.asStateFlow()

    private val _access = MutableStateFlow<Map<WaSource, AccessState>>(emptyMap())
    val access: StateFlow<Map<WaSource, AccessState>> = _access.asStateFlow()

    private val _saving = MutableStateFlow<Set<String>>(emptySet())
    val saving: StateFlow<Set<String>> = _saving.asStateFlow()

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    /** Items the full-screen viewer pages through; set right before navigating. */
    var viewerItems by mutableStateOf<List<StatusMedia>>(emptyList())
        private set

    private var refreshJob: Job? = null

    val usesSaf: Boolean get() = statusRepo.usesSaf
    val needsSafForDefault: Boolean get() = saveRepo.needsSafForDefault

    init {
        viewModelScope.launch {
            settings.filterNotNull()
                .map { RefreshKey(it.activeSource, it.statusTreeUris, it.defaultTreeUri, it.customTreeUri, it.useCustomLocation) }
                .distinctUntilChanged()
                .collect { refresh() }
        }
    }

    private data class RefreshKey(
        val source: WaSource,
        val trees: Map<WaSource, String>,
        val defaultTree: String?,
        val customTree: String?,
        val useCustom: Boolean,
    )

    private suspend fun currentSettings(): AppSettings = settings.value ?: settingsRepo.settings.first()

    fun isInstalled(source: WaSource) = statusRepo.isInstalled(source)

    fun hasLegacyWrite() = saveRepo.hasLegacyWritePermission()

    // ------------------------------------------------------------ refreshing

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val s = currentSettings()
            _access.value = WaSource.entries.associateWith { statusRepo.accessState(it, s) }
            refreshSaved(s)
            refreshStatuses(s)
        }
    }

    private suspend fun refreshStatuses(s: AppSettings) {
        val source = s.activeSource
        val installed = statusRepo.isInstalled(source)
        _statuses.update { it.copy(loading = true, source = source, installed = installed) }
        val access = statusRepo.accessState(source, s)
        if (access != AccessState.GRANTED) {
            _statuses.value = StatusesState(false, source, access, installed, emptyList())
            return
        }
        val result = statusRepo.load(source, s)
        _statuses.value = result.fold(
            onSuccess = { StatusesState(false, source, AccessState.GRANTED, installed, it) },
            onFailure = { e ->
                val state = if (e is StatusFolderMissingException) AccessState.FOLDER_MISSING else AccessState.NEEDS_ACCESS
                StatusesState(false, source, state, installed, emptyList())
            },
        )
    }

    private suspend fun refreshSaved(s: AppSettings) {
        val target = saveRepo.resolveTarget(s)
        _saved.update { it.copy(loading = true, target = target, pathLabel = saveRepo.describe(target)) }
        val items = saveRepo.listSaved(target)
        _saved.value = SavedState(false, target, saveRepo.describe(target), items)
    }

    private suspend fun reloadSaved() = refreshSaved(currentSettings())

    // ----------------------------------------------------------------- saving

    fun save(item: StatusMedia) = saveAll(listOf(item), announceSingle = true)

    fun saveAll(items: List<StatusMedia>, announceSingle: Boolean = false) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val s = currentSettings()
            val target = saveRepo.resolveTarget(s)
            val existing = HashSet(_saved.value.names)
            var ok = 0
            var already = 0
            var failed = 0
            _saving.update { it + items.map(StatusMedia::id) }
            for (item in items) {
                when (saveRepo.save(item, target, existing)) {
                    is SaveResult.Saved -> { ok++; existing += item.name }
                    SaveResult.AlreadySaved -> already++
                    is SaveResult.Failed -> failed++
                }
                _saving.update { it - item.id }
            }
            if (ok > 0) settingsRepo.addSaved(ok)
            reloadSaved()
            val msg = when {
                failed > 0 && ok == 0 -> UiMessage(R.string.msg_save_failed)
                failed > 0 -> UiMessage(R.string.msg_saved_partial, listOf(ok, failed))
                ok == 0 && already > 0 -> UiMessage(R.string.msg_already_saved)
                announceSingle || ok == 1 -> UiMessage(R.string.msg_saved_to, listOf(saveRepo.describe(target)))
                else -> UiMessage(R.string.msg_saved_many, listOf(ok))
            }
            _messages.emit(msg)
        }
    }

    fun delete(items: List<StatusMedia>, onDone: (() -> Unit)? = null) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val target = _saved.value.target ?: saveRepo.resolveTarget(currentSettings())
            var deleted = 0
            for (item in items) if (saveRepo.delete(item, target)) deleted++
            val ids = items.mapTo(HashSet()) { it.id }
            viewerItems = viewerItems.filterNot { it.id in ids }
            reloadSaved()
            _messages.emit(
                if (deleted == items.size) UiMessage(R.string.msg_deleted, listOf(deleted))
                else UiMessage(R.string.msg_delete_failed),
            )
            onDone?.invoke()
        }
    }

    fun openViewer(items: List<StatusMedia>) {
        viewerItems = items
    }

    fun message(@StringRes res: Int, vararg args: Any) {
        _messages.tryEmit(UiMessage(res, args.toList()))
    }

    // ------------------------------------------------------------ folder access

    fun onStatusFolderPicked(source: WaSource, uri: Uri) {
        viewModelScope.launch {
            statusRepo.forget(uri)
            if (!Saf.persist(getApplication(), uri, write = false)) {
                _messages.emit(UiMessage(R.string.msg_access_failed))
                return@launch
            }
            val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                statusRepo.resolveStatusesDir(uri) != null
            }
            if (!ok) {
                Saf.release(getApplication(), uri)
                _messages.emit(UiMessage(R.string.msg_wrong_status_folder))
                return@launch
            }
            val old = currentSettings().statusTreeUris[source]
            if (old != null && old != uri.toString() && currentSettings().statusTreeUris.values.count { it == old } == 1) {
                Saf.release(getApplication(), old.toUri())
            }
            settingsRepo.setStatusTree(source, uri.toString())
            settingsRepo.setActiveSource(source)
            _messages.emit(UiMessage(R.string.msg_access_granted))
            refresh()
        }
    }

    fun onSaveFolderPicked(uri: Uri, fromDefaultFlow: Boolean) {
        viewModelScope.launch {
            if (!Saf.persist(getApplication(), uri, write = true)) {
                _messages.emit(UiMessage(R.string.msg_access_failed))
                return@launch
            }
            val s = currentSettings()
            val isDefault = needsSafForDefault && saveRepo.isDefaultFolderTree(uri)
            if (isDefault) {
                settingsRepo.setDefaultTree(uri.toString())
                s.customTreeUri?.let { Saf.release(getApplication(), it.toUri()) }
                settingsRepo.setCustomTree(null)
                _messages.emit(UiMessage(R.string.msg_default_folder_ready))
            } else {
                if (fromDefaultFlow) _messages.emit(UiMessage(R.string.msg_not_default_folder))
                s.customTreeUri?.takeIf { it != uri.toString() }?.let { Saf.release(getApplication(), it.toUri()) }
                settingsRepo.setCustomTree(uri.toString())
                if (!fromDefaultFlow) _messages.emit(UiMessage(R.string.msg_custom_folder_set))
            }
            refresh()
        }
    }

    fun resetSaveLocation() {
        viewModelScope.launch {
            currentSettings().customTreeUri?.let { Saf.release(getApplication(), it.toUri()) }
            settingsRepo.setCustomTree(null)
            _messages.emit(UiMessage(R.string.msg_location_reset))
        }
    }

    fun onLegacyPermissionsResult() = refresh()

    // --------------------------------------------------------------- settings

    fun setActiveSource(source: WaSource) = viewModelScope.launch { settingsRepo.setActiveSource(source) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    fun setPalette(palette: AccentPalette) = viewModelScope.launch { settingsRepo.setPalette(palette) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsRepo.setDynamicColor(enabled) }
    fun setAmoled(enabled: Boolean) = viewModelScope.launch { settingsRepo.setAmoled(enabled) }
    fun setGridColumns(columns: Int) = viewModelScope.launch { settingsRepo.setGridColumns(columns) }
    fun setHaptics(enabled: Boolean) = viewModelScope.launch { settingsRepo.setHaptics(enabled) }
    fun setConfirmDelete(enabled: Boolean) = viewModelScope.launch { settingsRepo.setConfirmDelete(enabled) }
    fun setAutoSaveVideos(enabled: Boolean) = viewModelScope.launch { settingsRepo.setAutoSaveVideos(enabled) }

    fun setAutoSave(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.setAutoSave(enabled)
        if (enabled) AutoSaveWorker.schedule(getApplication()) else AutoSaveWorker.cancel(getApplication())
        _messages.emit(UiMessage(if (enabled) R.string.msg_auto_save_on else R.string.msg_auto_save_off))
    }

    fun setSourceEnabled(source: WaSource, enabled: Boolean) = viewModelScope.launch {
        val current = currentSettings().enabledSources
        val next = if (enabled) current + source else current - source
        if (next.isEmpty()) {
            _messages.emit(UiMessage(R.string.msg_keep_one_source))
            return@launch
        }
        settingsRepo.setEnabledSources(next)
    }

    fun completeOnboarding() = viewModelScope.launch { settingsRepo.setOnboardingDone() }
}
