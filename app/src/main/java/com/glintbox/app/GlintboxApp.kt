package com.glintbox.app

import android.app.Application
import android.content.Context
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.glintbox.app.data.SaveRepository
import com.glintbox.app.data.SettingsRepository
import com.glintbox.app.data.StatusRepository

class GlintboxApp : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(AnimatedImageDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.25).build() }
            .crossfade(true)
            .build()
}

/** Tiny manual DI container — the app is small enough not to need a DI framework. */
class AppContainer(context: Context) {
    val settings = SettingsRepository(context)
    val statuses = StatusRepository(context)
    val saves = SaveRepository(context)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as GlintboxApp).container
