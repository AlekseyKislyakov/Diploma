package com.example.diploma.entities

data class Generator(
    val portNumber: String,
    var started: Boolean,
    var startedTime: Long = 0L,
    var workTime: Long = 0L,
    val name: String = "Gener",
    val shape: SignalShape = SignalShape.SIN,
    val ampl: Float = 0.0f,
    val freq: Float = 0.0f
) : Entity

enum class SignalShape {
    SIN,
    SAW,
    SQU
}