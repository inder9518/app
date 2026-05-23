package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM hidden_apps ORDER BY addedAt DESC")
    fun getAllHiddenApps(): Flow<List<HiddenApp>>

    @Query("SELECT * FROM hidden_apps")
    suspend fun getHiddenAppsList(): List<HiddenApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenApp(hiddenApp: HiddenApp)

    @Query("DELETE FROM hidden_apps WHERE packageName = :packageName")
    suspend fun deleteHiddenAppByPackage(packageName: String)

    @Query("SELECT value FROM vault_config WHERE `key` = :key LIMIT 1")
    suspend fun getConfigValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: VaultConfig)
}
