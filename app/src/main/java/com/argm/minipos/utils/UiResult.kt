package com.argm.minipos.utils

sealed class UiResult<T>(
    open val data: T? = null,
    open val message: String? = null
) {
    class Success<T>(override val data: T) : UiResult<T>(data = data, message = null)

    class Error<T>(override val message: String, override val data: T? = null) : UiResult<T>(data = data, message = message)
    
    class Loading<T> : UiResult<T>(data = null, message = null)
}
