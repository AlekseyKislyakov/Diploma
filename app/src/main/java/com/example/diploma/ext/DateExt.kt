package com.example.diploma.ext

import java.text.SimpleDateFormat
import java.util.*

fun Long.convertLongToTime(): String {
    val date = Date(this)
    val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return format.format(date)
}

fun Long.convertLongToTimeUTC(): String {
    val date = Date(this)
    val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(date)
}