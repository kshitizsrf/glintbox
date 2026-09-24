package com.glintbox.app.ui.common

import android.Manifest
import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.glintbox.app.R
import com.glintbox.app.data.SaveRepository
import com.glintbox.app.data.model.WaSource
import com.glintbox.app.data.storage.Saf
import com.glintbox.app.ui.MainViewModel
import com.glintbox.app.ui.components.GlowIcon
import com.glintbox.app.ui.theme.LocalGlint

/**
 * Every folder grant in the app goes through here so the user always sees a short,
 * friendly explanation before the system picker opens.
 */
@Stable
class FolderAccess internal constructor(
    val requestStatusAccess: (WaSource) -> Unit,
    /** Opens the guided flow for "Internal storage/WhatsApp Statuses" (Android 11+) */
    val requestDefaultSaveFolder: () -> Unit,
    val requestCustomSaveFolder: () -> Unit,
    val requestLegacyPermissions: () -> Unit,
)

private val LEGACY_PERMISSIONS = arrayOf(
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
)

private sealed interface Guide {
    data class Status(val source: WaSource) : Guide
    data object DefaultFolder : Guide
}

@Composable
fun rememberFolderAccess(vm: MainViewModel): FolderAccess {
    var pendingSource by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDefault by rememberSaveable { mutableStateOf(false) }
    var guide by remember { mutableStateOf<Guide?>(null) }

    val statusLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val source = pendingSource?.let { name -> WaSource.entries.firstOrNull { it.name == name } }
        if (uri != null && source != null) vm.onStatusFolderPicked(source, uri)
        pendingSource = null
    }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.onSaveFolderPicked(uri, fromDefaultFlow = pendingDefault)
        pendingDefault = false
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        vm.onLegacyPermissionsResult()
    }

    fun launchSafe(block: () -> Unit) {
        try {
            block()
        } catch (_: ActivityNotFoundException) {
            vm.message(R.string.msg_no_picker)
        }
    }

    when (val g = guide) {
        is Guide.Status -> GuideDialog(
            icon = { GlowIcon(Icons.Rounded.FolderOpen, size = 52.dp) },
            title = stringResource(R.string.guide_status_title, stringResource(g.source.labelRes)),
            steps = listOf(
                stringResource(R.string.guide_status_step1),
                stringResource(R.string.guide_status_step2),
                stringResource(R.string.guide_status_step3),
            ),
            note = stringResource(R.string.guide_status_note),
            onConfirm = {
                guide = null
                pendingSource = g.source.name
                launchSafe { statusLauncher.launch(Saf.statusesInitialUri(g.source)) }
            },
            onDismiss = { guide = null },
        )
        Guide.DefaultFolder -> GuideDialog(
            icon = { GlowIcon(Icons.Rounded.CreateNewFolder, size = 52.dp) },
            title = stringResource(R.string.guide_default_title),
            steps = listOf(
                stringResource(R.string.guide_default_step1),
                stringResource(R.string.guide_default_step2, SaveRepository.DEFAULT_FOLDER),
                stringResource(R.string.guide_default_step3),
                stringResource(R.string.guide_default_step4),
            ),
            note = stringResource(R.string.guide_default_note),
            onConfirm = {
                guide = null
                pendingDefault = true
                launchSafe { saveLauncher.launch(Saf.defaultFolderInitialUri(SaveRepository.DEFAULT_FOLDER)) }
            },
            onDismiss = { guide = null },
        )
        null -> Unit
    }

    return remember(vm) {
        FolderAccess(
            requestStatusAccess = { source ->
                if (vm.usesSaf) {
                    guide = Guide.Status(source)
                } else {
                    permissionLauncher.launch(LEGACY_PERMISSIONS)
                }
            },
            requestDefaultSaveFolder = {
                if (vm.needsSafForDefault) {
                    guide = Guide.DefaultFolder
                } else {
                    permissionLauncher.launch(LEGACY_PERMISSIONS)
                }
            },
            requestCustomSaveFolder = {
                pendingDefault = false
                launchSafe { saveLauncher.launch(null) }
            },
            requestLegacyPermissions = { permissionLauncher.launch(LEGACY_PERMISSIONS) },
        )
    }
}

@Composable
private fun GuideDialog(
    icon: @Composable () -> Unit,
    title: String,
    steps: List<String>,
    note: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val glint = LocalGlint.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.forEachIndexed { index, step ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(glint.gradient)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${index + 1}", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(step, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_open_picker)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_not_now)) }
        },
    )
}
