package com.glintbox.app.ui.about

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.glintbox.app.BuildConfig
import com.glintbox.app.R
import com.glintbox.app.ui.components.AuroraBackground
import com.glintbox.app.ui.components.GlassCard
import com.glintbox.app.ui.components.GlowIcon
import com.glintbox.app.ui.components.GradientText
import com.glintbox.app.ui.home.CircleIconButton

@Composable
private fun DocScreen(
    icon: ImageVector,
    title: String,
    subtitle: String,
    sections: List<Pair<Int, Int>>,
    onBack: () -> Unit,
) {
    AuroraBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), onBack)
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlowIcon(icon, size = 56.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    GradientText(title, style = MaterialTheme.typography.headlineMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            sections.forEach { (heading, body) -> DocSection(heading, body) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DocSection(@StringRes heading: Int, @StringRes body: Int) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text(stringResource(heading), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    DocScreen(
        icon = Icons.Rounded.AutoAwesome,
        title = stringResource(R.string.about_title),
        subtitle = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
        sections = listOf(
            R.string.about_what_title to R.string.about_what_body,
            R.string.about_disclaimer_title to R.string.about_disclaimer_body,
            R.string.about_content_title to R.string.about_content_body,
            R.string.about_how_title to R.string.about_how_body,
            R.string.about_permissions_title to R.string.about_permissions_body,
        ),
        onBack = onBack,
    )
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    DocScreen(
        icon = Icons.Rounded.Policy,
        title = stringResource(R.string.privacy_title),
        subtitle = stringResource(R.string.privacy_updated),
        sections = listOf(
            R.string.privacy_summary_title to R.string.privacy_summary_body,
            R.string.privacy_collect_title to R.string.privacy_collect_body,
            R.string.privacy_access_title to R.string.privacy_access_body,
            R.string.privacy_share_title to R.string.privacy_share_body,
            R.string.privacy_children_title to R.string.privacy_children_body,
            R.string.privacy_changes_title to R.string.privacy_changes_body,
            R.string.privacy_contact_title to R.string.privacy_contact_body,
        ),
        onBack = onBack,
    )
}
