package com.example.diploma.ext

import kotlin.math.round
import kotlin.math.roundToInt

fun Float.prettyValue(): String {
    return String.format("%.2f", this)
}

fun Float.toPrettyValue(): String {
    return if (round(this) == this) {
        roundToInt().toString()
    } else {
        val result = "%.2f".format(this).replace(",", ".")
        if (result.last() == '0') {
            return result.dropLast(1)
        }
        return result
    }
}

fun Float.cleverAmpl(): String {
    return round((this / 12.0) * 255).toInt().toString()
}

fun Float.cleverFreq(): String {
    return round(this * 6.67).toInt().toString()
}