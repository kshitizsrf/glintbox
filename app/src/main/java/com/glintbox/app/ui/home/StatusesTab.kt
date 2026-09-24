package com.glintbox.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glintbox.app.R
import com.glintbox.app.data.model.AccessState
import com.glintbox.app.data.model.MediaFilter
import com.glintbox.app.data.model.MediaType
import com.glintbox.app.data.model.StatusMedia
import com.glintbox.app.ui.MainViewModel
import com.glintbox.app.ui.common.FolderAccess
import com.glintbox.app.ui.components.FilterPill
import com.glintbox.app.ui.components.GlassButton
import com.glintbox.app.ui.components.GlassCard
import com.glintbox.app.ui.components.GradientButton
import com.glintbox.app.ui.components.StatBlock
import com.glintbox.app.ui.components.StatusTile
import com.glintbox.app.util.Intents

fun List<StatusMedia>.filteredBy(mediaFilter: MediaFilter): List<StatusMedia> = when (mediaFilter) {
    MediaFilter.ALL -> this
    MediaFilter.IMAGES -> filter { it.type == MediaType.IMAGE }
    MediaFilter.VIDEOS -> filter { it.type == MediaType.VIDEO }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusesTab(
    vm: MainViewModel,
    folderAccess: FolderAccess,
    onOpenViewer: (Int, Boolean) -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings ?: return
    val state by vm.statuses.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var mediaFilter by rememberSaveable { mutableStateOf(MediaFilter.ALL) }
    var selection by remember { mutableStateOf(emptySet<String>()) }
    var pulling by remember { mutableStateOf(false) }

    val visible = remember(state.items, mediaFilter) { state.items.filteredBy(mediaFilter) }
    val unsaved = remember(visible, saved.names) { visible.filter { it.name !in saved.names } }
    val imageCount = remember(state.items) { state.items.count { it.type == MediaType.IMAGE } }
    val videoCount = state.items.size - imageCount
    val selectionMode = selection.isNotEmpty()

    fun buzz() {
        if (s.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun toggle(item: StatusMedia) {
        selection = if (item.id in selection) selection - item.id else selection + item.id
    }

    LaunchedEffect(state.loading) {
        if (!state.loading) {
            pulling = false
        }
    }
    LaunchedEffect(state.source) { selection = emptySet() }
    BackHandler(enabled = selectionMode) { selection = emptySet() }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    PullToRefreshBox(
        isRefreshing = pulling && state.loading,
        onRefresh = {
            pulling = true
            vm.refresh()
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(s.gridColumns),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topInset + 8.dp, bottom = 132.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                if (selectionMode) {
                    SelectionBar(count = selection.size, onClose = { selection = emptySet() }) {
                        CircleIconButton(Icons.Rounded.SelectAll, stringResource(R.string.action_select_all), {
                            selection = visible.mapTo(HashSet()) { it.id }
                        })
                        CircleIconButton(Icons.Rounded.Share, stringResource(R.string.action_share), {
                            val chosen = visible.filter { it.id in selection }
                            if (!Intents.share(context, chosen)) vm.message(R.string.msg_share_failed)
                        })
                        CircleIconButton(Icons.Rounded.Download, stringResource(R.string.action_save), {
                            buzz()
                            vm.saveAll(visible.filter { it.id in selection })
                            selection = emptySet()
                        })
                    }
                } else {
                    TabHeader(
                        title = stringResource(R.string.app_name),
                        subtitle = stringResource(R.string.home_subtitle),
                    ) {
                        CircleIconButton(Icons.Rounded.Refresh, stringResource(R.string.action_refresh), { vm.refresh() })
                    }
                }
            }

            if (s.enabledSources.size > 1) {
                item(key = "sources", span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        s.enabledSources.sortedBy { it.ordinal }.forEach { source ->
                            FilterPill(
                                text = stringResource(source.labelRes),
                                selected = source == state.source,
                                onClick = { vm.setActiveSource(source) },
                            )
                        }
                    }
                }
            }

            when {
                state.access == AccessState.NEEDS_ACCESS -> item(key = "access", span = { GridItemSpan(maxLineSpan) }) {
                    val label = stringResource(state.source.labelRes)
                    MessageCard(
                        icon = Icons.Rounded.LockOpen,
                        title = stringResource(R.string.access_title, label),
                        message = stringResource(
                            if (vm.usesSaf) R.string.access_message_saf else R.string.access_message_legacy,
                            label,
                        ),
                    ) {
                        GradientButton(
                            text = stringResource(R.string.action_grant_access),
                            onClick = { folderAccess.requestStatusAccess(state.source) },
                            icon = Icons.Rounded.LockOpen,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (!state.installed) {
                            Text(
                                stringResource(R.string.app_not_installed, label),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }

                state.access == AccessState.FOLDER_MISSING -> item(key = "missing", span = { GridItemSpan(maxLineSpan) }) {
                    val label = stringResource(state.source.labelRes)
                    MessageCard(
                        icon = Icons.Rounded.SearchOff,
                        title = stringResource(R.string.folder_missing_title),
                        message = stringResource(R.string.folder_missing_message, label),
                    ) {
                        if (state.installed) {
                            GradientButton(
                                text = stringResource(R.string.action_open_app, label),
                                onClick = { Intents.launchApp(context, state.source.packageName) },
                                icon = Icons.Rounded.OpenInNew,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        GlassButton(
                            text = stringResource(R.string.action_choose_again),
                            onClick = { folderAccess.requestStatusAccess(state.source) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                state.loading && state.items.isEmpty() -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }

                state.items.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                    val label = stringResource(state.source.labelRes)
                    MessageCard(
                        icon = Icons.Rounded.HourglassEmpty,
                        title = stringResource(R.string.empty_statuses_title),
                        message = stringResource(R.string.empty_statuses_message, label),
                    ) {
                        if (state.installed) {
                            GradientButton(
                                text = stringResource(R.string.action_open_app, label),
                                onClick = { Intents.launchApp(context, state.source.packageName) },
                                icon = Icons.Rounded.OpenInNew,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                else -> {
                    item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                        GlassCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                StatBlock(imageCount.toString(), stringResource(R.string.stat_images), Modifier.weight(1f))
                                StatBlock(videoCount.toString(), stringResource(R.string.stat_videos), Modifier.weight(1f))
                                StatBlock(unsaved.size.toString(), stringResource(R.string.stat_new), Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(16.dp))
                            if (unsaved.isNotEmpty()) {
                                GradientButton(
                                    text = pluralStringResource(R.plurals.save_all_new, unsaved.size, unsaved.size),
                                    onClick = {
                                        buzz()
                                        vm.saveAll(unsaved)
                                    },
                                    icon = Icons.Rounded.Download,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.DownloadDone, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.all_caught_up),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterPill(stringResource(R.string.filter_all), mediaFilter == MediaFilter.ALL, { mediaFilter = MediaFilter.ALL }, count = state.items.size)
                            FilterPill(stringResource(R.string.filter_images), mediaFilter == MediaFilter.IMAGES, { mediaFilter = MediaFilter.IMAGES }, count = imageCount)
                            FilterPill(stringResource(R.string.filter_videos), mediaFilter == MediaFilter.VIDEOS, { mediaFilter = MediaFilter.VIDEOS }, count = videoCount)
                        }
                    }

                    if (visible.isEmpty()) {
                        item(key = "filter-empty", span = { GridItemSpan(maxLineSpan) }) {
                            MessageCard(
                                icon = Icons.Rounded.SearchOff,
                                title = stringResource(R.string.empty_filter_title),
                                message = stringResource(R.string.empty_filter_message),
                            )
                        }
                    }

                    itemsIndexed(visible, key = { _, item -> item.id }) { index, item ->
                        StatusTile(
                            item = item,
                            isSaved = item.name in saved.names,
                            isSaving = item.id in saving,
                            selected = item.id in selection,
                            selectionMode = selectionMode,
                            showQuickSave = true,
                            onClick = {
                                if (selectionMode) {
                                    toggle(item)
                                } else {
                                    vm.openViewer(visible)
                                    onOpenViewer(index, false)
                                }
                            },
                            onLongClick = {
                                buzz()
                                toggle(item)
                            },
                            onQuickSave = {
                                buzz()
                                vm.save(item)
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }

                    item(key = "respect", span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, start = 8.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Shield,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(
                                stringResource(R.string.respect_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
