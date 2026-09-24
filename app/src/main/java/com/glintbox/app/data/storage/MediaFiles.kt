package com.glintbox.app.data.storage

import android.webkit.MimeTypeMap
import com.glintbox.app.data.model.MediaType

object MediaFiles {
    private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "heif")
    private val VIDEO_EXT = setOf("mp4", "3gp", "mkv", "webm", "mov", "m4v")

    fun typeOf(name: String, mime: String? = null): MediaType? {
        if (name.startsWith(".")) return null // .nomedia and other hidden files
        when {
            mime?.startsWith("image/") == true -> return MediaType.IMAGE
            mime?.startsWith("video/") == true -> return MediaType.VIDEO
        }
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            in IMAGE_EXT -> MediaType.IMAGE
            in VIDEO_EXT -> MediaType.VIDEO
            else -> null
        }
    }

    fun mimeOf(name: String, type: MediaType): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: if (type == MediaType.VIDEO) "video/mp4" else "image/jpeg"
    }
}
