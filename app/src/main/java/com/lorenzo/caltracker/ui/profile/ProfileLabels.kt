package com.lorenzo.caltracker.ui.profile

import com.lorenzo.caltracker.data.db.entity.ActivityLevel
import com.lorenzo.caltracker.data.db.entity.Sex

val Sex.label: String
    get() = when (this) {
        Sex.MALE -> "Male"
        Sex.FEMALE -> "Female"
    }

val ActivityLevel.title: String
    get() = when (this) {
        ActivityLevel.SEDENTARY -> "Sedentary"
        ActivityLevel.LIGHT -> "Lightly active"
        ActivityLevel.MODERATE -> "Moderately active"
        ActivityLevel.ACTIVE -> "Active"
        ActivityLevel.VERY_ACTIVE -> "Very active"
    }

val ActivityLevel.description: String
    get() = when (this) {
        ActivityLevel.SEDENTARY -> "Desk job, little exercise"
        ActivityLevel.LIGHT -> "Exercise 1–3 days a week"
        ActivityLevel.MODERATE -> "Exercise 3–5 days a week"
        ActivityLevel.ACTIVE -> "Hard exercise 6–7 days a week"
        ActivityLevel.VERY_ACTIVE -> "Physical job plus training"
    }
