package com.glintbox.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import coil3.video.videoFrameMillis
import com.glintbox.app.R
import com.glintbox.app.data.model.StatusMedia
import com.glintbox.app.ui.theme.LocalGlint
import com.glintbox.app.util.relativeTime

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StatusTile(
    item: StatusMedia,
    isSaved: Boolean,
    isSaving: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    showQuickSave: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onQuickSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glint = LocalGlint.current
    val context = LocalContext.current
    val shape = RoundedCornerShape(22.dp)
    val scale by animateFloatAsState(if (selected) 0.92f else 1f, label = "tile-scale")
    val description = stringResource(
        if (item.isVideo) R.string.cd_video_status else R.string.cd_image_status,
        relativeTime(item.modified),
    )

    Box(
        modifier = modifier
            .aspectRatio(0.78f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(
                if (selected) Modifier.border(3.dp, Brush.linearGradient(glint.gradient), shape)
                else Modifier.border(1.dp, glint.glassBorder, shape),
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = description },
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .size(Size(480, 620))
                .videoFrameMillis(1_000)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Bottom scrim for legibility
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))),
        )

        if (item.isVideo) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Text(stringResource(R.string.badge_video), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }

        Text(
            text = relativeTime(item.modified),
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, bottom = 10.dp),
        )

        if (showQuickSave && !selectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSaved) Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
                        else Brush.linearGradient(glint.gradient),
                    )
                    .clickable(enabled = !isSaving, role = Role.Button, onClick = onQuickSave),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isSaving -> CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    isSaved -> Icon(Icons.Rounded.DownloadDone, stringResource(R.string.cd_saved), tint = Color.White, modifier = Modifier.size(20.dp))
                    else -> Icon(Icons.Rounded.Download, stringResource(R.string.action_save), tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        AnimatedVisibility(
            visible = selectionMode,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (selected) Color.White else Color.Black.copy(alpha = 0.35f))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = stringResource(R.string.cd_selected),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}
