package com.example.diploma.ext


fun String.toShapeId(): String {
    return when(this) {
        "SIN" -> "0"
        "SAW" -> "1"
        "SQU" -> "2"
        "LIN" -> "3"
        else -> ""
    }
}