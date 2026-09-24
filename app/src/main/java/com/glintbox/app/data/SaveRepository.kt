package com.glintbox.app.data

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.glintbox.app.R
import com.glintbox.app.data.model.AppSettings
import com.glintbox.app.data.model.SaveResult
import com.glintbox.app.data.model.SaveTarget
import com.glintbox.app.data.model.StatusMedia
import com.glintbox.app.data.storage.MediaFiles
import com.glintbox.app.data.storage.Saf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Writes statuses to the chosen destination and lists what has been saved.
 *
 * Default destination is "Internal storage/WhatsApp Statuses":
 *  - Android 8–10: written directly (WRITE_EXTERNAL_STORAGE, maxSdk 29).
 *  - Android 11+: apps may not create top-level folders without "All files access" (not allowed
 *    for this app category on Play), so the user creates/approves the folder once in the system
 *    picker. Until then files go to "Download/WhatsApp Statuses" through MediaStore.
 */
class SaveRepository(private val context: Context) {

    companion object {
        const val DEFAULT_FOLDER = "WhatsApp Statuses"
        private val FALLBACK_RELATIVE_PATH = "${Environment.DIRECTORY_DOWNLOADS}/$DEFAULT_FOLDER/"
    }

    private val resolver get() = context.contentResolver

    val needsSafForDefault: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun hasLegacyWritePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    fun legacyDefaultDir(): File = File(Environment.getExternalStorageDirectory(), DEFAULT_FOLDER)

    fun isDefaultFolderTree(uri: Uri): Boolean =
        Saf.treeDocumentId(uri).equals("primary:$DEFAULT_FOLDER", ignoreCase = true)

    fun resolveTarget(settings: AppSettings): SaveTarget {
        val custom = settings.customTreeUri?.toUri()
        if (settings.useCustomLocation && custom != null && Saf.hasPersisted(context, custom, needWrite = true)) {
            return SaveTarget.Tree(custom, isDefault = false)
        }
        if (!needsSafForDefault) return SaveTarget.LegacyFolder(legacyDefaultDir())
        val default = settings.defaultTreeUri?.toUri()
        if (default != null && Saf.hasPersisted(context, default, needWrite = true)) {
            return SaveTarget.Tree(default, isDefault = true)
        }
        return SaveTarget.MediaStoreFallback
    }

    fun describe(target: SaveTarget): String {
        val internal = context.getString(R.string.storage_internal)
        return when (target) {
            is SaveTarget.Tree -> Saf.humanPath(target.uri, internal, context.getString(R.string.storage_sd))
            is SaveTarget.LegacyFolder -> "$internal/$DEFAULT_FOLDER"
            SaveTarget.MediaStoreFallback -> "$internal/${Environment.DIRECTORY_DOWNLOADS}/$DEFAULT_FOLDER"
        }
    }

    // ---------------------------------------------------------------- listing

    suspend fun listSaved(target: SaveTarget): List<StatusMedia> = withContext(Dispatchers.IO) {
        runCatching {
            when (target) {
                is SaveTarget.Tree -> listTree(target.uri)
                is SaveTarget.LegacyFolder -> listLegacy(target.dir)
                SaveTarget.MediaStoreFallback ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listMediaStore() else emptyList()
            }.sortedByDescending { it.modified }
        }.getOrDefault(emptyList())
    }

    private fun listTree(tree: Uri): List<StatusMedia> {
        val rootId = Saf.treeDocumentId(tree) ?: return emptyList()
        return Saf.listChildren(context, tree, rootId).mapNotNull { doc ->
            if (doc.isDirectory) return@mapNotNull null
            val type = MediaFiles.typeOf(doc.name, doc.mimeType) ?: return@mapNotNull null
            StatusMedia(
                uri = DocumentsContract.buildDocumentUriUsingTree(tree, doc.documentId),
                name = doc.name,
                type = type,
                size = doc.size,
                modified = doc.lastModified,
                source = null,
            )
        }
    }

