package com.lorenzo.caltracker.data.repository

import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The day shown in the Diary. App-wide so other screens (History) can open a specific day. */
class DiaryDateHolder {
    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    fun select(date: LocalDate) {
        _date.value = date
    }
}
