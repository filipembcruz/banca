package io.github.filipembcruz.banca.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

/** Slides an element to its new position when the layout moves it. */
fun Modifier.animatePlacement(): Modifier = composed {
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf(IntOffset.Zero) }
    var anim by remember { mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null) }
    this
        .onPlaced { target = it.positionInParent().round() }
        .graphicsLayer {
            val a = anim ?: Animatable(target, IntOffset.VectorConverter).also { anim = it }
            if (a.targetValue != target) {
                scope.launch { a.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow)) }
            }
            val d = a.value - target
            translationX = d.x.toFloat()
            translationY = d.y.toFloat()
        }
}

/**
 * A short list the user reorders by dragging the handle. Neighbours glide out of the way,
 * the dragged row lifts with a shadow, and [onMove] gets the final order on release.
 */
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    itemKey: (T) -> Any,
    onMove: (List<T>) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    content: @Composable (item: T, dragging: Boolean, handle: Modifier) -> Unit,
) {
    var list by remember(items) { mutableStateOf(items) }
    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var settlingKey by remember { mutableStateOf<Any?>(null) }
    val settle = remember { Animatable(0f) }
    val heights = remember { mutableStateMapOf<Any, Int>() }
    val spacingPx = with(LocalDensity.current) { spacing.toPx() }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        list.forEach { item ->
            val k = itemKey(item)
            key(k) {
                val dragging = draggingKey == k
                val lift by animateDpAsState(if (dragging) 10.dp else 0.dp, label = "lift")
                val handle = Modifier.pointerInput(k) {
                    detectDragGestures(
                        onDragStart = {
                            draggingKey = k
                            dragOffset = 0f
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragOffset += amount.y
                            val i = list.indexOfFirst { itemKey(it) == k }
                            if (i < 0) return@detectDragGestures
                            if (dragOffset > 0 && i < list.lastIndex) {
                                val h = (heights[itemKey(list[i + 1])] ?: 0) + spacingPx
                                if (dragOffset > h / 2f) {
                                    list = list.toMutableList().also { it.add(i + 1, it.removeAt(i)) }
                                    dragOffset -= h
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            } else if (dragOffset < 0 && i > 0) {
                                val h = (heights[itemKey(list[i - 1])] ?: 0) + spacingPx
                                if (-dragOffset > h / 2f) {
                                    list = list.toMutableList().also { it.add(i - 1, it.removeAt(i)) }
                                    dragOffset += h
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        },
                        onDragEnd = {
                            val start = dragOffset
                            settlingKey = k
                            draggingKey = null
                            dragOffset = 0f
                            onMove(list)
                            scope.launch {
                                settle.snapTo(start)
                                settle.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                settlingKey = null
                            }
                        },
                        onDragCancel = {
                            draggingKey = null
                            dragOffset = 0f
                        },
                    )
                }
                val moving = dragging || settlingKey == k
                Column(
                    Modifier
                        .onSizeChanged { heights[k] = it.height }
                        .zIndex(if (moving) 1f else 0f)
                        .then(if (moving) Modifier else Modifier.animatePlacement())
                        .graphicsLayer {
                            translationY = when {
                                dragging -> dragOffset
                                settlingKey == k -> settle.value
                                else -> 0f
                            }
                        }
                        .shadow(lift, MaterialTheme.shapes.medium, clip = false)
                ) {
                    content(item, dragging, handle)
                }
            }
        }
    }
}

/** The grip icon shown at the end of reorderable rows. */
@Composable
fun DragHandle(handle: Modifier) {
    Icon(
        Icons.Rounded.DragIndicator,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = handle,
    )
}