    private fun listLegacy(dir: File): List<StatusMedia> =
        dir.listFiles().orEmpty().mapNotNull { file ->
            if (!file.isFile) return@mapNotNull null
            val type = MediaFiles.typeOf(file.name) ?: return@mapNotNull null
            StatusMedia(Uri.fromFile(file), file.name, type, file.length(), file.lastModified(), null)
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun listMediaStore(): List<StatusMedia> {
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
        val result = ArrayList<StatusMedia>()
        resolver.query(
            collection,
            projection,
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
            arrayOf(FALLBACK_RELATIVE_PATH),
            null,
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val sizeIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val modIdx = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            while (c.moveToNext()) {
                val name = c.getString(nameIdx) ?: continue
                val type = MediaFiles.typeOf(name, c.getString(mimeIdx)) ?: continue
                result += StatusMedia(
                    uri = ContentUris.withAppendedId(collection, c.getLong(idIdx)),
                    name = name,
                    type = type,
                    size = c.getLong(sizeIdx),
                    modified = c.getLong(modIdx) * 1000L,
                    source = null,
                )
            }
        }
        return result
    }

    // ----------------------------------------------------------------- saving

    suspend fun save(item: StatusMedia, target: SaveTarget, existingNames: Set<String>): SaveResult =
        withContext(Dispatchers.IO) {
            if (item.name in existingNames) return@withContext SaveResult.AlreadySaved
            runCatching {
                val mime = MediaFiles.mimeOf(item.name, item.type)
                val uri = when (target) {
                    is SaveTarget.Tree -> saveToTree(item, target.uri, mime)
                    is SaveTarget.LegacyFolder -> saveLegacy(item, target.dir, mime)
                    SaveTarget.MediaStoreFallback ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) saveToMediaStore(item, mime)
                        else throw IOException("MediaStore saving requires Android 10+")
                }
                SaveResult.Saved(uri)
            }.getOrElse { SaveResult.Failed(it) }
        }

    private fun copy(from: Uri, to: Uri) {
        val input = resolver.openInputStream(from) ?: throw IOException("Cannot read $from")
        input.use { src ->
            val output = resolver.openOutputStream(to, "w") ?: throw IOException("Cannot write $to")
            output.use { src.copyTo(it, bufferSize = 64 * 1024) }
        }
    }

    private fun saveToTree(item: StatusMedia, tree: Uri, mime: String): Uri {
        val rootId = Saf.treeDocumentId(tree) ?: throw IOException("Invalid folder")
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, rootId)
        val dest = DocumentsContract.createDocument(resolver, parent, mime, item.name)
            ?: throw IOException("Cannot create file")
        try {
            copy(item.uri, dest)
        } catch (e: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, dest) }
            throw e
        }
        return dest
    }

    private fun saveLegacy(item: StatusMedia, dir: File, mime: String): Uri {
        if (!dir.exists() && !dir.mkdirs()) throw IOException("Cannot create ${dir.path}")
        val file = File(dir, item.name)
        try {
            val input = resolver.openInputStream(item.uri) ?: throw IOException("Cannot read")
            input.use { src -> file.outputStream().use { src.copyTo(it, 64 * 1024) } }
            file.setLastModified(System.currentTimeMillis())
        } catch (e: Exception) {
            file.delete()
            throw e
        }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mime), null)
        return Uri.fromFile(file)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToMediaStore(item: StatusMedia, mime: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, item.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, FALLBACK_RELATIVE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val dest = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("MediaStore insert failed")
        try {
            copy(item.uri, dest)
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(dest, values, null, null)
        } catch (e: Exception) {
            resolver.delete(dest, null, null)
            throw e
        }
        return dest
    }

    // --------------------------------------------------------------- deleting

    suspend fun delete(item: StatusMedia, target: SaveTarget): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            when (target) {
                is SaveTarget.Tree -> DocumentsContract.deleteDocument(resolver, item.uri)
                is SaveTarget.LegacyFolder -> {
                    val file = item.uri.path?.let(::File)
                    val ok = file?.delete() == true
                    if (ok) MediaScannerConnection.scanFile(context, arrayOf(file!!.absolutePath), null, null)
                    ok
                }
                SaveTarget.MediaStoreFallback -> resolver.delete(item.uri, null, null) > 0
            }
        }.getOrDefault(false)
    }
}
