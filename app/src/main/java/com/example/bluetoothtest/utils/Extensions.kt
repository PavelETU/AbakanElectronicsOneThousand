package com.example.bluetoothtest.utils

import com.example.bluetoothtest.ZERO_OFFSET

fun ByteArray.shiftValuesByZeroOffset(): ByteArray {
    return map { (it.toInt() - ZERO_OFFSET).toByte() }.toByteArray()
}