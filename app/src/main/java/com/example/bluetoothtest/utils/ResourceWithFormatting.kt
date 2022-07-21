package com.example.bluetoothtest.utils

import android.content.Context
import com.example.bluetoothtest.READABLE_NAME_OF_THE_DEVICE

data class ResourceWithFormatting(
    val resource: Int,
    val formattingString: String? = READABLE_NAME_OF_THE_DEVICE
)

fun Context.getString(resourceWithFormatting: ResourceWithFormatting): String =
    resourceWithFormatting.formattingString?.run {
        getString(resourceWithFormatting.resource, resourceWithFormatting.formattingString)
    } ?: getString(resourceWithFormatting.resource)