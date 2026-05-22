package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_articles ORDER BY timestamp DESC")
    fun getAllArticlesFlow(): Flow<List<NewsArticle>>

    @Query("SELECT * FROM news_articles WHERE id = :id LIMIT 1")
    suspend fun getArticleById(id: Int): NewsArticle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsArticle>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: NewsArticle): Long

    @Update
    suspend fun updateArticle(article: NewsArticle)

    @Query("UPDATE news_articles SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmarkStatus(id: Int, isBookmarked: Boolean)

    @Query("UPDATE news_articles SET likesCount = likesCount + :increment WHERE id = :id")
    suspend fun incrementLikes(id: Int, increment: Int)

    @Query("DELETE FROM news_articles WHERE id = :id")
    suspend fun deleteArticleById(id: Int)

    @Query("DELETE FROM news_articles WHERE isUserCreated = 1")
    suspend fun clearUserCreatedArticles()
}
