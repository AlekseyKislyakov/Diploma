package com.example.diploma.entities

data class Integrity(
    val name: String,
    val portNumber: String,
    var started: Boolean,
    var startedTime: Long = 0L,
    var workTime: Long = 0L,
    var broken: Boolean = false
)