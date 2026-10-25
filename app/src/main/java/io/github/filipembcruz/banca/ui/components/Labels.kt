package io.github.filipembcruz.banca.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.BusinessCenter
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.github.filipembcruz.banca.R
import io.github.filipembcruz.banca.data.model.TabDef
import io.github.filipembcruz.banca.data.model.TabType
import io.github.filipembcruz.banca.data.model.Topics

object Labels {

    fun topicRes(id: String): Int? = when (id) {
        "world" -> R.string.topic_world
        "nation" -> R.string.topic_nation
        "politics" -> R.string.topic_politics
        "business" -> R.string.topic_business
        "technology" -> R.string.topic_technology
        "science" -> R.string.topic_science
        "sports" -> R.string.topic_sports
        "entertainment" -> R.string.topic_entertainment
        "health" -> R.string.topic_health
        "games" -> R.string.topic_games
        "cars" -> R.string.topic_cars
        "travel" -> R.string.topic_travel
        "food" -> R.string.topic_food
        "music" -> R.string.topic_music
        Topics.SCITECH -> R.string.topic_scitech
        "top" -> R.string.topic_top
        "markets" -> R.string.topic_markets
        else -> null
    }

    fun topicIcon(id: String): ImageVector = when (id) {
        "world" -> Icons.Rounded.Public
        "nation" -> Icons.Rounded.Flag
        "politics" -> Icons.Rounded.AccountBalance
        "business" -> Icons.Rounded.BusinessCenter
        "technology" -> Icons.Rounded.Memory
        "science", Topics.SCITECH -> Icons.Rounded.Science
        "sports" -> Icons.Rounded.SportsSoccer
        "entertainment" -> Icons.Rounded.Movie
        "health" -> Icons.Rounded.MonitorHeart
        "games" -> Icons.Rounded.SportsEsports
        "cars" -> Icons.Rounded.DirectionsCar
        "travel" -> Icons.Rounded.Flight
        "food" -> Icons.Rounded.Restaurant
        "music" -> Icons.Rounded.MusicNote
        else -> Icons.Rounded.Whatshot
    }
}

/** Display name of an article's topic tag, or null when it has none worth showing. */
@Composable
fun topicLabel(topic: String?): String? {
    if (topic.isNullOrBlank()) return null
    Labels.topicRes(topic)?.let { return stringResource(it) }
    return when {
        topic.startsWith("place:") -> topic.removePrefix("place:")
        topic.startsWith("term:") -> topic.removePrefix("term:")
        else -> null
    }
}

@Composable
fun tabTitle(tab: TabDef): String = when (tab.type) {
    TabType.FOR_YOU -> stringResource(R.string.tab_for_you)
    TabType.TRENDING -> stringResource(R.string.tab_trending)
    TabType.LOCAL -> stringResource(R.string.tab_local)
    TabType.TOPIC -> Labels.topicRes(tab.value)?.let { stringResource(it) } ?: tab.title.ifBlank { tab.value }
    TabType.SEARCH, TabType.FEED -> tab.title.ifBlank { tab.value }
}
