package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_articles")
data class NewsArticle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String,
    val category: String,       // "Satire", "Real-ish", "Sci-Fi", "Alternate History"
    val isFake: Boolean,        // True if it is fake/ satirical news, false if it is a wacky real news story
    val author: String = "Global Satire Desk",
    val timestamp: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false,
    val factCheckStatus: String = "SATIRICAL PIECE", // e.g., "100% PARODY", "VERIFIED DEBUNKED", "ACTUALLY TRUE FACT"
    val likesCount: Int = 0,
    val isUserCreated: Boolean = false
)
