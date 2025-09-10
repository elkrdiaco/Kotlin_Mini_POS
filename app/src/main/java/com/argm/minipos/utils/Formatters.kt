package com.argm.minipos.utils

import java.text.NumberFormat
import java.util.Locale

fun formatPrice(price: Double): String {
    val format = NumberFormat.getNumberInstance(Locale("es", "ES"))
    format.maximumFractionDigits = 2
    format.minimumFractionDigits = 2
    return format.format(price)
}
