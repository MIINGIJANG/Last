package com.last.app.data.entity

object EventSources {
    const val BLUETOOTH = "BLUETOOTH"
    const val USB = "USB"
    const val POWER = "POWER"

    fun fromDeviceType(deviceType: String): String {
        return when (deviceType.uppercase()) {
            BLUETOOTH -> BLUETOOTH
            USB -> USB
            POWER -> POWER
            else -> deviceType.uppercase()
        }
    }
}
