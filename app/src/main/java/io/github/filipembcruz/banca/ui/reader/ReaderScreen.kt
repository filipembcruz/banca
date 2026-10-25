package io.github.filipembcruz.banca.ui.reader

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
import io.github.filipembcruz.banca.ui.components.ImageViewer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChromeReaderMode
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.core.Format
import io.github.filipembcruz.banca.data.model.Article
import io.github.filipembcruz.banca.ui.AppViewModel
import io.github.filipembcruz.banca.ui.components.RelatedList
import io.github.filipembcruz.banca.ui.components.SourceIcon
import io.github.filipembcruz.banca.ui.components.rememberArticleImage
import io.github.filipembcruz.banca.ui.components.shareArticle
import io.github.filipembcruz.banca.ui.components.shimmer
import io.github.filipembcruz.banca.ui.nav.LocalActions
import io.github.filipembcruz.banca.ui.nav.LocalSettings
import io.github.filipembcruz.banca.ui.nav.sharedImage
import io.github.filipembcruz.banca.ui.theme.LocalExtras
import io.github.filipembcruz.banca.ui.theme.readerStyle

@Composable
fun ReaderScreen(vm: ReaderViewModel, app: AppViewModel, sharedKey: String?) {
    val actions = LocalActions.current
    val settings = LocalSettings.current
    val context = LocalContext.current
    val article by vm.article.collectAsStateWithLifecycle()
    val body by vm.body.collectAsStateWithLifecycle()
    val url by vm.url.collectAsStateWithLifecycle()
    val readerModeState by vm.readerMode.collectAsStateWithLifecycle()
    val saved by vm.saved.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()

    var showSummary by remember { mutableStateOf(false) }
    var showCoverage by remember { mutableStateOf(false) }
    var showTextSize by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var webProgress by remember { mutableFloatStateOf(0f) }
    var readProgress by remember { mutableFloatStateOf(0f) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    val readerMode = readerModeState ?: settings.readerDefault
    val a = article

    BackHandler(enabled = !readerMode && webView?.canGoBack() == true) { webView?.goBack() }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top bar
        Row(
            Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = actions.back) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
            if (a != null) {
                SourceIcon(a.source, a.favicon, size = 24.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.source, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        (url ?: a.bestLink).let { Uri.parse(it).host?.removePrefix("www.") ?: "" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
            IconButton(onClick = { vm.setReaderMode(!readerMode) }, enabled = body is ReaderViewModel.Body.Ready || readerMode) {
                AnimatedContent(readerMode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "mode") { rm ->
                    Icon(
                        if (rm) Icons.Rounded.Language else Icons.Rounded.ChromeReaderMode,
                        stringResource(if (rm) R.string.action_web_view else R.string.action_reader_view),
                    )
                }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.action_more)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_open_browser)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) },
                        onClick = {
                            menu = false
                            (url ?: a?.bestLink)?.let { openExternal(context, it) }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_copy_link)) },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                        onClick = {
                            menu = false
                            (url ?: a?.bestLink)?.let { copy(context, it) }
                            app.notify(R.string.msg_link_copied)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_text_size)) },
                        leadingIcon = { Icon(Icons.Rounded.FormatSize, null) },
                        onClick = { menu = false; showTextSize = true },
                    )
                    if (a != null) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_less_source, a.source)) },
                            onClick = { menu = false; app.showLessSource(a) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_block_source, a.source), color = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; app.block(a); actions.back() },
                        )
                    }
                }
            }
        }

        // Progress: page load in web mode, reading position in reader mode.
        val progress by animateFloatAsState(if (readerMode) readProgress else webProgress, label = "progress")
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Butt,
            drawStopIndicator = {},
        )

        Box(Modifier.weight(1f)) {
            if (a == null) {
                if (body is ReaderViewModel.Body.Failed) {
                    Text(stringResource(R.string.reader_not_found), Modifier.align(Alignment.Center).padding(32.dp))
                } else {
                    ReaderSkeleton()
                }
            } else {
                AnimatedContent(
                    targetState = readerMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "readerMode",
                ) { rm ->
                    if (rm) {
                        ReaderBody(a, body, sharedKey, onProgress = { readProgress = it }, onOpenWeb = { vm.setReaderMode(false) })
                    } else {
                        val target = url
                        if (target == null) {
                            ReaderSkeleton()
                        } else {
                            WebPane(target, onProgress = { webProgress = it }, onCreated = { webView = it })
                        }
                    }
                }
            }
        }

        // Bottom actions
        Surface(color = LocalExtras.current.card, tonalElevation = 0.dp) {
            Row(
                Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                BarAction(Icons.AutoMirrored.Rounded.FormatListBulleted, stringResource(R.string.action_summarize)) {
                    vm.summarize(); showSummary = true
                }
                BarAction(
                    if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    stringResource(if (saved) R.string.action_saved else R.string.action_save),
                    highlighted = saved,
                ) { vm.toggleSave() }
                BarAction(Icons.Rounded.Share, stringResource(R.string.action_share)) {
                    a?.let { shareArticle(context, it.title, url ?: it.bestLink) }
                }
                BarAction(
                    Icons.Rounded.Layers,
                    stringResource(R.string.action_coverage),
                    badge = a?.related?.size?.takeIf { it > 0 },
                ) { showCoverage = true }
            }
        }
    }

    if (showSummary) SummarySheet(summary, onDismiss = { showSummary = false })
    if (showCoverage && a != null) CoverageSheet(a, onDismiss = { showCoverage = false })
    if (showTextSize) TextSizeSheet(app, onDismiss = { showTextSize = false })
}

