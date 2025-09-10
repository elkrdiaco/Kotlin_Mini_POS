package com.argm.minipos.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_deposits",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["rut"],
            childColumns = ["customerRut"],
            onDelete = ForeignKey.CASCADE // O la acción que prefieras
        )
    ],
    indices = [Index(value = ["customerRut"])]
)
data class PendingDeposit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerRut: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis()
)
