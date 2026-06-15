package com.last.app.presentation.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.last.app.presentation.theme.LastPrimary
import com.last.app.presentation.theme.LastTextMuted

private data class NavItem(
    val destination: AppDestination,
    val icon: ImageVector,
)

private val navItems = listOf(
    NavItem(AppDestination.Dashboard, Icons.Outlined.Map),
    NavItem(AppDestination.Device, Icons.Outlined.VerifiedUser),
    NavItem(AppDestination.History, Icons.Outlined.History),
    NavItem(AppDestination.Location, Icons.Outlined.Search),
    NavItem(AppDestination.Settings, Icons.Outlined.Settings),
)

@Composable
fun BottomNavBar(
    selected: AppDestination,
    onSelect: (AppDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = Color.White,
        tonalElevation = 0.dp,
    ) {
        navItems.forEach { item ->
            val isSelected = selected == item.destination
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(item.destination) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.destination.labelRes),
                        modifier = Modifier.size(22.dp),
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.destination.labelRes),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = LastPrimary,
                    selectedTextColor = LastPrimary,
                    unselectedIconColor = LastTextMuted,
                    unselectedTextColor = LastTextMuted,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}
