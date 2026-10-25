package io.github.filipembcruz.banca.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.filipembcruz.banca.core.LocaleHelper
import io.github.filipembcruz.banca.ui.nav.BancaNav
import io.github.filipembcruz.banca.ui.theme.BancaTheme
import io.github.filipembcruz.banca.ui.theme.LocalExtras
import io.github.filipembcruz.banca.work.Notifier

class MainActivity : ComponentActivity() {

    private val app: AppViewModel by viewModels()

    /** Article requested by a notification tap; consumed by the navigation host. */
    val pendingArticle = mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val settings by app.settings.collectAsStateWithLifecycle()
            val s = settings
            // Settings load in a few milliseconds; the window background shows meanwhile.
            if (s != null) BancaTheme(mode = s.theme, amoled = s.amoled, dynamic = s.dynamicColor, reducedMotion = s.reducedMotion) {
                val dark = LocalExtras.current.dark
                val bg = MaterialTheme.colorScheme.background
                LaunchedEffect(dark, bg) {
                    enableEdgeToEdge(
                        statusBarStyle = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                        navigationBarStyle = if (dark) SystemBarStyle.dark(bg.toArgb())
                        else SystemBarStyle.light(bg.toArgb(), bg.toArgb()),
                    )
                }
                Box(Modifier.fillMaxSize().background(bg)) {
                    BancaNav(app, s, pendingArticle)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val id = intent?.getStringExtra(Notifier.EXTRA_ARTICLE) ?: return
        pendingArticle.value = id
        intent.removeExtra(Notifier.EXTRA_ARTICLE)
    }
}
