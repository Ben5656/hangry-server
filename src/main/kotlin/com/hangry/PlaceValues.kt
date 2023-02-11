package com.hangry

data class PlaceValues(
    val html_attributions: List<Any>,
    val result: Result,
    val status: String
)

data class CurrentOpeningHours(
    val open_now: Boolean,
    val periods: List<Period>,
    val weekday_text: List<String>
)

data class Period(
    val close: Close,
    val `open`: Open
)

data class Close(
    val date: String,
    val day: Int,
    val time: String,
    val truncated: Boolean
)

data class Open(
    val date: String,
    val day: Int,
    val time: String
)