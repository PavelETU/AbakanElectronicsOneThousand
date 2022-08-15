package com.abakan.electronics.one.thousand

import java.util.*

const val NAME_OF_THE_DEVICE = "Abakan Electronics 1000"
val DEFAULT_UUID_FOR_CUSTOM_DEVICES: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
const val SAMPLE_RATE = 4000
const val ZERO_OFFSET = 74
// Changed from const val for testing
var FFT_SAMPLE_SIZE = 6400
// Changed from const val for testing
var BUFFER_SIZE = 128