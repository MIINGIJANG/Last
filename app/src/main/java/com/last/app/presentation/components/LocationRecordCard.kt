package com.last.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.last.app.presentation.util.deviceIcon
import com.last.app.presentation.theme.LastCardBorder
import com.last.app.presentation.theme.LastIconBackground
import com.last.app.presentation.theme.LastStatusConnected
import com.last.app.presentation.theme.LastStatusDisconnected
import com.last.app.presentation.theme.LastTextLight
import com.last.app.presentation.theme.LastTextPrimary
import com.last.app.presentation.theme.LastTextSecondary

@Composable
fun LocationRecordCard(
    deviceName: String,
    deviceType: String,
    statusLabel: String?,
    isDisconnect: Boolean = false,
    timeLabel: String,
    locationLabel: String,
    modifier: Modifier = Modifier,
) {
    val statusColor = if (isDisconnect) LastStatusDisconnected else LastStatusConnected

    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, LastCardBorder, RoundedCornerShape(10.dp))
                    .background(LastIconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    deviceIcon(deviceType),
                    deviceName,
                    Modifier.size(20.dp),
                    LastTextSecondary,
                )
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    deviceName,
                    color = LastTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (statusLabel.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                } else {
                    Text(
                        statusLabel,
                        color = statusColor,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(timeLabel, color = LastTextLight, fontSize = 12.sp)
                Text(
                    locationLabel,
                    Modifier.padding(top = 4.dp),
                    color = LastTextLight,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
