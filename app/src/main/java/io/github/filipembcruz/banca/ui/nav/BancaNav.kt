package io.github.filipembcruz.banca.ui.nav

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import io.github.filipembcruz.banca.ui.inbox.InboxScreen
import io.github.filipembcruz.banca.ui.inbox.InboxViewModel
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.data.model.FeedItem
import io.github.filipembcruz.banca.data.settings.AppSettings
import io.github.filipembcruz.banca.ui.AppViewModel
import io.github.filipembcruz.banca.ui.components.ArticleSheet
import io.github.filipembcruz.banca.ui.explore.ExploreScreen
import io.github.filipembcruz.banca.ui.explore.ExploreViewModel
import io.github.filipembcruz.banca.ui.home.HomeScreen
import io.github.filipembcruz.banca.ui.home.HomeViewModel
import io.github.filipembcruz.banca.ui.onboarding.OnboardingScreen
import io.github.filipembcruz.banca.ui.reader.ReaderScreen
import io.github.filipembcruz.banca.ui.reader.ReaderViewModel
import io.github.filipembcruz.banca.ui.saved.SavedScreen
import io.github.filipembcruz.banca.ui.saved.SavedViewModel
import io.github.filipembcruz.banca.ui.settings.AboutPage
import io.github.filipembcruz.banca.ui.settings.AppearancePage
import io.github.filipembcruz.banca.ui.settings.DataPage
import io.github.filipembcruz.banca.ui.settings.ForYouPage
import io.github.filipembcruz.banca.ui.settings.LanguagePage
import io.github.filipembcruz.banca.ui.settings.MarketsPage
import io.github.filipembcruz.banca.ui.settings.NotificationsPage
import io.github.filipembcruz.banca.ui.settings.PlacesPage
import io.github.filipembcruz.banca.ui.settings.ReadingPage
import io.github.filipembcruz.banca.ui.settings.RulePage
import io.github.filipembcruz.banca.ui.settings.SettingsHome
import io.github.filipembcruz.banca.ui.settings.SettingsViewModel
import io.github.filipembcruz.banca.ui.settings.TabsPage
import io.github.filipembcruz.banca.ui.settings.TopicsSourcesPage
import io.github.filipembcruz.banca.ui.theme.LocalExtras
import kotlinx.coroutines.launch

private data class Dest(val label: Int, val icon: ImageVector, val selectedIcon: ImageVector)

private val dests = listOf(
    Dest(R.string.nav_home, Icons.Outlined.Home, Icons.Rounded.Home),
    Dest(R.string.nav_saved, Icons.Outlined.BookmarkBorder, Icons.Rounded.Bookmark),
    Dest(R.string.nav_settings, Icons.Outlined.Settings, Icons.Rounded.Settings),
)

