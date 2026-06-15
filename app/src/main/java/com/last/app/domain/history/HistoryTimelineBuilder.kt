package com.last.app.domain.history

import com.last.app.data.entity.Device
import com.last.app.data.entity.DeviceConnectionEvent
import com.last.app.domain.model.history.HistoryEventDisplay
import com.last.app.domain.model.history.HistoryTimelineSection
import com.last.app.domain.history.historyEventStatusLabel
import com.last.app.domain.history.isDisconnectEvent
import com.last.app.domain.util.DateFormats

object HistoryTimelineBuilder {

    fun buildSections(
        events: List<DeviceConnectionEvent>,
        devices: List<Device>,
        locationLabels: Map<Long, String>,
    ): List<HistoryTimelineSection> {
        val deviceById = devices.associateBy { it.id }
        if (events.isEmpty()) {
            return listOf(
                HistoryTimelineSection(
                    dateLabel = DateFormats.formatKoreanDateHeader(System.currentTimeMillis()),
                    items = emptyList(),
                ),
            )
        }

        val sections = linkedMapOf<String, MutableList<HistoryEventDisplay>>()
        for (index in events.indices.reversed()) {
            val event = events[index]
            val device = event.deviceId?.let { deviceById[it] }
            if (event.deviceId != null && device == null) continue
            val dateLabel = DateFormats.formatKoreanDateHeader(event.eventTime)
            val display = HistoryEventDisplay(
                deviceName = device?.deviceName ?: event.deviceType,
                deviceType = device?.deviceType ?: event.deviceType,
                eventStatusLabel = historyEventStatusLabel(event.eventType),
                isDisconnect = isDisconnectEvent(event.eventType),
                timeLabel = DateFormats.formatKoreanTime(event.eventTime),
                locationLabel = locationLabels[event.eventId] ?: "위치 정보 없음",
            )
            sections.getOrPut(dateLabel) { mutableListOf() }.add(display)
        }

        return buildList(sections.size) {
            for ((dateLabel, items) in sections) {
                add(HistoryTimelineSection(dateLabel = dateLabel, items = items))
            }
        }
    }
}
