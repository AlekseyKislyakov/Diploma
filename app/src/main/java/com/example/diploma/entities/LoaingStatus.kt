package com.example.diploma.entities

enum class LoadingStatus {
    NONE,
    LOADING,
    SUCCESS,
    FAIL
}

fun LoadingStatus.simplify(): String {
    return when (this) {
        LoadingStatus.NONE -> "1"
        LoadingStatus.SUCCESS -> "0"
        LoadingStatus.FAIL -> "0"
        else -> ""
    }
}

fun LoadingStatus.toBoolean(): Boolean {
    return when (this) {
        LoadingStatus.NONE -> false
        LoadingStatus.SUCCESS -> true
        LoadingStatus.FAIL -> false
        else -> false
    }
}