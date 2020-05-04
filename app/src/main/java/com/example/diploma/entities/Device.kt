package com.example.diploma.entities

import android.bluetooth.BluetoothDevice

data class Device(
    val device: BluetoothDevice,
    var status: LoadingStatus
)