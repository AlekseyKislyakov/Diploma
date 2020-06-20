package com.example.diploma.entities

data class MagnetRelay(
    val portNumber: String,
    var started: Boolean,
    var startedTime: Long = 0L,
    var workTime: Long = 0L,
    val name: String = "REL",
    val explicitName: String = ""
): Entity