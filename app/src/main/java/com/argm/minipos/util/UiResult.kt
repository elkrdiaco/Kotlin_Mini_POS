package com.argm.minipos.util

sealed class UiResult<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T?) : UiResult<T>(data)
    class Error<T>(message: String, data: T? = null) : UiResult<T>(data, message)
}