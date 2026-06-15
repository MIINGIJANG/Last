package com.last.app.presentation.navigation

enum class AppDestination(
    val labelRes: Int,
) {
    Dashboard(com.last.app.R.string.use_case_dashboard),
    Device(com.last.app.R.string.use_case_register),
    History(com.last.app.R.string.use_case_history),
    Location(com.last.app.R.string.use_case_location),
    Settings(com.last.app.R.string.use_case_settings),
}
