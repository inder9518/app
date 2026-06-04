package com.example.data

import kotlinx.coroutines.flow.Flow

class InventoryRepository(private val inventoryDao: InventoryDao) {
    val allInventories: Flow<List<Inventory>> = inventoryDao.getAllInventories()
    val allInventoryItems: Flow<List<InventoryItem>> = inventoryDao.getAllInventoryItems()
    val allEvents: Flow<List<Event>> = inventoryDao.getAllEvents()
    val allEventItems: Flow<List<EventItem>> = inventoryDao.getAllEventItems()

    suspend fun insertInventory(inventory: Inventory): Long = inventoryDao.insertInventory(inventory)
    suspend fun updateInventory(inventory: Inventory) = inventoryDao.updateInventory(inventory)
    suspend fun deleteInventory(inventory: Inventory) = inventoryDao.deleteInventory(inventory)
    suspend fun getInventoryById(id: Int): Inventory? = inventoryDao.getInventoryById(id)
    fun getInventoryItemsForInventory(inventoryId: Int): Flow<List<InventoryItem>> =
        inventoryDao.getInventoryItemsForInventory(inventoryId)

    suspend fun insertInventoryItem(item: InventoryItem) = inventoryDao.insertInventoryItem(item)
    suspend fun updateInventoryItem(item: InventoryItem) = inventoryDao.updateInventoryItem(item)
    suspend fun deleteInventoryItem(item: InventoryItem) = inventoryDao.deleteInventoryItem(item)

    suspend fun insertEvent(event: Event): Long = inventoryDao.insertEvent(event)
    suspend fun updateEvent(event: Event) = inventoryDao.updateEvent(event)
    suspend fun deleteEvent(event: Event) = inventoryDao.deleteEvent(event)

    suspend fun insertEventItem(eventItem: EventItem) = inventoryDao.insertEventItem(eventItem)
    suspend fun updateEventItem(eventItem: EventItem) = inventoryDao.updateEventItem(eventItem)
    suspend fun deleteEventItem(eventItem: EventItem) = inventoryDao.deleteEventItem(eventItem)

    fun getEventItemsWithDetails(eventId: Int): Flow<List<EventItemWithDetails>> =
        inventoryDao.getEventItemsWithDetails(eventId)
}
