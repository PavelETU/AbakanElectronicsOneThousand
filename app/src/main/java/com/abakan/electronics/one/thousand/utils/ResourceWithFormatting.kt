package com.abakan.electronics.one.thousand.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.abakan.electronics.one.thousand.READABLE_NAME_OF_THE_DEVICE

data class ResourceWithFormatting(
    val resource: Int,
    val formattingString: String? = READABLE_NAME_OF_THE_DEVICE
)

fun Context.getStringFromResource(resourceWithFormatting: ResourceWithFormatting): String =
    resourceWithFormatting.formattingString?.run {
        getString(resourceWithFormatting.resource, resourceWithFormatting.formattingString)
    } ?: getString(resourceWithFormatting.resource)

@Composable
fun ResourceWithFormatting.getStringForCompose(): String =
    formattingString?.run {
        stringResource(resource, formattingString)
    } ?: stringResource(resource)