package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Query("SELECT * FROM articles WHERE category = :category ORDER BY publishedAt DESC")
    fun getArticlesByCategory(category: String): Flow<List<NewsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsEntity>)

    @Query("DELETE FROM articles WHERE category = :category")
    suspend fun clearByCategory(category: String)

    @Query("SELECT * FROM articles WHERE title LIKE :searchQuery OR description LIKE :searchQuery ORDER BY publishedAt DESC")
    fun searchArticles(searchQuery: String): Flow<List<NewsEntity>>
}
