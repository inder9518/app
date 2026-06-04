package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class EventItemWithDetails(
    val id: Int,
    val eventId: Int,
    val inventoryItemId: Int,
    val itemName: String,
    val totalQuantity: Int,
    val allocatedQuantity: Int
)

@Dao
interface InventoryDao {

    // --- Inventories ---
    @Query("SELECT * FROM inventories ORDER BY name ASC")
    fun getAllInventories(): Flow<List<Inventory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(inventory: Inventory): Long

    @Update
    suspend fun updateInventory(inventory: Inventory)

    @Delete
    suspend fun deleteInventory(inventory: Inventory)

    @Query("SELECT * FROM inventories WHERE id = :id")
    suspend fun getInventoryById(id: Int): Inventory?

    // --- Inventory Items ---
    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun getAllInventoryItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE inventoryId = :inventoryId ORDER BY name ASC")
    fun getInventoryItemsForInventory(inventoryId: Int): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem): Long

    @Update
    suspend fun updateInventoryItem(item: InventoryItem)

    @Delete
    suspend fun deleteInventoryItem(item: InventoryItem)

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getInventoryItemById(id: Int): InventoryItem?

    // --- Events ---
    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<Event>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    // --- Event Items ---
    @Query("SELECT * FROM event_items WHERE eventId = :eventId")
    fun getEventItemsForEvent(eventId: Int): Flow<List<EventItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventItem(eventItem: EventItem): Long

    @Update
    suspend fun updateEventItem(eventItem: EventItem)

    @Delete
    suspend fun deleteEventItem(eventItem: EventItem)

    @Query("""
        SELECT ei.id, ei.eventId, ei.inventoryItemId, ii.name AS itemName, ii.quantity AS totalQuantity, ei.quantity AS allocatedQuantity 
        FROM event_items ei
        INNER JOIN inventory_items ii ON ei.inventoryItemId = ii.id
        WHERE ei.eventId = :eventId
        ORDER BY ii.name ASC
    """)
    fun getEventItemsWithDetails(eventId: Int): Flow<List<EventItemWithDetails>>

    // All Event Items (to calculate complete allocated map easily in ViewModel)
    @Query("SELECT * FROM event_items")
    fun getAllEventItems(): Flow<List<EventItem>>
}
