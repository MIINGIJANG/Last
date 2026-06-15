package com.last.app.domain.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormats {
    val koreanDateTime = SimpleDateFormat("yyyy.MM.dd a h:mm", Locale.KOREAN)
    val koreanTime = SimpleDateFormat("a h:mm", Locale.KOREAN)
    val koreanDateHeader = SimpleDateFormat("M월 d일 (E)", Locale.KOREAN)

    private const val DAY_MS = 86_400_000L
    private const val DATE_HEADER_CACHE_LIMIT = 64
    private val dateHeaderByDay = object : LinkedHashMap<Long, String>(DATE_HEADER_CACHE_LIMIT, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, String>): Boolean {
            return size > DATE_HEADER_CACHE_LIMIT
        }
    }

    fun formatKoreanTime(epochMillis: Long): String = koreanTime.format(Date(epochMillis))

    fun formatKoreanDateHeader(epochMillis: Long): String {
        val dayEpoch = epochMillis / DAY_MS
        synchronized(dateHeaderByDay) {
            return dateHeaderByDay.getOrPut(dayEpoch) {
                koreanDateHeader.format(Date(epochMillis))
            }
        }
    }
}
