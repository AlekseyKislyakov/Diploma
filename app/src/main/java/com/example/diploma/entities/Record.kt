package com.example.diploma.entities

data class Record(
    val portNumber: String,
    var started: LoadingStatus,
    var startedTime: Long = 0L,
    var workTime: Long = 0L,
    val name: String = "REC",
    var period: Int = 200,
    val minPeriod: Int = 20,
    var fileName: String = "file",
    val maxAmpl: Float = 50.0f,
    var map: MutableMap<Long, Float> = mutableMapOf(),
    val explicitName: String = ""
): Entity