@Composable
fun BancaNav(app: AppViewModel, settings: AppSettings, pendingArticle: MutableState<String?>, pendingInbox: MutableState<Boolean>) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val reduced = LocalExtras.current.reducedMotion
    var menuItem by remember { mutableStateOf<FeedItem?>(null) }
    var mainTab by rememberSaveable { mutableIntStateOf(0) }
    val start = remember { if (settings.onboarded) "main" else "onboarding" }

    // Snackbars with Undo.
    LaunchedEffect(Unit) {
        app.messages.collect { m ->
            snackbar.currentSnackbarData?.dismiss()
            val text = if (m.arg != null) context.getString(m.text, m.arg) else context.getString(m.text)
            val result = snackbar.showSnackbar(
                message = text,
                actionLabel = if (m.undo != null) context.getString(R.string.action_undo) else null,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) m.undo?.invoke()
        }
    }

    // Digest or grouped notification taps open the inbox.
    val inbox = pendingInbox.value
    LaunchedEffect(inbox, settings.onboarded) {
        if (inbox && settings.onboarded) {
            nav.navigate("inbox") { launchSingleTop = true }
            pendingInbox.value = false
        }
    }

    // Notification taps.
    val pending = pendingArticle.value
    LaunchedEffect(pending, settings.onboarded) {
        if (pending != null && settings.onboarded) {
            nav.navigate("reader/$pending")
            pendingArticle.value = null
        }
    }

    val actions = remember(nav) {
        AppActions(
            openArticle = { a, key ->
                val shared = key?.let { "?shared=" + Uri.encode(it) } ?: ""
                nav.navigate("reader/${a.id}$shared")
            },
            openRelated = { r, parent ->
                scope.launch {
                    val id = app.relatedId(r, parent)
                    nav.navigate("reader/$id")
                }
            },
            showMenu = { menuItem = it },
            toggleSave = { app.toggleSave(it) },
            openSettings = { route -> nav.navigate(route) { launchSingleTop = true } },
            openSearch = { q ->
                nav.navigate(if (q.isNullOrBlank()) "search" else "search?q=" + Uri.encode(q)) { launchSingleTop = true }
            },
            back = { nav.popBackStack() },
            image = { a -> app.news.image(a) },
        )
    }

    val slide = spring<IntOffset>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    CompositionLocalProvider(LocalActions provides actions, LocalSettings provides settings) {
        Box(Modifier.fillMaxSize()) {
            SharedTransitionLayout {
                CompositionLocalProvider(LocalSharedScope provides this) {
                    NavHost(
                        navController = nav,
                        startDestination = start,
                        enterTransition = {
                            if (reduced) fadeIn(tween(0)) else slideInHorizontally(slide) { it / 3 } + fadeIn(tween(220))
                        },
                        exitTransition = {
                            if (reduced) fadeOut(tween(0)) else slideOutHorizontally(slide) { -it / 6 } + fadeOut(tween(180))
                        },
                        popEnterTransition = {
                            if (reduced) fadeIn(tween(0)) else slideInHorizontally(slide) { -it / 6 } + fadeIn(tween(220))
                        },
                        popExitTransition = {
                            if (reduced) fadeOut(tween(0)) else slideOutHorizontally(slide) { it / 3 } + fadeOut(tween(180))
                        },
                    ) {
                        composable("onboarding") {
                            val svm: SettingsViewModel = viewModel()
                            OnboardingScreen(app, svm) {
                                nav.navigate("main") { popUpTo("onboarding") { inclusive = true } }
                            }
                        }
                        composable("main") {
                            CompositionLocalProvider(LocalAnimScope provides this) {
                                MainScreen(
                                    tab = mainTab,
                                    onTab = { mainTab = it },
                                    app = app,
                                    navigate = { nav.navigate(it) { launchSingleTop = true } },
                                )
                            }
                        }
                        composable(
                            "reader/{id}?shared={shared}",
                            arguments = listOf(
                                navArgument("id") { type = NavType.StringType },
                                navArgument("shared") { type = NavType.StringType; nullable = true; defaultValue = null },
                            ),
                            enterTransition = { if (reduced) fadeIn(tween(0)) else fadeIn(tween(260)) + scaleIn(initialScale = 0.98f) },
                            exitTransition = { fadeOut(tween(200)) },
                            popEnterTransition = { fadeIn(tween(220)) },
                            popExitTransition = { if (reduced) fadeOut(tween(0)) else fadeOut(tween(220)) + scaleOut(targetScale = 0.98f) },
                        ) { entry ->
                            val vm: ReaderViewModel = viewModel()
                            val shared = entry.arguments?.getString("shared")
                            CompositionLocalProvider(LocalAnimScope provides this) {
                                ReaderScreen(vm, app, shared)
                            }
                        }
                        composable(
                            "search?q={q}",
                            arguments = listOf(navArgument("q") { type = NavType.StringType; nullable = true; defaultValue = null }),
                        ) { entry ->
                            val evm: ExploreViewModel = viewModel()
                            val q = entry.arguments?.getString("q")
                            ExploreScreen(evm, app, initialQuery = q, focusSearch = q == null, onBack = { nav.popBackStack() })
                        }
                        composable("inbox") {
                            val ivm: InboxViewModel = viewModel()
                            CompositionLocalProvider(LocalAnimScope provides this) {
                                InboxScreen(ivm, onBack = { nav.popBackStack() }, onSettings = { nav.navigate("settings/notifications") })
                            }
                        }
                        composable("settings/tabs") { TabsPage(app) { nav.popBackStack() } }
                        composable("settings/foryou") { ForYouPage(app) { nav.popBackStack() } }
                        composable("settings/topics") {
                            val svm: SettingsViewModel = viewModel()
                            TopicsSourcesPage(svm, app) { nav.popBackStack() }
                        }
                        composable("settings/places") {
                            val svm: SettingsViewModel = viewModel()
                            PlacesPage(svm, app) { nav.popBackStack() }
                        }
                        composable("settings/markets") { MarketsPage(app) { nav.popBackStack() } }
                        composable("settings/reading") { ReadingPage(app) { nav.popBackStack() } }
                        composable("settings/notifications") {
                            NotificationsPage(app, onBack = { nav.popBackStack() }, navigate = { nav.navigate(it) })
                        }
                        composable(
                            "settings/rule/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType }),
                        ) { entry ->
                            RulePage(entry.arguments?.getString("id").orEmpty(), app) { nav.popBackStack() }
                        }
                        composable("settings/appearance") { AppearancePage(app) { nav.popBackStack() } }
                        composable("settings/data") {
                            val svm: SettingsViewModel = viewModel()
                            DataPage(svm, app) { nav.popBackStack() }
                        }
                        composable("settings/language") { LanguagePage { nav.popBackStack() } }
                        composable("settings/about") { AboutPage { nav.popBackStack() } }
                    }
                }
            }

            SnackbarHost(
                snackbar,
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 100.dp),
            )
        }

        menuItem?.let { item ->
            ArticleSheet(item, app, onDismiss = { menuItem = null })
        }
    }
}

