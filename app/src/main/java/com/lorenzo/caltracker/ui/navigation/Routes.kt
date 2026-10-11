package com.lorenzo.caltracker.ui.navigation

import kotlinx.serialization.Serializable

// Screens and their arguments. Dates are passed as epoch days (LocalDate.toEpochDay()).

@Serializable
data object DiaryRoute

@Serializable
data object HistoryRoute

@Serializable
data object SettingsRoute

@Serializable
data class AddFoodRoute(val epochDay: Long)

@Serializable
data class LogFoodRoute(val foodId: Long, val epochDay: Long)

@Serializable
data class QuickAddRoute(val epochDay: Long)

@Serializable
data class NewFoodRoute(val epochDay: Long)
