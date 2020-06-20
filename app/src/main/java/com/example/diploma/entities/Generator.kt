package com.example.diploma.entities

data class Generator(
    val portNumber: String,
    var started: LoadingStatus,
    var startedTime: Long = 0L,
    var workTime: Long = 0L,
    val name: String = "GEN",
    var shape: String = "SIN",
    var ampl: Float = 0.0f,
    val maxAmpl: Float = 12.0f,
    val maxFreq: Float = 100.0f,
    var freq: Float = 0.0f,
    val explicitName: String = ""
) : Entity
