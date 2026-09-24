package com.glintbox.app.ui.onboarding

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DriveFolderUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.glintbox.app.R
import com.glintbox.app.data.SaveRepository
import com.glintbox.app.data.model.AccessState
import com.glintbox.app.data.model.SaveTarget
import com.glintbox.app.data.model.WaSource
import com.glintbox.app.ui.MainViewModel
import com.glintbox.app.ui.common.rememberFolderAccess
import com.glintbox.app.ui.components.AuroraBackground
import com.glintbox.app.ui.components.GlassButton
import com.glintbox.app.ui.components.GlassCard
import com.glintbox.app.ui.components.GlowIcon
import com.glintbox.app.ui.components.GradientButton
import com.glintbox.app.ui.components.GradientText
import com.glintbox.app.ui.theme.LocalGlint
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 4

@Composable
fun OnboardingScreen(
    vm: MainViewModel,
    onFinish: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val pager = rememberPagerState { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val folderAccess = rememberFolderAccess(vm)
    val access by vm.access.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    AuroraBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    when (page) {
                        0 -> WelcomePage()
                        1 -> RespectPage(onOpenPrivacy)
                        2 -> AccessPage(
                            sources = settings?.enabledSources.orEmpty().sortedBy { it.ordinal },
                            access = access,
                            isInstalled = vm::isInstalled,
                            onGrant = folderAccess.requestStatusAccess,
                        )
                        else -> SaveLocationPage(
                            pathLabel = saved.pathLabel,
                            target = saved.target,
                            needsSaf = vm.needsSafForDefault,
                            onSetupDefault = folderAccess.requestDefaultSaveFolder,
                            onChooseCustom = folderAccess.requestCustomSaveFolder,
                        )
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PageDots(count = PAGE_COUNT, current = pager.currentPage, modifier = Modifier.weight(1f))
                val last = pager.currentPage == PAGE_COUNT - 1
                GradientButton(
                    text = stringResource(
                        when {
                            last -> R.string.action_get_started
                            pager.currentPage == 1 -> R.string.action_i_agree
                            else -> R.string.action_next
                        },
                    ),
                    onClick = {
                        if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                    },
                    modifier = Modifier.animateContentSize(),
                )
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    val glint = LocalGlint.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 26.dp else 8.dp, label = "dot")
            Box(
                Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (i == current) Brush.horizontalGradient(glint.gradient)
                        else Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        GlowIcon(Icons.Rounded.AutoAwesome, size = 96.dp)
        Spacer(Modifier.height(28.dp))
        GradientText(stringResource(R.string.app_name), style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_tagline),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        FeatureRow(Icons.Rounded.VideoLibrary, R.string.feature_media_title, R.string.feature_media_desc)
        FeatureRow(Icons.Rounded.AutoMode, R.string.feature_auto_title, R.string.feature_auto_desc)
        FeatureRow(Icons.Rounded.Folder, R.string.feature_folder_title, R.string.feature_folder_desc)
        FeatureRow(Icons.Rounded.Palette, R.string.feature_theme_title, R.string.feature_theme_desc)
        FeatureRow(Icons.Rounded.WifiOff, R.string.feature_private_title, R.string.feature_private_desc)
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: Int, desc: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RespectPage(onOpenPrivacy: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        GlowIcon(Icons.Rounded.Shield, size = 72.dp)
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.respect_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Bullet(Icons.Rounded.Gavel, stringResource(R.string.respect_point_affiliation))
            Bullet(Icons.Rounded.Shield, stringResource(R.string.respect_point_consent))
            Bullet(Icons.Rounded.PhoneAndroid, stringResource(R.string.respect_point_local))
            Bullet(Icons.Rounded.WifiOff, stringResource(R.string.respect_point_offline))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.read_privacy_policy),
            style = MaterialTheme.typography.labelLarge.copy(textDecoration = TextDecoration.Underline),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onOpenPrivacy)
                .padding(8.dp),
        )
    }
}

@Composable
private fun Bullet(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AccessPage(
    sources: List<WaSource>,
    access: Map<WaSource, AccessState>,
    isInstalled: (WaSource) -> Boolean,
    onGrant: (WaSource) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        GlowIcon(Icons.Rounded.LockOpen, size = 72.dp)
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.onboarding_access_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_access_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        sources.forEach { source ->
            val granted = access[source] == AccessState.GRANTED
            val installed = isInstalled(source)
            GlassCard(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(source.labelRes), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(
                                when {
                                    granted -> R.string.source_access_granted
                                    !installed -> R.string.source_not_installed
                                    else -> R.string.source_access_needed
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (granted) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = stringResource(R.string.source_access_granted),
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(30.dp),
                        )
                    } else {
                        GradientButton(
                            text = stringResource(R.string.action_allow),
                            onClick = { onGrant(source) },
                            height = 44.dp,
                        )
                    }
                }
            }
        }
        Text(
            stringResource(R.string.onboarding_access_skip),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SaveLocationPage(
    pathLabel: String,
    target: SaveTarget?,
    needsSaf: Boolean,
    onSetupDefault: () -> Unit,
    onChooseCustom: () -> Unit,
) {
    val defaultReady = target is SaveTarget.LegacyFolder || (target as? SaveTarget.Tree)?.isDefault == true
    Column(Modifier.fillMaxWidth()) {
        GlowIcon(Icons.Rounded.Folder, size = 72.dp)
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.onboarding_save_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboarding_save_desc, SaveRepository.DEFAULT_FOLDER),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Text(
                stringResource(R.string.current_location),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(pathLabel, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            if (needsSaf && !defaultReady) {
                GradientButton(
                    text = stringResource(R.string.action_setup_default),
                    onClick = onSetupDefault,
                    icon = Icons.Rounded.CreateNewFolder,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
            }
            GlassButton(
                text = stringResource(R.string.action_choose_other_folder),
                onClick = onChooseCustom,
                icon = Icons.Rounded.DriveFolderUpload,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.onboarding_save_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
