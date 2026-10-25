package io.github.filipembcruz.banca.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.core.Format
import io.github.filipembcruz.banca.data.model.Article
import io.github.filipembcruz.banca.data.model.FeedItem
import io.github.filipembcruz.banca.ui.nav.LocalActions
import io.github.filipembcruz.banca.ui.nav.LocalSettings
import io.github.filipembcruz.banca.ui.nav.sharedImage

/** Picture for a card: the feed's own, or one resolved in the background. */
@Composable
fun rememberArticleImage(a: Article): String? {
    val actions = LocalActions.current
    val settings = LocalSettings.current
    val context = LocalContext.current
    val allowed = remember(settings.imagesWifiOnly) { !settings.imagesWifiOnly || !Format.isMetered(context) }
    val img by produceState(initialValue = a.image, a.id, allowed) {
        if (value == null && allowed) value = runCatching { actions.image(a) }.getOrNull()
    }
    return if (allowed) img else null
}

@Composable
fun SourceLine(a: Article, modifier: Modifier = Modifier, light: Boolean = false, showTopic: Boolean = false) {
    val context = LocalContext.current
    val color = if (light) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        SourceIcon(a.source, a.favicon, size = 18.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            a.source,
            style = MaterialTheme.typography.labelMedium,
            color = if (light) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        val ago = Format.ago(context, a.published)
        if (ago.isNotEmpty()) {
            Text(" · $ago", style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
        }
        if (showTopic) {
            topicLabel(a.topic)?.let { t ->
                Text(" · $t", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
            }
        }
    }
}

/** Standard feed row: text on the left, square picture on the right. */
@Composable
fun ArticleRow(
    item: FeedItem,
    modifier: Modifier = Modifier,
    sharedKey: String? = null,
    showTopic: Boolean = false,
    divider: Boolean = true,
) {
    val a = item.article
    val actions = LocalActions.current
    val settings = LocalSettings.current
    val image = rememberArticleImage(a)
    var expanded by rememberSaveable(a.id) { mutableStateOf(false) }

    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(
                    onClick = { actions.openArticle(a, sharedKey) },
                    onLongClick = { actions.showMenu(item) },
                )
                .padding(vertical = 12.dp, horizontal = 4.dp)
        ) {
            SourceLine(a, showTopic = showTopic)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    a.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (item.read) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                AnimatedVisibility(visible = image != null, enter = fadeIn() + scaleIn(initialScale = 0.92f), exit = fadeOut()) {
                    Row {
                        Spacer(Modifier.width(14.dp))
                        AsyncImage(
                            model = image,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .sharedImage(sharedKey)
                                .size(88.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }
                }
            }
            if (settings.feedSummaries && !a.summary.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    a.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                if (a.related.isNotEmpty()) {
                    Surface(
                        onClick = { expanded = !expanded },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Row(Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                pluralStringResource(R.plurals.more_sources, a.related.size, a.related.size),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Icon(
                                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { actions.toggleSave(item) }) {
                    AnimatedContent(
                        targetState = item.saved,
                        transitionSpec = { (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.6f) + fadeOut()) },
                        label = "save",
                    ) { saved ->
                        Icon(
                            if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = stringResource(if (saved) R.string.action_unsave else R.string.action_save),
                            tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = { actions.showMenu(item) }) {
                    Icon(Icons.Rounded.MoreVert, stringResource(R.string.action_more), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                RelatedList(a)
            }
        }
        if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}

@Composable
fun RelatedList(a: Article, max: Int = 8) {
    val actions = LocalActions.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(vertical = 4.dp)
    ) {
        a.related.take(max).forEach { r ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(onClick = { actions.openRelated(r, a) })
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                SourceIcon(r.source, null, size = 18.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.source, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(r.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Large picture card with the title over the image. */
@Composable
fun HeroCard(
    item: FeedItem,
    modifier: Modifier = Modifier,
    sharedKey: String? = null,
    height: Dp = 232.dp,
) {
    val a = item.article
    val actions = LocalActions.current
    val image = rememberArticleImage(a)
    val fallback = remember(a.source) { avatarColor(a.source) }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(onClick = { actions.openArticle(a, sharedKey) }, onLongClick = { actions.showMenu(item) })
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(fallback, fallback.copy(alpha = 0.55f), Color(0xFF12151C))))
        )
        if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .sharedImage(sharedKey)
                    .fillMaxSize(),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.35f to Color.Black.copy(alpha = 0.05f),
                        1f to Color.Black.copy(alpha = 0.82f),
                    )
                )
        )
        if (a.related.isNotEmpty()) {
            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                contentColor = Color.White,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            ) {
                Text(
                    pluralStringResource(R.plurals.sources_count, a.related.size + 1, a.related.size + 1),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SourceLine(a, light = true)
            Text(
                a.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Dot used in pagers. */
@Composable
fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            val active = i == current
            val w by animateDpAsState(if (active) 18.dp else 6.dp, label = "dot")
            Box(
                Modifier
                    .height(6.dp)
                    .width(w)
                    .clip(CircleShape)
                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            )
        }
    }
}
