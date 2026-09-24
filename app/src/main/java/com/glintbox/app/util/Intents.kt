package com.glintbox.app.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.glintbox.app.BuildConfig
import com.glintbox.app.R
import com.glintbox.app.data.model.MediaType
import com.glintbox.app.data.model.StatusMedia
import java.io.File

object Intents {

    private fun shareable(context: Context, uri: Uri): Uri =
        if (uri.scheme == "file") {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(requireNotNull(uri.path)))
        } else {
            uri
        }

    /** Shares one or more media items. [targetPackage] limits it to one app (e.g. "repost"). */
    fun share(context: Context, items: List<StatusMedia>, targetPackage: String? = null): Boolean {
        if (items.isEmpty()) return false
        val uris = items.map { shareable(context, it.uri) }
        val mime = when {
            items.all { it.type == MediaType.IMAGE } -> "image/*"
            items.all { it.type == MediaType.VIDEO } -> "video/*"
            else -> "*/*"
        }
        val base = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        val intent = base.apply {
            type = mime
            clipData = ClipData.newRawUri(null, uris.first()).also { clip ->
                uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (targetPackage != null) setPackage(targetPackage)
        }
        return try {
            val launch = if (targetPackage != null) intent
            else Intent.createChooser(intent, context.getString(R.string.share_via))
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        return runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    }

    fun rateApp(context: Context) {
        val id = context.packageName.removeSuffix(".debug")
        val market = Intent(Intent.ACTION_VIEW, "market://details?id=$id".toUri())
        val web = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$id".toUri())
        runCatching { context.startActivity(market.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { runCatching { context.startActivity(web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }

    fun shareApp(context: Context) {
        val id = context.packageName.removeSuffix(".debug")
        val text = context.getString(R.string.share_app_text, "https://play.google.com/store/apps/details?id=$id")
        val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        runCatching {
            context.startActivity(
                Intent.createChooser(intent, context.getString(R.string.share_via)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun emailSupport(context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(context.getString(R.string.support_email)))
            .putExtra(Intent.EXTRA_SUBJECT, "Glintbox ${BuildConfig.VERSION_NAME} feedback")
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}
