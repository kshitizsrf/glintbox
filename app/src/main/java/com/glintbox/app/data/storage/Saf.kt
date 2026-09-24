package com.glintbox.app.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import com.glintbox.app.data.model.WaSource
import java.io.File

/** Helpers around the Storage Access Framework (the only Play-compliant way to reach these folders on Android 11+). */
object Saf {

    const val EXTERNAL_AUTHORITY = "com.android.externalstorage.documents"
    private const val PRIMARY = "primary"

    data class Doc(
        val documentId: String,
        val name: String,
        val mimeType: String,
        val size: Long,
        val lastModified: Long,
    ) {
        val isDirectory: Boolean get() = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
    }

    fun documentUri(documentId: String): Uri =
        DocumentsContract.buildDocumentUri(EXTERNAL_AUTHORITY, documentId)

    /** Opens the picker right inside the .Statuses folder of [source]. */
    fun statusesInitialUri(source: WaSource): Uri = documentUri("$PRIMARY:${source.scopedPath}")

    /** Opens the picker at "Internal storage/WhatsApp Statuses", or at the storage root if it does not exist yet. */
    fun defaultFolderInitialUri(folderName: String): Uri {
        @Suppress("DEPRECATION")
        val exists = runCatching { File(Environment.getExternalStorageDirectory(), folderName).isDirectory }
            .getOrDefault(false)
        return if (exists) documentUri("$PRIMARY:$folderName") else documentUri("$PRIMARY:")
    }

    fun treeDocumentId(treeUri: Uri): String? = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()

    fun hasPersisted(context: Context, uri: Uri, needWrite: Boolean = false): Boolean =
        context.contentResolver.persistedUriPermissions.any { p ->
            p.uri == uri && p.isReadPermission && (!needWrite || p.isWritePermission)
        }

    fun persist(context: Context, uri: Uri, write: Boolean): Boolean = runCatching {
        var flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        if (write) flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, flags)
        true
    }.getOrDefault(false)

    fun release(context: Context, uri: Uri) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        runCatching {
            context.contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /** Lists direct children using a single cursor query (much faster than DocumentFile.listFiles()). */
    fun listChildren(context: Context, treeUri: Uri, parentDocumentId: String): List<Doc> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val result = ArrayList<Doc>()
        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            val modIdx = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            while (c.moveToNext()) {
                result += Doc(
                    documentId = c.getString(idIdx) ?: continue,
                    name = c.getString(nameIdx) ?: continue,
                    mimeType = c.getString(mimeIdx).orEmpty(),
                    size = if (c.isNull(sizeIdx)) 0L else c.getLong(sizeIdx),
                    lastModified = if (c.isNull(modIdx)) 0L else c.getLong(modIdx),
                )
            }
        }
        return result
    }

    /** "primary:WhatsApp Statuses" -> "Internal storage/WhatsApp Statuses". */
    fun humanPath(treeUri: Uri, internalLabel: String, sdLabel: String): String {
        val docId = treeDocumentId(treeUri) ?: return treeUri.lastPathSegment.orEmpty()
        val volume = docId.substringBefore(':')
        val path = docId.substringAfter(':', "")
        val root = if (volume.equals(PRIMARY, ignoreCase = true)) internalLabel else sdLabel
        return if (path.isEmpty()) root else "$root/$path"
    }
}
