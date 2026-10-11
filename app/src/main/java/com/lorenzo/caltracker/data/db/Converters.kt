package com.lorenzo.caltracker.data.db

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

/**
 * Dates are stored as ISO text ("2026-10-11") and times as "HH:mm":
 * both sort correctly as text and stay readable in exports.
 */
class Converters {
    @TypeConverter
    fun localDateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun localTimeToString(time: LocalTime?): String? = time?.withSecond(0)?.withNano(0)?.toString()

    @TypeConverter
    fun stringToLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)
}
