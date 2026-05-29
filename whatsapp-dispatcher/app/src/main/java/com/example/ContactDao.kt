package com.example

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY id ASC")
    fun getAllContactsFlow(): Flow<List<ContactMessage>>

    @Query("SELECT * FROM contacts WHERE status = 'Pending' ORDER BY id ASC")
    suspend fun getPendingContacts(): List<ContactMessage>

    @Query("SELECT COUNT(*) FROM contacts WHERE status = 'Pending'")
    fun getPendingCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactMessage>)

    @Query("UPDATE contacts SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Update
    suspend fun update(contact: ContactMessage)

    @Query("DELETE FROM contacts")
    suspend fun deleteAll()

    @Query("DELETE FROM contacts WHERE status = 'Sent'")
    suspend fun deleteSentContacts()
}
