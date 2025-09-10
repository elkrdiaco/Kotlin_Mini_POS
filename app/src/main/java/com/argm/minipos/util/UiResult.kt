package com.argm.minipos.util

sealed class UiResult<T>(
    // Estos campos base pueden ser útiles si quieres acceder a ellos
    // directamente desde una variable de tipo UiResult<T> sin castear,
    // pero las subclases deben definirlos consistentemente.
    open val data: T? = null,
    open val message: String? = null
) {
    // data: T asegura que en caso de éxito, 'data' no es null.
    // Si la data en sí misma puede ser opcional incluso en éxito, entonces T? sería data: T?
    class Success<T>(override val data: T) : UiResult<T>(data = data, message = null)

    class Error<T>(override val message: String, override val data: T? = null) : UiResult<T>(data = data, message = message)
    
    class Loading<T> : UiResult<T>(data = null, message = null) // Loading no lleva data ni mensaje específico

    fun <R> map(transform: (T) -> R): UiResult<R> {
        return when (this) {
            is Success -> {
                // 'data' aquí es de tipo T (no T?) porque Success lo define así.
                UiResult.Success(transform(data))
            }
            is Error -> UiResult.Error(message, null) // El 'data' original del error (tipo T) no se transforma. Se pasa null para el nuevo tipo R.
            is Loading -> UiResult.Loading()
        }
    }
}
