package io.github.filipembcruz.banca.core

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import io.github.filipembcruz.banca.data.db.BancaDb
import io.github.filipembcruz.banca.data.db.Store
import io.github.filipembcruz.banca.data.net.Http
import io.github.filipembcruz.banca.data.net.LinkResolver
import io.github.filipembcruz.banca.data.net.MarketApi
import io.github.filipembcruz.banca.data.net.WeatherApi
import io.github.filipembcruz.banca.data.repo.DataRepository
import io.github.filipembcruz.banca.data.repo.NewsRepository
import io.github.filipembcruz.banca.data.settings.SettingsStore
import io.github.filipembcruz.banca.work.Notifier
import io.github.filipembcruz.banca.work.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val http = Http(context)
    val store = Store(BancaDb(context))
    val settings = SettingsStore(context)
    val resolver = LinkResolver(http)
    val news = NewsRepository(store, settings, http, resolver)
    val data = DataRepository(store, WeatherApi(http), MarketApi(http))
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

class BancaApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifier.createChannels(this)
        // Keep background work in step with the notification and refresh settings.
        container.scope.launch {
            container.settings.flow
                .map { s -> listOf(s.notifications, s.refreshMinutes, s.wifiOnly, s.chargingOnly, s.pauseOnSaver, s.digest, s.digestTimes, s.rules.size) }
                .distinctUntilChanged()
                .collect { runCatching { WorkScheduler.sync(this@BancaApp, container.settings.current()) } }
        }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient(container.http.client)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.2).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(120L * 1024 * 1024).build() }
        .crossfade(250)
        .respectCacheHeaders(false)
        .build()
}

val Context.container: AppContainer get() = (applicationContext as BancaApp).container
