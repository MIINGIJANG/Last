package com.last.app.presentation.app

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.zIndex

@Composable
fun KeepAliveTab(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .layout { measurable, constraints ->
                if (!visible) {
                    layout(0, 0) {}
                } else {
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
            }
            .alpha(if (visible) 1f else 0f)
            .zIndex(if (visible) 1f else -1f)
            .then(
                if (visible) {
                    Modifier
                } else {
                    Modifier.pointerInput(Unit) {}
                },
            ),
    ) {
        content()
    }
}
