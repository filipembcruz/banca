package io.github.filipembcruz.banca.ui.components

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.data.model.FeedItem
import io.github.filipembcruz.banca.data.model.Topics
import io.github.filipembcruz.banca.ui.AppViewModel
import io.github.filipembcruz.banca.ui.nav.LocalSettings

fun shareArticle(context: android.content.Context, title: String, link: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, "$title\n$link")
    }
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
fun ArticleSheet(item: FeedItem, vm: AppViewModel, onDismiss: () -> Unit) {
    val a = item.article
    val context = LocalContext.current
    val settings = LocalSettings.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val topic = topicLabel(a.topic)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 12.dp)) {
            Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                SourceIcon(a.source, a.favicon, size = 22.dp)
                Spacer(Modifier.width(10.dp))
                Text(a.source, style = MaterialTheme.typography.labelLarge)
            }
            Text(
                a.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
            )
            HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)

            SheetRow(if (item.saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                stringResource(if (item.saved) R.string.action_unsave else R.string.action_save)) {
                vm.toggleSave(item); onDismiss()
            }
            SheetRow(Icons.Rounded.Share, stringResource(R.string.action_share)) {
                shareArticle(context, a.title, a.bestLink); onDismiss()
            }
            SheetRow(Icons.Rounded.VisibilityOff, stringResource(R.string.action_hide_story)) {
                vm.hide(a); onDismiss()
            }
            if (topic != null && a.topic != null && a.topic != "top") {
                SheetRow(Icons.Rounded.ThumbDown, stringResource(R.string.action_less_topic, topic)) {
                    vm.showLessTopic(a, topic); onDismiss()
                }
                if (Topics.byId(a.topic) != null && a.topic !in settings.topics) {
                    SheetRow(Icons.Rounded.AddCircleOutline, stringResource(R.string.action_follow, topic)) {
                        vm.followTopic(a.topic, topic); onDismiss()
                    }
                }
            }
            SheetRow(Icons.Rounded.RemoveCircleOutline, stringResource(R.string.action_less_source, a.source)) {
                vm.showLessSource(a); onDismiss()
            }
            if (a.domain != null) {
                SheetRow(Icons.Rounded.AddCircleOutline, stringResource(R.string.action_follow_source, a.source)) {
                    vm.followSource(a); onDismiss()
                }
            }
            SheetRow(Icons.Rounded.Block, stringResource(R.string.action_block_source, a.source), danger = true) {
                vm.block(a); onDismiss()
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun SheetRow(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    val color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (danger) color else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(18.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@Suppress("unused")
private val keep = Color.Unspecified
