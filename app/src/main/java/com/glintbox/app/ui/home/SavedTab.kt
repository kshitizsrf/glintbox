package com.glintbox.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFolderUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glintbox.app.R
import com.glintbox.app.data.model.MediaFilter
import com.glintbox.app.data.model.MediaType
import com.glintbox.app.data.model.SaveTarget
import com.glintbox.app.data.model.StatusMedia
import com.glintbox.app.ui.MainViewModel
import com.glintbox.app.ui.common.FolderAccess
import com.glintbox.app.ui.components.FilterPill
import com.glintbox.app.ui.components.GlassButton
import com.glintbox.app.ui.components.GlassCard
import com.glintbox.app.ui.components.GlowIcon
import com.glintbox.app.ui.components.GradientButton
import com.glintbox.app.ui.components.StatusTile
import com.glintbox.app.util.Intents

@Composable
fun SavedTab(
    vm: MainViewModel,
    folderAccess: FolderAccess,
    onOpenViewer: (Int, Boolean) -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings ?: return
    val saved by vm.saved.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var mediaFilter by rememberSaveable { mutableStateOf(MediaFilter.ALL) }
    var selection by remember { mutableStateOf(emptySet<String>()) }
    var pendingDelete by remember { mutableStateOf<List<StatusMedia>>(emptyList()) }

    val visible = remember(saved.items, mediaFilter) { saved.items.filteredBy(mediaFilter) }
    val imageCount = remember(saved.items) { saved.items.count { it.type == MediaType.IMAGE } }
    val selectionMode = selection.isNotEmpty()
    val isCustom = (saved.target as? SaveTarget.Tree)?.isDefault == false
    val isFallback = saved.target == SaveTarget.MediaStoreFallback
    val needsLegacyPermission = saved.target is SaveTarget.LegacyFolder && !vm.hasLegacyWrite()

    fun toggle(item: StatusMedia) {
        selection = if (item.id in selection) selection - item.id else selection + item.id
    }

    fun requestDelete(items: List<StatusMedia>) {
        if (s.confirmDelete) pendingDelete = items else vm.delete(items)
        selection = emptySet()
    }

    BackHandler(enabled = selectionMode) { selection = emptySet() }

    if (pendingDelete.isNotEmpty()) {
        val items = pendingDelete
        AlertDialog(
            onDismissRequest = { pendingDelete = emptyList() },
            icon = { Icon(Icons.Rounded.Delete, null) },
            title = { Text(pluralStringResource(R.plurals.delete_title, items.size, items.size)) },
            text = { Text(stringResource(R.string.delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(items)
                    pendingDelete = emptyList()
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = emptyList() }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

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
                        if (!Intents.share(context, visible.filter { it.id in selection })) vm.message(R.string.msg_share_failed)
                    })
                    CircleIconButton(Icons.Rounded.Delete, stringResource(R.string.action_delete), {
                        requestDelete(visible.filter { it.id in selection })
                    })
                }
            } else {
                TabHeader(
                    title = stringResource(R.string.saved_title),
                    subtitle = pluralStringResource(R.plurals.saved_subtitle, saved.items.size, saved.items.size),
                )
            }
        }

        item(key = "location", span = { GridItemSpan(maxLineSpan) }) {
            GlassCard(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlowIcon(Icons.Rounded.Folder, size = 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(if (isCustom) R.string.location_custom else R.string.location_default),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            saved.pathLabel,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (isFallback) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.fallback_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    GradientButton(
                        text = stringResource(R.string.action_setup_default),
                        onClick = folderAccess.requestDefaultSaveFolder,
                        icon = Icons.Rounded.CreateNewFolder,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (needsLegacyPermission) {
                    Spacer(Modifier.height(12.dp))
                    GradientButton(
                        text = stringResource(R.string.action_allow_storage),
                        onClick = folderAccess.requestLegacyPermissions,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(
                        text = stringResource(R.string.action_change_folder),
                        onClick = folderAccess.requestCustomSaveFolder,
                        icon = Icons.Rounded.DriveFolderUpload,
                        modifier = Modifier.weight(1f),
                    )
                    if (isCustom) {
                        GlassButton(
                            text = stringResource(R.string.action_use_default),
                            onClick = { vm.resetSaveLocation() },
                            icon = Icons.Rounded.Restore,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        if (saved.items.isEmpty() && !saved.loading) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                MessageCard(
                    icon = Icons.Rounded.PhotoLibrary,
                    title = stringResource(R.string.empty_saved_title),
                    message = stringResource(R.string.empty_saved_message),
                )
            }
        } else if (saved.items.isNotEmpty()) {
            item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterPill(stringResource(R.string.filter_all), mediaFilter == MediaFilter.ALL, { mediaFilter = MediaFilter.ALL }, count = saved.items.size)
                    FilterPill(stringResource(R.string.filter_images), mediaFilter == MediaFilter.IMAGES, { mediaFilter = MediaFilter.IMAGES }, count = imageCount)
                    FilterPill(stringResource(R.string.filter_videos), mediaFilter == MediaFilter.VIDEOS, { mediaFilter = MediaFilter.VIDEOS }, count = saved.items.size - imageCount)
                }
            }

            itemsIndexed(visible, key = { _, item -> item.id }) { index, item ->
                StatusTile(
                    item = item,
                    isSaved = true,
                    isSaving = false,
                    selected = item.id in selection,
                    selectionMode = selectionMode,
                    showQuickSave = false,
                    onClick = {
                        if (selectionMode) {
                            toggle(item)
                        } else {
                            vm.openViewer(visible)
                            onOpenViewer(index, true)
                        }
                    },
                    onLongClick = {
                        if (s.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        toggle(item)
                    },
                    onQuickSave = {},
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}
