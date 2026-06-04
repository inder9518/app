package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AllocationResult {
    object Success : AllocationResult
    data class Error(val message: String) : AllocationResult
}

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    val repository: InventoryRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = InventoryRepository(database.inventoryDao())
    }

    // State flows from Room
    val allInventories: StateFlow<List<Inventory>> = repository.allInventories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInventoryItems: StateFlow<List<InventoryItem>> = repository.allInventoryItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEvents: StateFlow<List<Event>> = repository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEventItems: StateFlow<List<EventItem>> = repository.allEventItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected inventory state
    private val _selectedInventoryId = MutableStateFlow<Int?>(null)
    val selectedInventoryId: StateFlow<Int?> = _selectedInventoryId.asStateFlow()

    fun selectInventory(inventoryId: Int?) {
        _selectedInventoryId.value = inventoryId
    }

    val selectedInventoryItems: StateFlow<List<InventoryItem>> = combine(
        selectedInventoryId,
        allInventoryItems
    ) { inventoryId, allItems ->
        if (inventoryId == null) emptyList() else allItems.filter { it.inventoryId == inventoryId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Map of [inventoryItemId -> remainingAvailableQuantity]
    // Remaining = Total - allocated across all events
    val remainingStockMap: StateFlow<Map<Int, Int>> = combine(
        allInventoryItems,
        allEventItems
    ) { inventoryItems, eventItems ->
        val allocatedCounts = eventItems.groupBy { it.inventoryItemId }
            .mapValues { (_, allocations) -> allocations.sumOf { it.quantity } }

        inventoryItems.associate { item ->
            val allocated = allocatedCounts[item.id] ?: 0
            item.id to (item.quantity - allocated).coerceAtLeast(0)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Map of [eventId -> List of EventItemWithDetails] (all allocations with item details)
    val eventItemsWithDetailsMap: StateFlow<Map<Int, List<EventItemWithDetails>>> = combine(
        allEventItems,
        allInventoryItems
    ) { eventItems, inventoryItems ->
        val itemMap = inventoryItems.associateBy { it.id }
        eventItems.map { ei ->
            val item = itemMap[ei.inventoryItemId]
            EventItemWithDetails(
                id = ei.id,
                eventId = ei.eventId,
                inventoryItemId = ei.inventoryItemId,
                itemName = item?.name ?: "Unknown Item",
                totalQuantity = item?.quantity ?: 0,
                allocatedQuantity = ei.quantity
            )
        }.groupBy { it.eventId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Main screen tabs & state
    private val _selectedEventId = MutableStateFlow<Int?>(null)
    val selectedEventId: StateFlow<Int?> = _selectedEventId.asStateFlow()

    fun selectEvent(eventId: Int?) {
        _selectedEventId.value = eventId
    }

    // List of items allocated to the currently selected event
    val selectedEventItems: StateFlow<List<EventItemWithDetails>> = combine(
        selectedEventId,
        eventItemsWithDetailsMap
    ) { eventId, detailsMap ->
        if (eventId == null) emptyList() else detailsMap[eventId] ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Utilities to count allocations
    fun getAllocatedCount(inventoryItemId: Int): Int {
        return allEventItems.value
            .filter { it.inventoryItemId == inventoryItemId }
            .sumOf { it.quantity }
    }

    fun checkMaxAllocatable(inventoryItemId: Int, forEventId: Int): Int {
        val item = allInventoryItems.value.find { it.id == inventoryItemId } ?: return 0
        val totalAllocatedInOthers = allEventItems.value
            .filter { it.inventoryItemId == inventoryItemId && it.eventId != forEventId }
            .sumOf { it.quantity }
        return (item.quantity - totalAllocatedInOthers).coerceAtLeast(0)
    }

    // --- Actions ---

    fun addInventory(name: String) {
        viewModelScope.launch {
            repository.insertInventory(Inventory(name = name))
        }
    }

    fun updateInventory(id: Int, newName: String) {
        viewModelScope.launch {
            repository.updateInventory(Inventory(id = id, name = newName))
        }
    }

    fun deleteInventory(inventory: Inventory) {
        viewModelScope.launch {
            if (_selectedInventoryId.value == inventory.id) {
                _selectedInventoryId.value = null
            }
            repository.deleteInventory(inventory)
        }
    }

    fun addInventoryItem(inventoryId: Int, name: String, quantity: Int) {
        viewModelScope.launch {
            repository.insertInventoryItem(InventoryItem(inventoryId = inventoryId, name = name, quantity = quantity))
        }
    }

    fun updateInventoryItem(id: Int, name: String, quantity: Int): Boolean {
        val currentAllocated = getAllocatedCount(id)
        if (quantity < currentAllocated) {
            return false // Cannot reduce total below currently allocated
        }
        val existing = allInventoryItems.value.find { it.id == id } ?: return false
        viewModelScope.launch {
            repository.updateInventoryItem(existing.copy(name = name, quantity = quantity))
        }
        return true
    }

    fun deleteInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.deleteInventoryItem(item)
        }
    }

    fun addEvent(name: String) {
        viewModelScope.launch {
            repository.insertEvent(Event(name = name))
        }
    }

    fun updateEvent(event: Event, newName: String) {
        viewModelScope.launch {
            repository.updateEvent(event.copy(name = newName))
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            if (_selectedEventId.value == event.id) {
                _selectedEventId.value = null
            }
            repository.deleteEvent(event)
        }
    }

    suspend fun allocateItemToEvent(eventId: Int, inventoryItemId: Int, quantity: Int): AllocationResult {
        if (quantity <= 0) {
            val existing = allEventItems.value.find { it.eventId == eventId && it.inventoryItemId == inventoryItemId }
            if (existing != null) {
                repository.deleteEventItem(existing)
            }
            return AllocationResult.Success
        }

        val maxAllowed = checkMaxAllocatable(inventoryItemId, eventId)
        if (quantity > maxAllowed) {
            return AllocationResult.Error("Only $maxAllowed items are available in stock to allocate.")
        }

        val existing = allEventItems.value.find { it.eventId == eventId && it.inventoryItemId == inventoryItemId }
        if (existing != null) {
            repository.updateEventItem(existing.copy(quantity = quantity))
        } else {
            repository.insertEventItem(EventItem(eventId = eventId, inventoryItemId = inventoryItemId, quantity = quantity))
        }
        return AllocationResult.Success
    }
}
