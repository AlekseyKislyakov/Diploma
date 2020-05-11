package com.example.diploma.entities

data class Record(
    val portNumber: String,
    var started: Boolean,
    var startedTime: Long = 0L,
    var workTime: Long = 0L,
    val name: String = "Record",
    val frequency: Int = 10,
    val fileName: String = "file",
    val map: Map<String, String> = mapOf()
): Entity