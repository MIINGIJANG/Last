package com.last.app.domain.history

import com.last.app.data.entity.Device
import com.last.app.data.entity.DeviceConnectionEvent
import com.last.app.domain.model.history.HistoryTimelineSection
import com.last.app.domain.util.deviceListSignature
import com.last.app.domain.util.eventTimelineSignature
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object HistoryTimelineCache {

    private const val MAX_ENTRIES = 4

    private data class CacheKey(
        val eventSignature: Long,
        val deviceSignature: Long,
    )

    private val cache = object : LinkedHashMap<CacheKey, List<HistoryTimelineSection>>(
        MAX_ENTRIES,
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, List<HistoryTimelineSection>>): Boolean {
            return size > MAX_ENTRIES
        }
    }

    private val buildMutex = Mutex()

    suspend fun getOrBuild(
        events: List<DeviceConnectionEvent>,
        devices: List<Device>,
        builder: suspend () -> List<HistoryTimelineSection>,
    ): List<HistoryTimelineSection> {
        val key = CacheKey(events.eventTimelineSignature(), devices.deviceListSignature())
        synchronized(cache) {
            cache[key]?.let { return it }
        }
        return buildMutex.withLock {
            synchronized(cache) {
                cache[key]?.let { return it }
            }
            val sections = builder()
            synchronized(cache) {
                cache[key] = sections
            }
            sections
        }
    }

    fun invalidate() {
        synchronized(cache) { cache.clear() }
    }
}
