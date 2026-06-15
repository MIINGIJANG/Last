package com.last.app.domain.model.history

data class HistoryTimelineSection(
    val dateLabel: String,
    val items: List<HistoryEventDisplay>,
)
