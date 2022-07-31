package com.abakan.electronics.one.thousand.utils

import com.abakan.electronics.one.thousand.ZERO_OFFSET

fun ByteArray.shiftValuesByZeroOffset(): ByteArray {
    return map { (it.toInt() - ZERO_OFFSET).toByte() }.toByteArray()
}