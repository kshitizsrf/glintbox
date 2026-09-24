package com.glintbox.app.ui.home

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DriveFolderUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glintbox.app.BuildConfig
import com.glintbox.app.R
import com.glintbox.app.data.SaveRepository
import com.glintbox.app.data.model.AccentPalette
import com.glintbox.app.data.model.AccessState
import com.glintbox.app.data.model.SaveTarget
import com.glintbox.app.data.model.ThemeMode
import com.glintbox.app.data.model.WaSource
import com.glintbox.app.ui.MainViewModel
import com.glintbox.app.ui.common.FolderAccess
import com.glintbox.app.ui.components.FilterPill
import com.glintbox.app.ui.components.GlassCard
import com.glintbox.app.ui.components.GradientText
import com.glintbox.app.ui.components.SectionLabel
import com.glintbox.app.ui.theme.LocalGlint
import com.glintbox.app.ui.theme.isAppInDarkTheme
import com.glintbox.app.ui.theme.spec
import com.glintbox.app.util.Intents

@Composable
fun SettingsTab(
    vm: MainViewModel,
    folderAccess: FolderAccess,
    onOpenAbout: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val s = settings ?: return
    val saved by vm.saved.collectAsStateWithLifecycle()
    val access by vm.access.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dark = isAppInDarkTheme(s)
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isCustom = (saved.target as? SaveTarget.Tree)?.isDefault == false

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topInset + 8.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TabHeader(
                title = stringResource(R.string.settings_title),
                subtitle = stringResource(R.string.settings_subtitle),
            )
        }

        // ---------------------------------------------------------- appearance
        item {
            SettingsSection(stringResource(R.string.section_appearance)) {
                SettingRow(Icons.Rounded.DarkMode, stringResource(R.string.setting_theme), null) {}
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 64.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterPill(stringResource(R.string.theme_system), s.themeMode == ThemeMode.SYSTEM, { vm.setThemeMode(ThemeMode.SYSTEM) })
                    FilterPill(stringResource(R.string.theme_light), s.themeMode == ThemeMode.LIGHT, { vm.setThemeMode(ThemeMode.LIGHT) })
                    FilterPill(stringResource(R.string.theme_dark), s.themeMode == ThemeMode.DARK, { vm.setThemeMode(ThemeMode.DARK) })
                }

                SettingRow(
                    Icons.Rounded.Palette,
                    stringResource(R.string.setting_palette),
                    stringResource(s.palette.spec().nameRes),
                ) {}
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 64.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AccentPalette.entries.forEach { palette ->
                        PaletteSwatch(
                            palette = palette,
                            selected = palette == s.palette && !s.dynamicColor,
                            onClick = {
                                vm.setPalette(palette)
                                if (s.dynamicColor) vm.setDynamicColor(false)
                            },
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchRow(
                        icon = Icons.Rounded.Colorize,
                        title = stringResource(R.string.setting_dynamic),
                        subtitle = stringResource(R.string.setting_dynamic_desc),
                        checked = s.dynamicColor,
                        onChange = { vm.setDynamicColor(it) },
                    )
                }
                SwitchRow(
                    icon = Icons.Rounded.Smartphone,
                    title = stringResource(R.string.setting_amoled),
                    subtitle = stringResource(if (dark) R.string.setting_amoled_desc else R.string.setting_amoled_desc_light),
                    checked = s.amoled,
                    onChange = { vm.setAmoled(it) },
                )
                SettingRow(Icons.Rounded.GridView, stringResource(R.string.setting_grid), null) {}
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 64.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (2..4).forEach { n ->
                        FilterPill(pluralStringResource(R.plurals.grid_columns, n, n), s.gridColumns == n, { vm.setGridColumns(n) })
                    }
                }
            }
        }

        // ------------------------------------------------------------- storage
        item {
            SettingsSection(stringResource(R.string.section_storage)) {
                SettingRow(
                    icon = Icons.Rounded.Folder,
                    title = stringResource(if (isCustom) R.string.location_custom else R.string.location_default),
                    subtitle = saved.pathLabel,
                ) {}
                if (vm.needsSafForDefault && s.defaultTreeUri == null) {
                    SettingRow(
                        icon = Icons.Rounded.CreateNewFolder,
                        title = stringResource(R.string.action_setup_default),
                        subtitle = stringResource(R.string.setup_default_desc, SaveRepository.DEFAULT_FOLDER),
                        onClick = folderAccess.requestDefaultSaveFolder,
                    )
                }
                if (!vm.needsSafForDefault && !vm.hasLegacyWrite()) {
                    SettingRow(
                        icon = Icons.Rounded.LockOpen,
                        title = stringResource(R.string.action_allow_storage),
                        subtitle = stringResource(R.string.allow_storage_desc),
                        onClick = folderAccess.requestLegacyPermissions,
                    )
                }
                SettingRow(
                    icon = Icons.Rounded.DriveFolderUpload,
                    title = stringResource(R.string.action_change_folder),
                    subtitle = stringResource(R.string.change_folder_desc),
                    onClick = folderAccess.requestCustomSaveFolder,
                )
                if (isCustom) {
                    SettingRow(
                        icon = Icons.Rounded.Restore,
                        title = stringResource(R.string.action_use_default),
                        subtitle = stringResource(R.string.use_default_desc, SaveRepository.DEFAULT_FOLDER),
                        onClick = { vm.resetSaveLocation() },
                    )
                }
            }
        }

        // ------------------------------------------------------------- sources
        item {
            SettingsSection(stringResource(R.string.section_sources)) {
                WaSource.entries.forEach { source ->
                    val label = stringResource(source.labelRes)
                    val granted = access[source] == AccessState.GRANTED
                    val installed = vm.isInstalled(source)
                    SwitchRow(
                        icon = Icons.Rounded.Smartphone,
                        title = label,
                        subtitle = stringResource(
                            when {
                                !installed -> R.string.source_not_installed
                                granted -> R.string.source_access_granted
                                else -> R.string.source_access_needed
                            },
                        ),
                        checked = source in s.enabledSources,
                        onChange = { vm.setSourceEnabled(source, it) },
                    )
                    if (source in s.enabledSources) {
                        SettingRow(
                            icon = Icons.Rounded.LockOpen,
                            title = stringResource(if (granted) R.string.action_change_access else R.string.action_grant_access),
                            subtitle = null,
                            onClick = { folderAccess.requestStatusAccess(source) },
                            indent = true,
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------- automation
        item {
            SettingsSection(stringResource(R.string.section_automation)) {
                SwitchRow(
                    icon = Icons.Rounded.AutoMode,
                    title = stringResource(R.string.setting_auto_save),
                    subtitle = stringResource(R.string.setting_auto_save_desc),
                    checked = s.autoSave,
                    onChange = { vm.setAutoSave(it) },
                )
                if (s.autoSave) {
                    SwitchRow(
                        icon = Icons.Rounded.Movie,
                        title = stringResource(R.string.setting_auto_save_videos),
                        subtitle = stringResource(R.string.setting_auto_save_videos_desc),
                        checked = s.autoSaveVideos,
                        onChange = { vm.setAutoSaveVideos(it) },
                    )
                }
            }
        }

        // ------------------------------------------------------------ behavior
        item {
            SettingsSection(stringResource(R.string.section_behavior)) {
                SwitchRow(
                    icon = Icons.Rounded.Vibration,
                    title = stringResource(R.string.setting_haptics),
                    subtitle = stringResource(R.string.setting_haptics_desc),
                    checked = s.haptics,
                    onChange = { vm.setHaptics(it) },
                )
                SwitchRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = stringResource(R.string.setting_confirm_delete),
                    subtitle = stringResource(R.string.setting_confirm_delete_desc),
                    checked = s.confirmDelete,
                    onChange = { vm.setConfirmDelete(it) },
                )
            }
        }

        // --------------------------------------------------------------- about
        item {
            SettingsSection(stringResource(R.string.section_about)) {
                SettingRow(Icons.Rounded.Info, stringResource(R.string.about_title), stringResource(R.string.about_row_desc), onClick = onOpenAbout)
                SettingRow(Icons.Rounded.Policy, stringResource(R.string.privacy_title), stringResource(R.string.privacy_row_desc), onClick = onOpenPrivacy)
                SettingRow(Icons.Rounded.Star, stringResource(R.string.action_rate), stringResource(R.string.rate_desc), onClick = { Intents.rateApp(context) })
                SettingRow(Icons.Rounded.Share, stringResource(R.string.action_share_app), stringResource(R.string.share_app_desc), onClick = { Intents.shareApp(context) })
                SettingRow(Icons.Rounded.Mail, stringResource(R.string.action_feedback), stringResource(R.string.support_email), onClick = { Intents.emailSupport(context) })
            }
        }

        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                GradientText(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    pluralStringResource(R.plurals.lifetime_saves, s.totalSaved, s.totalSaved),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SectionLabel(title)
        GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 8.dp), content = content)
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    indent: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {
        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    },
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = if (indent) 64.dp else 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!indent) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (indent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    SettingRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = { onChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
            )
        },
    )
}

@Composable
private fun PaletteSwatch(palette: AccentPalette, selected: Boolean, onClick: () -> Unit) {
    val spec = palette.spec()
    val name = stringResource(spec.nameRes)
    val ring = LocalGlint.current.glassBorder
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.onSurface else ring, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(spec.gradient))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}
