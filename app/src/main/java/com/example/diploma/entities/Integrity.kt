package com.example.diploma.entities

data class Integrity(
    val name: String = "INT",
    val portNumber: String,
    var started: LoadingStatus,
    var startedTime: Long = 0L,
    var workTime: Long = 0L,
    var broken: Boolean = false,
    val explicitName: String = ""
) : Entity