package com.argm.minipos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey
    val rut: String,
    val name: String?,
    val balance: Double = 0.0
)