@Composable
private fun BarAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    highlighted: Boolean = false,
    badge: Int? = null,
    onClick: () -> Unit,
) {
    val color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(onClick = onClick, color = Color.Transparent, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                Icon(icon, null, tint = color)
                if (badge != null) {
                    Text(
                        badge.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(start = 18.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 5.dp),
                    )
                }
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun ReaderSkeleton() {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16 / 9f).shimmer(RoundedCornerShape(20.dp)))
        Box(Modifier.fillMaxWidth().height(26.dp).shimmer())
        Box(Modifier.fillMaxWidth(0.7f).height(26.dp).shimmer())
        repeat(6) { Box(Modifier.fillMaxWidth().height(16.dp).shimmer()) }
    }
}

@Composable
internal fun ReaderBody(
    a: Article,
    body: ReaderViewModel.Body,
    sharedKey: String?,
    onProgress: (Float) -> Unit,
    onOpenWeb: () -> Unit,
) {
    val settings = LocalSettings.current
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val cardImage = rememberArticleImage(a).url
    val image = (body as? ReaderViewModel.Body.Ready)?.content?.image ?: cardImage
    var viewing by remember { mutableStateOf(false) }
    val pad = Modifier.padding(horizontal = 20.dp)
    val textColor = MaterialTheme.colorScheme.onSurface
    val style = readerStyle(settings.serifReader, settings.textScale, textColor)

    val progress = remember(listState) {
        androidx.compose.runtime.derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total <= 1) 0f else {
                val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                (last.toFloat() / (total - 1)).coerceIn(0f, 1f)
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow { progress.value }.collect { onProgress(it) }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(bottom = 40.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Full-bleed picture with a gentle parallax; the text card slides up over it.
        item(key = "image") {
            if (image != null) {
                AsyncImage(
                    model = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .sharedImage(sharedKey)
                        .fillMaxWidth()
                        .aspectRatio(4 / 3f)
                        .graphicsLayer {
                            if (listState.firstVisibleItemIndex == 0) {
                                translationY = listState.firstVisibleItemScrollOffset * 0.45f
                            }
                        }
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { viewing = true },
                )
            }
        }
        item(key = "title") {
            Column(
                Modifier
                    .then(if (image != null) Modifier.offset(y = (-28).dp) else Modifier)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp)
            ) {
            val title = (body as? ReaderViewModel.Body.Ready)?.content?.title?.takeIf { it.length > 10 } ?: a.title
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = if (settings.serifReader) FontFamily.Serif else FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    fontSize = (25 * settings.textScale).sp,
                    lineHeight = (32 * settings.textScale).sp,
                ),
            )
            Spacer(Modifier.height(12.dp))
            val minutes = (body as? ReaderViewModel.Body.Ready)?.content?.minutes
            val author = (body as? ReaderViewModel.Body.Ready)?.content?.author
            Text(
                listOfNotNull(
                    author,
                    Format.ago(context, a.published).takeIf { it.isNotEmpty() },
                    minutes?.let { stringResource(R.string.reader_minutes, it) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }
            Spacer(Modifier.height(if (image != null) 0.dp else 20.dp))
        }
        when (body) {
            is ReaderViewModel.Body.Loading -> item(key = "loading") {
                Column(pad, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(7) { i -> Box(Modifier.fillMaxWidth(if (i % 3 == 2) 0.6f else 1f).height(16.dp).shimmer()) }
                }
            }
            is ReaderViewModel.Body.Failed -> item(key = "failed") {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = pad.fillMaxWidth().padding(vertical = 24.dp)) {
                    Text(
                        stringResource(R.string.reader_unavailable),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onOpenWeb) { Text(stringResource(R.string.action_web_view)) }
                }
            }
            is ReaderViewModel.Body.Ready -> {
                items(body.content.blocks) { b ->
                    SelectionContainer(pad) {
                        when {
                            b.heading -> Text(
                                b.text,
                                style = style.copy(fontWeight = FontWeight.Bold, fontSize = style.fontSize * 1.1f),
                                modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                            )
                            b.quote -> Row(Modifier.padding(vertical = 10.dp).height(IntrinsicSize.Min)) {
                                Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
                                Spacer(Modifier.width(14.dp))
                                Text(b.text, style = style.copy(fontStyle = FontStyle.Italic))
                            }
                            else -> Text(b.text, style = style, modifier = Modifier.padding(bottom = 16.dp))
                        }
                    }
                }
                item(key = "source") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onOpenWeb, modifier = pad.fillMaxWidth()) {
                        Text(stringResource(R.string.reader_read_on, a.source))
                    }
                }
            }
        }
        if (a.related.isNotEmpty()) {
            item(key = "coverage") {
                Column(pad) {
                    Spacer(Modifier.height(28.dp))
                    Text(stringResource(R.string.reader_other_coverage), style = MaterialTheme.typography.titleMedium)
                    RelatedList(a)
                }
            }
        }
    }
    if (viewing && image != null) ImageViewer(image) { viewing = false }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebPane(url: String, onProgress: (Float) -> Unit, onCreated: (WebView) -> Unit) {
    val dark = LocalExtras.current.dark
    var view by remember { mutableStateOf<WebView?>(null) }
    DisposableEffect(Unit) {
        onDispose { view?.destroy() }
    }
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                if (Build.VERSION.SDK_INT >= 33) settings.isAlgorithmicDarkeningAllowed = dark
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val scheme = request.url.scheme ?: return true
                        if (scheme == "http" || scheme == "https") return false
                        // intent:, mailto:, app links: hand off to the system.
                        runCatching { view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        return true
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        onProgress(0.1f)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onProgress(1f)
                    }
                }
                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgress(newProgress / 100f)
                    }
                }
                loadUrl(url)
                view = this
                onCreated(this)
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun SummarySheet(state: ReaderViewModel.SummaryState, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            AnimatedContent(state, transitionSpec = { fadeIn() togetherWith fadeOut() }, contentKey = { it::class }, label = "summary") { s ->
                when (s) {
                    is ReaderViewModel.SummaryState.Ready -> Column {
                        Text(
                            stringResource(if (s.summary.fromPublisher) R.string.summary_from_publisher else R.string.summary_key_points),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(14.dp))
                        s.summary.points.forEach { p ->
                            Row(Modifier.padding(bottom = 12.dp)) {
                                if (!s.summary.fromPublisher) {
                                    Box(
                                        Modifier
                                            .padding(top = 9.dp)
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }
                                Text(p, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        Text(
                            stringResource(R.string.summary_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is ReaderViewModel.SummaryState.Unavailable -> Text(
                        stringResource(R.string.summary_unavailable),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                    else -> Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(4) { Box(Modifier.fillMaxWidth().height(16.dp).shimmer()) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoverageSheet(a: Article, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.reader_other_coverage), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 8.dp))
            if (a.related.isEmpty()) {
                Text(
                    stringResource(R.string.coverage_none),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            } else {
                RelatedList(a, max = 20)
            }
        }
    }
}

@Composable
private fun TextSizeSheet(app: AppViewModel, onDismiss: () -> Unit) {
    val settings = LocalSettings.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var scale by remember { mutableFloatStateOf(settings.textScale) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.action_text_size), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.text_size_sample),
                style = readerStyle(settings.serifReader, scale, MaterialTheme.colorScheme.onSurface),
            )
            Slider(
                value = scale,
                onValueChange = { scale = it },
                onValueChangeFinished = { app.updateSettings { s -> s.copy(textScale = scale) } },
                valueRange = 0.85f..1.4f,
                steps = 10,
            )
        }
    }
}

private fun openExternal(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("link", text))
}
