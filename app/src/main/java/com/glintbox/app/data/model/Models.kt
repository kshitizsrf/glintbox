package com.glintbox.app.data.model

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.glintbox.app.R
import java.io.File

/** Messenger apps whose statuses Glintbox can read. */
enum class WaSource(
    val packageName: String,
    @param:StringRes val labelRes: Int,
    /** Status folder paths relative to the primary storage root, newest layout first. */
    val statusPaths: List<String>,
) {
    WHATSAPP(
        packageName = "com.whatsapp",
        labelRes = R.string.source_whatsapp,
        statusPaths = listOf(
            "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
            "WhatsApp/Media/.Statuses",
        ),
    ),
    BUSINESS(
        packageName = "com.whatsapp.w4b",
        labelRes = R.string.source_business,
        statusPaths = listOf(
            "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
            "WhatsApp Business/Media/.Statuses",
        ),
    );

    /** Folder shown first in the system picker on Android 11+. */
    val scopedPath: String get() = statusPaths.first()
}

enum class MediaType { IMAGE, VIDEO }

/** A status (from a messenger) or a file already saved by Glintbox. */
@Immutable
data class StatusMedia(
    val uri: Uri,
    val name: String,
    val type: MediaType,
    val size: Long,
    val modified: Long,
    /** Null for items in the saved gallery. */
    val source: WaSource?,
) {
    val id: String get() = uri.toString()
    val isVideo: Boolean get() = type == MediaType.VIDEO
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentPalette { AURORA, EMBER, LAGOON, ORCHID, JADE, MIDAS }

enum class MediaFilter { ALL, IMAGES, VIDEOS }

@Immutable
data class AppSettings(
    val onboardingDone: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val palette: AccentPalette = AccentPalette.AURORA,
    val dynamicColor: Boolean = false,
    val amoled: Boolean = false,
    val gridColumns: Int = 3,
    val haptics: Boolean = true,
    val confirmDelete: Boolean = true,
    val enabledSources: Set<WaSource> = WaSource.entries.toSet(),
    val activeSource: WaSource = WaSource.WHATSAPP,
    /** Android 11+: persisted SAF tree per source pointing at (or above) the .Statuses folder. */
    val statusTreeUris: Map<WaSource, String> = emptyMap(),
    /** Android 11+: SAF grant for "Internal storage/WhatsApp Statuses". */
    val defaultTreeUri: String? = null,
    /** Any user-chosen folder. */
    val customTreeUri: String? = null,
    val useCustomLocation: Boolean = false,
    val autoSave: Boolean = false,
    val autoSaveVideos: Boolean = true,
    val totalSaved: Int = 0,
)

/** Where saved statuses are written. */
sealed interface SaveTarget {
    /** A folder granted through the Storage Access Framework. */
    data class Tree(val uri: Uri, val isDefault: Boolean) : SaveTarget

    /** Android 8–10: direct file access to Internal storage/WhatsApp Statuses. */
    data class LegacyFolder(val dir: File) : SaveTarget

    /** Android 11+ before the default folder is granted: Download/WhatsApp Statuses via MediaStore. */
    data object MediaStoreFallback : SaveTarget
}

sealed interface SaveResult {
    data class Saved(val uri: Uri) : SaveResult
    data object AlreadySaved : SaveResult
    data class Failed(val error: Throwable) : SaveResult
}

enum class AccessState { GRANTED, NEEDS_ACCESS, FOLDER_MISSING }
