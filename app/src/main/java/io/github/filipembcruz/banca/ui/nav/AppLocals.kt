package io.github.filipembcruz.banca.ui.nav

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import io.github.filipembcruz.banca.data.model.Article
import io.github.filipembcruz.banca.data.model.FeedItem
import io.github.filipembcruz.banca.data.model.Related
import io.github.filipembcruz.banca.data.settings.AppSettings
import io.github.filipembcruz.banca.ui.theme.LocalExtras

/** Everything a card or screen can ask the app to do. Defaults are no-ops (previews, screenshots). */
class AppActions(
    val openArticle: (article: Article, sharedKey: String?) -> Unit = { _, _ -> },
    val openRelated: (related: Related, parent: Article) -> Unit = { _, _ -> },
    val showMenu: (item: FeedItem) -> Unit = {},
    val toggleSave: (item: FeedItem) -> Unit = {},
    val openSettings: (route: String) -> Unit = {},
    val openSearch: (query: String?) -> Unit = {},
    val back: () -> Unit = {},
    /** Picture for an article that came without one (resolved from the publisher page). */
    val image: suspend (Article) -> String? = { it.image },
)

val LocalActions = staticCompositionLocalOf { AppActions() }
val LocalSettings = compositionLocalOf { AppSettings(onboarded = true) }
val LocalSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Shared-element bounds for article pictures; no-op when there is no transition scope. */
@Composable
fun Modifier.sharedImage(key: String?): Modifier {
    val shared = LocalSharedScope.current
    val anim = LocalAnimScope.current
    if (key == null || shared == null || anim == null || LocalExtras.current.reducedMotion) return this
    return with(shared) {
        this@sharedImage.sharedElement(
            state = rememberSharedContentState(key = key),
            animatedVisibilityScope = anim,
        )
    }
}
