package com.glintbox.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.glintbox.app.data.model.AccessState
import com.glintbox.app.data.model.AppSettings
import com.glintbox.app.data.model.StatusMedia
import com.glintbox.app.data.model.WaSource
import com.glintbox.app.data.storage.MediaFiles
import com.glintbox.app.data.storage.Saf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class StatusFolderMissingException : IllegalStateException("Status folder not found")

/**
 * Reads statuses the messenger has already downloaded to the device.
 * Android 8–10: plain file access. Android 11+: a folder the user granted via the system picker.
 */
class StatusRepository(private val context: Context) {

    /** tree uri -> document id of its .Statuses folder */
    private val resolvedDirs = ConcurrentHashMap<String, String>()

    val usesSaf: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun hasLegacyPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED

    fun isInstalled(source: WaSource): Boolean = runCatching {
        context.packageManager.getPackageInfo(source.packageName, 0)
        true
    }.getOrDefault(false)

    fun accessState(source: WaSource, settings: AppSettings): AccessState {
        if (!usesSaf) return if (hasLegacyPermission()) AccessState.GRANTED else AccessState.NEEDS_ACCESS
        val uri = settings.statusTreeUris[source]?.toUri() ?: return AccessState.NEEDS_ACCESS
        return if (Saf.hasPersisted(context, uri)) AccessState.GRANTED else AccessState.NEEDS_ACCESS
    }

    suspend fun load(source: WaSource, settings: AppSettings): Result<List<StatusMedia>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val items = if (usesSaf) {
                    val tree = settings.statusTreeUris[source]?.toUri() ?: error("No access")
                    loadFromTree(tree, source)
                } else {
                    loadLegacy(source)
                }
                items.sortedByDescending { it.modified }
            }
        }

    @Suppress("DEPRECATION")
    private fun loadLegacy(source: WaSource): List<StatusMedia> {
        val root = Environment.getExternalStorageDirectory()
        val dirs = source.statusPaths.map { File(root, it) }.filter { it.isDirectory }
        if (dirs.isEmpty()) throw StatusFolderMissingException()
        return dirs.asSequence()
            .flatMap { it.listFiles()?.asSequence() ?: emptySequence() }
            .filter { it.isFile && it.length() > 0 }
            .mapNotNull { file ->
                val type = MediaFiles.typeOf(file.name) ?: return@mapNotNull null
                StatusMedia(
                    uri = Uri.fromFile(file),
                    name = file.name,
                    type = type,
                    size = file.length(),
                    modified = file.lastModified(),
                    source = source,
                )
            }
            .distinctBy { it.name }
            .toList()
    }

    private fun loadFromTree(tree: Uri, source: WaSource): List<StatusMedia> {
        val dirId = resolveStatusesDir(tree) ?: throw StatusFolderMissingException()
        return Saf.listChildren(context, tree, dirId).mapNotNull { doc ->
            if (doc.isDirectory || doc.size <= 0) return@mapNotNull null
            val type = MediaFiles.typeOf(doc.name, doc.mimeType) ?: return@mapNotNull null
            StatusMedia(
                uri = DocumentsContract.buildDocumentUriUsingTree(tree, doc.documentId),
                name = doc.name,
                type = type,
                size = doc.size,
                modified = doc.lastModified,
                source = source,
            )
        }
    }

    /**
     * Finds the .Statuses folder inside a granted tree. Users sometimes pick a parent
     * (e.g. "Media" or "com.whatsapp"), so we walk down known folder names.
     */
    fun resolveStatusesDir(tree: Uri): String? {
        resolvedDirs[tree.toString()]?.let { return it }
        val rootId = Saf.treeDocumentId(tree) ?: return null
        val found = if (rootId.substringAfterLast('/').substringAfterLast(':') == STATUSES) {
            rootId
        } else {
            search(tree, rootId, depth = 0)
        }
        found?.let { resolvedDirs[tree.toString()] = it }
        return found
    }

    private fun search(tree: Uri, docId: String, depth: Int): String? {
        if (depth > 5) return null
        val children = runCatching { Saf.listChildren(context, tree, docId) }.getOrNull() ?: return null
        children.firstOrNull { it.isDirectory && it.name == STATUSES }?.let { return it.documentId }
        for (child in children) {
            if (child.isDirectory && child.name in PATH_HINTS) {
                search(tree, child.documentId, depth + 1)?.let { return it }
            }
        }
        return null
    }

    fun forget(tree: Uri) {
        resolvedDirs.remove(tree.toString())
    }

    private companion object {
        const val STATUSES = ".Statuses"
        val PATH_HINTS = setOf(
            "Android", "media", "com.whatsapp", "com.whatsapp.w4b",
            "WhatsApp", "WhatsApp Business", "Media",
        )
    }
}
