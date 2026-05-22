package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class NewsEntity(
    @PrimaryKey val url: String,
    val title: String,
    val description: String,
    val sourceName: String,
    val urlToImage: String,
    val publishedAt: String,
    val category: String,
    val cachedAt: Long
)