@Composable
private fun MainScreen(
    tab: Int,
    onTab: (Int) -> Unit,
    app: AppViewModel,
    navigate: (String) -> Unit,
) {
    val homeVm: HomeViewModel = viewModel()
    val savedVm: SavedViewModel = viewModel()
    val holder = rememberSaveableStateHolder()
    val reduced = LocalExtras.current.reducedMotion
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottom: Dp = navInset + 92.dp

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                if (reduced) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                else {
                    val dir = if (targetState > initialState) AnimatedContentTransitionScope.SlideDirection.Left
                    else AnimatedContentTransitionScope.SlideDirection.Right
                    (fadeIn(tween(220)) + slideIntoContainer(dir, tween(280)) { it / 12 }) togetherWith
                        (fadeOut(tween(160)) + slideOutOfContainer(dir, tween(280)) { it / 12 })
                }
            },
            label = "mainTab",
            modifier = Modifier.fillMaxSize(),
        ) { t ->
            holder.SaveableStateProvider(t) {
                when (t) {
                    0 -> HomeScreen(homeVm, bottom, onPlace = { key -> app.updateSettings { it.copy(localPlace = key) } })
                    1 -> SavedScreen(savedVm, app, bottom)
                    else -> SettingsHome(navigate, bottom)
                }
            }
        }
        FloatingNav(
            selected = tab,
            onSelect = onTab,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navInset + 12.dp),
        )
    }
}

/** Floating pill navigation: the selected item grows into a tinted capsule with its label. */
@Composable
private fun FloatingNav(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val reduced = LocalExtras.current.reducedMotion
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = LocalExtras.current.card,
        shadowElevation = 14.dp,
        tonalElevation = 0.dp,
        modifier = modifier,
    ) {
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            dests.forEachIndexed { i, d ->
                val isSel = i == selected
                val bg by animateColorAsState(
                    if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent, label = "navBg",
                )
                val fg by animateColorAsState(
                    if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, label = "navFg",
                )
                Row(
                    Modifier
                        .clip(RoundedCornerShape(26.dp))
                        .background(bg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(i) }
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .animateContentSize(if (reduced) snap() else spring(stiffness = Spring.StiffnessMediumLow)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedContent(isSel, transitionSpec = {
                        (scaleIn(initialScale = 0.7f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.7f) + fadeOut())
                    }, label = "navIcon") { sel ->
                        Icon(if (sel) d.selectedIcon else d.icon, stringResource(d.label), tint = fg)
                    }
                    if (isSel) {
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(d.label), color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }
    }
}
