package com.last.app.presentation.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.last.app.presentation.theme.LastBackground

object ScreenLayoutDefaults {
    const val CONTENT_HORIZONTAL_PADDING_DP = 24
    val HEADER_BOTTOM_SPACING = 8.dp
    val PAGE_BOTTOM_SPACING = 20.dp
    val SCROLL_CONTENT_BOTTOM_SPACING = 32.dp
}

@Composable
fun ScreenLayout(
    title: String,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LastBackground),
    ) {
        ScreenHeader(title = title)
        Spacer(modifier = Modifier.height(ScreenLayoutDefaults.HEADER_BOTTOM_SPACING))
        val bodyModifier = if (scrollable) {
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        } else {
            Modifier.weight(1f)
        }
        Column(modifier = bodyModifier) {
            content()
            if (scrollable) {
                Spacer(modifier = Modifier.height(ScreenLayoutDefaults.PAGE_BOTTOM_SPACING))
            }
        }
    }
}
