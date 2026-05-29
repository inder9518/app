package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sNo: String,
    val phoneNumber: String,
    val customerName: String,
    val customMessage: String,
    var status: String // "Pending" or "Sent"
)
