package com.example.diploma.ext

fun Float.prettyValue(): String {
    return String.format("%.2f", this)
}