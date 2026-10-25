package io.github.filipembcruz.banca.ui.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.core.Text as TextUtil
import io.github.filipembcruz.banca.data.model.FeedItem
import io.github.filipembcruz.banca.data.model.Topics
import io.github.filipembcruz.banca.ui.AppViewModel
import io.github.filipembcruz.banca.ui.nav.LocalSettings
import io.github.filipembcruz.banca.ui.theme.LocalExtras

fun shareArticle(context: android.content.Context, title: String, link: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "$title\n$link")
    }
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/**
 * Story actions: three quick tiles (save, share, hide), then "show less about…" chips with the
 * topic, names from the headline and the outlet, then follow and block.
 */
@Composable
fun ArticleSheet(item: FeedItem, vm: AppViewModel, onDismiss: () -> Unit) {
    val a = item.article
    val context = LocalContext.current
    val settings = LocalSettings.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val topic = topicLabel(a.topic)
    val phrases = remember(a.title) { TextUtil.keyPhrases(a.title) }
    val card = LocalExtras.current.card

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SourceIcon(a.source, a.favicon, size = 22.dp)
                Spacer(Modifier.width(10.dp))
                Text(a.source, style = MaterialTheme.typography.labelLarge)
            }
            Text(
                a.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile(
                    if (item.saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    stringResource(if (item.saved) R.string.action_saved else R.string.action_save),
                    card, Modifier.weight(1f), highlighted = item.saved,
                ) { vm.toggleSave(item); onDismiss() }
                QuickTile(Icons.Rounded.Share, stringResource(R.string.action_share), card, Modifier.weight(1f)) {
                    shareArticle(context, a.title, a.bestLink); onDismiss()
                }
                QuickTile(Icons.Rounded.VisibilityOff, stringResource(R.string.action_hide), card, Modifier.weight(1f)) {
                    vm.hide(a); onDismiss()
                }
            }

            Row(Modifier.padding(top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ThumbDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.sheet_show_less), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ChipFlow {
                if (topic != null && a.topic != null && a.topic != "top") {
                    LessChip(topic) { vm.showLessTopic(a, topic); onDismiss() }
                }
                phrases.forEach { p -> LessChip(p) { vm.showLessTerm(p); onDismiss() } }
                LessChip(a.source) { vm.showLessSource(a); onDismiss() }
            }

            Spacer(Modifier.height(18.dp))
            Surface(shape = RoundedCornerShape(20.dp), color = card) {
                Column {
                    if (topic != null && a.topic != null && Topics.byId(a.topic) != null && a.topic !in settings.topics) {
                        SheetRow(Icons.Rounded.AddCircleOutline, stringResource(R.string.action_follow, topic)) {
                            vm.followTopic(a.topic, topic); onDismiss()
                        }
                    }
                    if (a.domain != null) {
                        SheetRow(Icons.Rounded.AddCircleOutline, stringResource(R.string.action_follow_source, a.source)) {
                            vm.followSource(a); onDismiss()
                        }
                    }
                    SheetRow(Icons.Rounded.Block, stringResource(R.string.action_block_source, a.source), danger = true) {
                        vm.block(a); onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = color, modifier = modifier) {
        Column(Modifier.padding(vertical = 16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LessChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        shape = RoundedCornerShape(50),
        colors = AssistChipDefaults.assistChipColors(containerColor = LocalExtras.current.card),
        border = null,
    )
}

@Composable
fun SheetRow(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (danger) color else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Medium)
    }
}
