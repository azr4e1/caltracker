package com.lorenzo.caltracker.data.db

import androidx.room.TypeConverter
import java.time.LocalDate

/** Dates are stored as ISO text ("2026-10-11"): sortable and readable in exports. */
class Converters {
    @TypeConverter
    fun localDateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)
}
