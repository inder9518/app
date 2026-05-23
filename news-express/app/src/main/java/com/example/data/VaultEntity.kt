package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_apps")
data class HiddenApp(
    @PrimaryKey val packageName: String,
    val appName: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_config")
data class VaultConfig(
    @PrimaryKey val key: String,
    val value: String
)
