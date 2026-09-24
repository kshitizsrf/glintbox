package com.glintbox.app.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import com.glintbox.app.R
import com.glintbox.app.data.model.StatusMedia
import com.glintbox.app.ui.MainViewModel
import com.glintbox.app.ui.theme.LocalGlint
import com.glintbox.app.util.Intents
import com.glintbox.app.util.fileSize
import com.glintbox.app.util.hoursUntilExpiry
import com.glintbox.app.util.relativeTime

@Composable
fun ViewerScreen(
    vm: MainViewModel,
    startIndex: Int,
    savedMode: Boolean,
    onBack: () -> Unit,
) {
    val items = vm.viewerItems
    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val settings by vm.settings.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(0, items.lastIndex)) { items.size }
    var chromeVisible by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<StatusMedia?>(null) }

    val current = items[pagerState.currentPage.coerceIn(0, items.lastIndex)]
    val isSaved = savedMode || current.name in saved.names
    val isSaving = current.id in saving

    LaunchedEffect(pagerState.settledPage) { zoomed = false }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !zoomed,
            key = { items[it].id },
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = items[page]
            if (!item.isVideo) {
                ZoomableImage(
                    uri = item.uri,
                    contentDescription = stringResource(R.string.cd_image_status, relativeTime(item.modified)),
                    onTap = { chromeVisible = !chromeVisible },
                    onZoomChanged = { isZoomed ->
                        if (page == pagerState.settledPage) {
                            zoomed = isZoomed
                        }
                    },
                )
            } else if (page == pagerState.settledPage) {
                VideoPlayer(
                    uri = item.uri,
                    onControllerVisible = { chromeVisible = it },
                    modifier = Modifier.padding(bottom = if (chromeVisible) 96.dp else 0.dp),
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(item.uri).videoFrameMillis(1_000).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Icon(Icons.Rounded.PlayCircle, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(64.dp))
                }
            }
        }

        // ------------------------------------------------------------ top bar
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DarkCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), onBack)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.viewer_position, pagerState.currentPage + 1, items.size),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "${relativeTime(current.modified)} · ${fileSize(current.size)}",
                        color = Color.White.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!savedMode) {
                    hoursUntilExpiry(current.modified)?.let { hours ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.16f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Schedule, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                pluralStringResource(R.plurals.expires_in_hours, hours.toInt(), hours.toInt()),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }

        // --------------------------------------------------------- action bar
        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ViewerAction(Icons.Rounded.Share, stringResource(R.string.action_share)) {
                    if (!Intents.share(context, listOf(current))) vm.message(R.string.msg_share_failed)
                }
                val sourcePackage = current.source?.packageName ?: settings?.activeSource?.packageName
                if (sourcePackage != null) {
                    ViewerAction(Icons.Rounded.Repeat, stringResource(R.string.action_repost)) {
                        if (!Intents.share(context, listOf(current), targetPackage = sourcePackage)) {
                            vm.message(R.string.msg_repost_failed)
                        }
                    }
                }
                if (savedMode) {
                    ViewerAction(Icons.Rounded.Delete, stringResource(R.string.action_delete)) {
                        if (settings?.confirmDelete != false) {
                            confirmDelete = current
                        } else {
                            vm.delete(listOf(current))
                        }
                    }
                } else {
                    SaveAction(
                        saved = isSaved,
                        saving = isSaving,
                        onClick = {
                            if (settings?.haptics != false) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.save(current)
                        },
                    )
                }
            }
        }
    }

    confirmDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            icon = { Icon(Icons.Rounded.Delete, null) },
            title = { Text(pluralStringResource(R.plurals.delete_title, 1, 1)) },
            text = { Text(stringResource(R.string.delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    vm.delete(listOf(target)) { if (vm.viewerItems.isEmpty()) onBack() }
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun DarkCircleButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Color.White)
    }
}

@Composable
private fun ViewerAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = Color.White) }
        Spacer(Modifier.size(4.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SaveAction(saved: Boolean, saving: Boolean, onClick: () -> Unit) {
    val glint = LocalGlint.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (saved) Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
                else Brush.horizontalGradient(glint.gradient),
            )
            .clickable(enabled = !saving && !saved, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            saving -> CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            saved -> Icon(Icons.Rounded.DownloadDone, null, tint = Color.White)
            else -> Icon(Icons.Rounded.Download, null, tint = Color.White)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(if (saved) R.string.state_saved else R.string.action_save),
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}
