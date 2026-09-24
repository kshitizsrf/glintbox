package com.glintbox.app.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.glintbox.app.R
import com.glintbox.app.ui.MainViewModel
import com.glintbox.app.ui.common.rememberFolderAccess
import com.glintbox.app.ui.components.AuroraBackground
import com.glintbox.app.ui.theme.LocalGlint

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenViewer: (index: Int, savedMode: Boolean) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val folderAccess = rememberFolderAccess(vm)

    // Registered first so that tab-level handlers (e.g. clearing a selection) take priority.
    BackHandler(enabled = tab != 0) { tab = 0 }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    AuroraBackground {
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
            label = "home-tabs",
        ) { current ->
            when (current) {
                0 -> StatusesTab(vm, folderAccess, onOpenViewer)
                1 -> SavedTab(vm, folderAccess, onOpenViewer)
                else -> SettingsTab(vm, folderAccess, onOpenAbout, onOpenPrivacy)
            }
        }
        GlintNavBar(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private data class NavEntry(val icon: ImageVector, val label: Int)

private val NavEntries = listOf(
    NavEntry(Icons.Rounded.AutoAwesome, R.string.tab_statuses),
    NavEntry(Icons.Rounded.Collections, R.string.tab_saved),
    NavEntry(Icons.Rounded.Tune, R.string.tab_settings),
)

@Composable
private fun GlintNavBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val glint = LocalGlint.current
    val shape = RoundedCornerShape(32.dp)
    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .shadow(24.dp, shape, ambientColor = glint.gradient.first(), spotColor = glint.gradient.first())
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (glint.isDark) 0.92f else 0.96f))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavEntries.forEachIndexed { index, entry ->
            val isSelected = index == selected
            val label = stringResource(entry.label)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        if (isSelected) Brush.horizontalGradient(glint.gradient)
                        else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                    )
                    .clickable(role = Role.Tab) { onSelect(index) }
                    .semantics {
                        this.selected = isSelected
                        contentDescription = label
                    }
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    entry.icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    Row {
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
