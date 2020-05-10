package com.example.diploma.entities

data class MagnetRelay(
    val name: String,
    val portNumber: String,
    var started: Boolean,
    var startedTime: Long = 0L,
    var workTime: Long = 0L
)