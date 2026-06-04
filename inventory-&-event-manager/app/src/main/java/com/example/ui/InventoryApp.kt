package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Event
import com.example.data.EventItemWithDetails
import com.example.data.Inventory
import com.example.data.InventoryItem
import com.example.utils.InventoryExporter
import com.example.viewmodel.AllocationResult
import com.example.viewmodel.InventoryViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryApp(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // View states
    val inventories by viewModel.allInventories.collectAsStateWithLifecycle()
    val items by viewModel.allInventoryItems.collectAsStateWithLifecycle()
    val events by viewModel.allEvents.collectAsStateWithLifecycle()
    val allEventItems by viewModel.allEventItems.collectAsStateWithLifecycle()
    val remainingStock by viewModel.remainingStockMap.collectAsStateWithLifecycle()
    val eventDetailsMap by viewModel.eventItemsWithDetailsMap.collectAsStateWithLifecycle()

    val selectedEventId by viewModel.selectedEventId.collectAsStateWithLifecycle()
    val selectedEventItems by viewModel.selectedEventItems.collectAsStateWithLifecycle()

    val selectedInventoryId by viewModel.selectedInventoryId.collectAsStateWithLifecycle()
    val selectedInventoryItems by viewModel.selectedInventoryItems.collectAsStateWithLifecycle()

    val inventoryNameMap = remember(inventories) {
        inventories.associate { it.id to it.name }
    }

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Stock, 1 = Events

    // Dialog state
    var showEditItemDialog by remember { mutableStateOf<InventoryItem?>(null) }
    var showAddAllocationDialog by remember { mutableStateOf<Event?>(null) }
    var showEditEventDialog by remember { mutableStateOf<Event?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Stock & Event Manager",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val targetItems = if (selectedInventoryId != null) selectedInventoryItems else items
                            if (targetItems.isEmpty()) {
                                Toast.makeText(context, "No items in this inventory to export!", Toast.LENGTH_SHORT).show()
                            } else {
                                InventoryExporter.printSimpleInventoryPdf(context, targetItems)
                            }
                        },
                        modifier = Modifier.testTag("pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Simple PDF Report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            val targetItems = if (selectedInventoryId != null) selectedInventoryItems else items
                            if (targetItems.isEmpty()) {
                                Toast.makeText(context, "No items to print!", Toast.LENGTH_SHORT).show()
                            } else {
                                InventoryExporter.printInventoryReport(
                                    context = context,
                                    items = targetItems,
                                    events = events,
                                    eventItemMap = eventDetailsMap,
                                    remainingMap = remainingStock
                                )
                            }
                        },
                        modifier = Modifier.testTag("print_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print report",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            val targetItems = if (selectedInventoryId != null) selectedInventoryItems else items
                            if (targetItems.isEmpty()) {
                                Toast.makeText(context, "No items to share!", Toast.LENGTH_SHORT).show()
                            } else {
                                InventoryExporter.shareCsvReport(
                                    context = context,
                                    items = targetItems,
                                    remainingMap = remainingStock
                                )
                            }
                        },
                        modifier = Modifier.testTag("share_csv_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share CSV",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_navigation")
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Storage, contentDescription = "Inventory Tab") },
                    label = { Text("Stock") },
                    modifier = Modifier.testTag("nav_stock_tab")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Event, contentDescription = "Events Tab") },
                    label = { Text("Events") },
                    modifier = Modifier.testTag("nav_events_tab")
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Dashboard Summary Header
            val visibleItems = if (selectedInventoryId == null) items else selectedInventoryItems
            SummaryHeaderCard(
                totalItems = visibleItems.size,
                totalStock = visibleItems.sumOf { it.quantity },
                remainingStock = visibleItems.sumOf { remainingStock[it.id] ?: 0 }
            )

            HorizontalDivider()

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "TabContentAnimation"
            ) { tab ->
                when (tab) {
                    0 -> {
                        StockManagerScreen(
                            inventories = inventories,
                            selectedInventoryId = selectedInventoryId,
                            selectedInventoryItems = selectedInventoryItems,
                            remainingStock = remainingStock,
                            allItems = items,
                            onAddInventory = { name -> viewModel.addInventory(name) },
                            onEditInventory = { inventory, newName -> viewModel.updateInventory(inventory.id, newName) },
                            onDeleteInventory = { inventory -> viewModel.deleteInventory(inventory) },
                            onSelectInventory = { id -> viewModel.selectInventory(id) },
                            onAddItem = { inventoryId, name, qty -> viewModel.addInventoryItem(inventoryId, name, qty) },
                            onEditClick = { item -> showEditItemDialog = item },
                            onDeleteClick = { item -> viewModel.deleteInventoryItem(item) }
                        )
                    }
                    1 -> {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val isWideScreen = maxWidth > 650.dp
                            
                            EventsManagerScreen(
                                events = events,
                                items = items,
                                remainingStock = remainingStock,
                                eventDetailsMap = eventDetailsMap,
                                selectedEventId = selectedEventId,
                                selectedEventItems = selectedEventItems,
                                isWideScreen = isWideScreen,
                                onAddEvent = { name -> viewModel.addEvent(name) },
                                onDeleteEvent = { event -> viewModel.deleteEvent(event) },
                                onSelectEvent = { eventId -> viewModel.selectEvent(eventId) },
                                onEditEvent = { event -> showEditEventDialog = event },
                                onExportEvent = { event ->
                                    InventoryExporter.shareEventCsvReport(
                                        context = context,
                                        event = event,
                                        allocations = eventDetailsMap[event.id] ?: emptyList()
                                    )
                                },
                                onRemoveAllocation = { eventItem ->
                                    scope.launch {
                                        viewModel.allocateItemToEvent(
                                            eventId = eventItem.eventId,
                                            inventoryItemId = eventItem.inventoryItemId,
                                            quantity = 0
                                        )
                                    }
                                },
                                onAddAllocationClick = { event -> showAddAllocationDialog = event }
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Item Dialog
    showEditItemDialog?.let { item ->
        EditItemDialog(
            item = item,
            allocatedCount = viewModel.getAllocatedCount(item.id),
            onDismiss = { showEditItemDialog = null },
            onSave = { updatedName, updatedQty ->
                val result = viewModel.updateInventoryItem(item.id, updatedName, updatedQty)
                if (result) {
                    showEditItemDialog = null
                    Toast.makeText(context, "Item updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        "Error: Stock quantity cannot be less than allocated quantity (${viewModel.getAllocatedCount(item.id)})!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }

    // Allocation Dialog
    showAddAllocationDialog?.let { event ->
        AddAllocationDialog(
            event = event,
            items = items,
            inventories = inventories,
            remainingStock = remainingStock,
            currentEventAllocations = eventDetailsMap[event.id] ?: emptyList(),
            onDismiss = { showAddAllocationDialog = null },
            onAllocate = { itemId, quantity ->
                scope.launch {
                    val result = viewModel.allocateItemToEvent(event.id, itemId, quantity)
                    when (result) {
                        is AllocationResult.Success -> {
                            showAddAllocationDialog = null
                            Toast.makeText(context, "Stock allocated to event!", Toast.LENGTH_SHORT).show()
                        }
                        is AllocationResult.Error -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
    }

    showEditEventDialog?.let { event ->
        EditEventDialog(
            event = event,
            onDismiss = { showEditEventDialog = null },
            onSave = { updatedName ->
                viewModel.updateEvent(event, updatedName)
                showEditEventDialog = null
                Toast.makeText(context, "Event name updated!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun SummaryHeaderCard(
    totalItems: Int,
    totalStock: Int,
    remainingStock: Int
) {
    val allocatedStock = totalStock - remainingStock

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "OVERALL STOCK DASHBOARD SUMMARY",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Total Stock
            StatDashboardCard(
                title = "Total Stock",
                value = "$totalStock",
                subtitle = "units",
                textColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            // Card 2: Available
            StatDashboardCard(
                title = "Available",
                value = "$remainingStock",
                subtitle = "units",
                textColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 3: Allocated
            StatDashboardCard(
                title = "Allocated",
                value = "$allocatedStock",
                subtitle = "units",
                textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            // Card 4: Total Types
            StatDashboardCard(
                title = "Total Types",
                value = "$totalItems",
                subtitle = "items",
                textColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatDashboardCard(
    title: String,
    value: String,
    subtitle: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    color = textColor
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

@Composable
fun StockManagerScreen(
    inventories: List<Inventory>,
    selectedInventoryId: Int?,
    selectedInventoryItems: List<InventoryItem>,
    remainingStock: Map<Int, Int>,
    allItems: List<InventoryItem>,
    onAddInventory: (String) -> Unit,
    onEditInventory: (Inventory, String) -> Unit,
    onDeleteInventory: (Inventory) -> Unit,
    onSelectInventory: (Int?) -> Unit,
    onAddItem: (Int, String, Int) -> Unit,
    onEditClick: (InventoryItem) -> Unit,
    onDeleteClick: (InventoryItem) -> Unit
) {
    var newInventoryName by remember { mutableStateOf("") }
    var renameInventoryTarget by remember { mutableStateOf<Inventory?>(null) }
    
    // Item Fields for selected inventory
    var itemName by remember { mutableStateOf("") }
    var itemQty by remember { mutableStateOf("") }
    val isItemFormValid = itemName.isNotBlank() && itemQty.isNotBlank() && (itemQty.toIntOrNull() ?: -1) >= 0

    if (selectedInventoryId == null) {
        // --- MULTIPLE INVENTORIES DASHBOARD VIEW ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Create New Inventory List",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newInventoryName,
                            onValueChange = { newInventoryName = it },
                            label = { Text("Inventory Name (e.g. Warehouse A, Store B)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_inventory_name"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onAddInventory(newInventoryName.trim())
                                newInventoryName = ""
                            },
                            enabled = newInventoryName.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_inventory_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Inventory")
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Registered Inventories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (inventories.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Empty inventories",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No inventories created yet.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Create one above to begin adding custom stock items.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(inventories, key = { it.id }) { inventory ->
                    val itemCount = allItems.count { it.inventoryId == inventory.id }
                    val totalQty = allItems.filter { it.inventoryId == inventory.id }.sumOf { it.quantity }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectInventory(inventory.id) }
                            .testTag("inventory_card_${inventory.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = "Stock Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = inventory.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$itemCount items inside (Total Qty: $totalQty)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { renameInventoryTarget = inventory },
                                    modifier = Modifier.testTag("edit_inventory_${inventory.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename Inventory",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteInventory(inventory) },
                                    modifier = Modifier.testTag("delete_inventory_${inventory.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Inventory",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Open Inventory",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    } else {
        // --- SPECIFIC INVENTORY DETAILED ITEMS VIEW ---
        val selectedInventory = inventories.find { it.id == selectedInventoryId }
        
        if (selectedInventory == null) {
            onSelectInventory(null)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header displaying selected inventory with a back button
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onSelectInventory(null) },
                            modifier = Modifier.testTag("back_to_inventories")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Inventories list"
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = selectedInventory.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Viewing items inside this inventory",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                }

                // Add Item Card for THIS specific inventory list
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Add New Item into ${selectedInventory.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = itemName,
                                onValueChange = { itemName = it },
                                label = { Text("Item Name") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_item_name"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = itemQty,
                                onValueChange = { itemQty = it.filter { char -> char.isDigit() } },
                                label = { Text("Total Quantity") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_item_qty"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val qty = itemQty.toIntOrNull() ?: 0
                                    onAddItem(selectedInventory.id, itemName.trim(), qty)
                                    itemName = ""
                                    itemQty = ""
                                },
                                enabled = isItemFormValid,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_item_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Inventory Item")
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Warehouse Item Stock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                if (selectedInventoryItems.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = "Empty stock",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No stock items registered.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "Use the form above to add items to this warehouse.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                } else {
                    items(selectedInventoryItems, key = { it.id }) { item ->
                        val rem = remainingStock[item.id] ?: item.quantity
                        val allocated = item.quantity - rem
                        val progress = if (item.quantity > 0) rem.toFloat() / item.quantity.toFloat() else 0f

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("item_card_${item.id}"),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left accent vertical bar based on remaining stock status
                                val accentColor = when {
                                    rem <= 0 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    rem <= 5 -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(6.dp)
                                        .background(accentColor)
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = item.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "#${item.id}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                SuggestionChip(
                                                    onClick = { },
                                                    label = { Text("Total: ${item.quantity}") }
                                                )
                                                SuggestionChip(
                                                    onClick = { },
                                                    label = { Text("Allocated: $allocated") },
                                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                                    )
                                                )
                                            }
                                        }

                                        Row {
                                            IconButton(
                                                onClick = { onEditClick(item) },
                                                modifier = Modifier.testTag("edit_item_${item.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Item",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteClick(item) },
                                                modifier = Modifier.testTag("delete_item_${item.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Item",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Remaining/Available section with Progress Bar
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Available Stock:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            color = if (rem <= 5) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "$rem units available",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (rem <= 5) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = if (rem <= 5) Color(0xFFE74C3C) else Color(0xFF2ECC71),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Rename Inventory Dialog
    renameInventoryTarget?.let { inventory ->
        RenameInventoryDialog(
            inventory = inventory,
            onDismiss = { renameInventoryTarget = null },
            onSave = { newName ->
                onEditInventory(inventory, newName)
                renameInventoryTarget = null
            }
        )
    }
}

@Composable
fun RenameInventoryDialog(
    inventory: Inventory,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(inventory.name) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Rename Inventory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Inventory Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(name.trim()) },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun EventsManagerScreen(
    events: List<Event>,
    items: List<InventoryItem>,
    remainingStock: Map<Int, Int>,
    eventDetailsMap: Map<Int, List<EventItemWithDetails>>,
    selectedEventId: Int?,
    selectedEventItems: List<EventItemWithDetails>,
    isWideScreen: Boolean,
    onAddEvent: (String) -> Unit,
    onDeleteEvent: (Event) -> Unit,
    onSelectEvent: (Int?) -> Unit,
    onEditEvent: (Event) -> Unit,
    onExportEvent: (Event) -> Unit,
    onRemoveAllocation: (EventItemWithDetails) -> Unit,
    onAddAllocationClick: (Event) -> Unit
) {
    if (isWideScreen) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1.2f)) {
                EventListSection(
                    events = events,
                    eventDetailsMap = eventDetailsMap,
                    selectedEventId = selectedEventId,
                    onAddEvent = onAddEvent,
                    onDeleteEvent = onDeleteEvent,
                    onSelectEvent = onSelectEvent,
                    onEditEvent = onEditEvent
                )
            }
            VerticalDivider()
            Box(modifier = Modifier.weight(1.8f)) {
                val currentEvent = events.find { it.id == selectedEventId }
                if (currentEvent != null) {
                    EventDetailPage(
                        event = currentEvent,
                        items = selectedEventItems,
                        remainingStock = remainingStock,
                        onBack = { onSelectEvent(null) },
                        onAddAllocationClick = { onAddAllocationClick(currentEvent) },
                        onRemoveAllocation = onRemoveAllocation,
                        onEditEvent = onEditEvent,
                        onExportEvent = onExportEvent,
                        showBackButton = false
                    )
                } else {
                    EmptySelectionState("Select an event to view detail allocations & calculate remaining quantities.")
                }
            }
        }
    } else {
        val currentEvent = events.find { it.id == selectedEventId }
        AnimatedContent(
            targetState = currentEvent,
            transitionSpec = {
                if (targetState != null) {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> width } + fadeOut()
                }
            },
            label = "MobileEventDetailsAnimation"
        ) { activeEvent ->
            if (activeEvent != null) {
                EventDetailPage(
                    event = activeEvent,
                    items = selectedEventItems,
                    remainingStock = remainingStock,
                    onBack = { onSelectEvent(null) },
                    onAddAllocationClick = { onAddAllocationClick(activeEvent) },
                    onRemoveAllocation = onRemoveAllocation,
                    onEditEvent = onEditEvent,
                    onExportEvent = onExportEvent,
                    showBackButton = true
                )
            } else {
                EventListSection(
                    events = events,
                    eventDetailsMap = eventDetailsMap,
                    selectedEventId = null,
                    onAddEvent = onAddEvent,
                    onDeleteEvent = onDeleteEvent,
                    onSelectEvent = onSelectEvent,
                    onEditEvent = onEditEvent
                )
            }
        }
    }
}

@Composable
fun EventListSection(
    events: List<Event>,
    eventDetailsMap: Map<Int, List<EventItemWithDetails>>,
    selectedEventId: Int?,
    onAddEvent: (String) -> Unit,
    onDeleteEvent: (Event) -> Unit,
    onSelectEvent: (Int) -> Unit,
    onEditEvent: (Event) -> Unit
) {
    var eventName by remember { mutableStateOf("") }
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Create New Event",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = eventName,
                        onValueChange = { eventName = it },
                        label = { Text("Event Name (e.g. Wedding, Party)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_event_name"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onAddEvent(eventName.trim())
                            eventName = ""
                        },
                        enabled = eventName.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_event_button")
                    ) {
                        Text("Create Event")
                    }
                }
            }
        }

        item {
            Text(
                text = "Active Events List",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (events.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = "No events",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No events created yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            items(events, key = { it.id }) { event ->
                val dateStr = sdf.format(Date(event.timestamp))
                val allocations = eventDetailsMap[event.id] ?: emptyList()
                val totalAllocated = allocations.sumOf { it.allocatedQuantity }
                val itemTypes = allocations.size
                val isSelected = event.id == selectedEventId

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectEvent(event.id) }
                        .testTag("event_card_${event.id}"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Created: $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                              )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "$itemTypes types allocated",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Total Qty: $totalAllocated",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Light
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onEditEvent(event) },
                                modifier = Modifier.testTag("edit_event_${event.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Event Name",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { onDeleteEvent(event) },
                                modifier = Modifier.testTag("delete_event_${event.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Event",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "View detail",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventDetailPage(
    event: Event,
    items: List<EventItemWithDetails>,
    remainingStock: Map<Int, Int>,
    onBack: () -> Unit,
    onAddAllocationClick: () -> Unit,
    onRemoveAllocation: (EventItemWithDetails) -> Unit,
    onEditEvent: (Event) -> Unit,
    onExportEvent: (Event) -> Unit,
    showBackButton: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("event_detail_back")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Main tab list map")
                    }
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        IconButton(
                            onClick = { onEditEvent(event) },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_edit_event_detail_${event.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Event Name",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = "Event allocations and details",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onExportEvent(event) },
                    modifier = Modifier.testTag("btn_export_event_${event.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export Event allocations CSV",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Button(
                    onClick = onAddAllocationClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.testTag("btn_add_allocation")
                ) {
                    Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Select Item", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Inventories Dispatched to this Event:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Empty",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No items have been assigned to this event yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Click on 'Select Item' above to allocate items from the main stock.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items, key = { it.id }) { allocatedItem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("allocated_item_card_${allocatedItem.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = allocatedItem.itemName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Allocated: ${allocatedItem.allocatedQuantity} units",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "(Total: ${allocatedItem.totalQuantity})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val rem = remainingStock[allocatedItem.inventoryItemId] ?: 0
                                Text(
                                    text = "Remaining in Warehouse: $rem units left",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (rem <= 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { onRemoveAllocation(allocatedItem) },
                                modifier = Modifier.testTag("remove_allocation_${allocatedItem.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Remove item allocation",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditItemDialog(
    item: InventoryItem,
    allocatedCount: Int,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var qty by remember { mutableStateOf(item.quantity.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Edit Inventory Stock",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_dialog_name"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it.filter { char -> char.isDigit() } },
                    label = { Text("Total Quantity") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_dialog_qty"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "* Warning: $allocatedCount units of this stock are already allocated across events.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("edit_dialog_cancel")) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newQty = qty.toIntOrNull() ?: 0
                            onSave(name.trim(), newQty)
                        },
                        enabled = name.isNotBlank() && qty.isNotBlank() && (qty.toIntOrNull() ?: -1) >= 0,
                        modifier = Modifier.testTag("edit_dialog_save")
                    ) {
                        Text("Update")
                    }
                }
            }
        }
    }
}

@Composable
fun AddAllocationDialog(
    event: Event,
    items: List<InventoryItem>,
    inventories: List<Inventory>,
    remainingStock: Map<Int, Int>,
    currentEventAllocations: List<EventItemWithDetails>,
    onDismiss: () -> Unit,
    onAllocate: (Int, Int) -> Unit
) {
    var selectedItem by remember { mutableStateOf<InventoryItem?>(null) }
    var qtyString by remember { mutableStateOf("") }

    // Filter items to select from
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Event: ${event.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Allocation details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "1. Select Item from Warehouse Stock:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (items.isEmpty()) {
                    Text(
                        text = "No stock items available. Please add items in the warehouse stock screen first.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    // Simple custom scrollable selection list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(4.dp)
                    ) {
                        items(items) { item ->
                            val currentEventAllocated = currentEventAllocations.find { it.inventoryItemId == item.id }?.allocatedQuantity ?: 0
                            val rem = remainingStock[item.id] ?: 0
                            val totalAvailableForThis = rem + currentEventAllocated
                            val isSelected = selectedItem?.id == item.id

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable {
                                        selectedItem = item
                                        qtyString = if (currentEventAllocated > 0) currentEventAllocated.toString() else ""
                                    }
                                    .padding(8.dp)
                                    .testTag("select_item_${item.id}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val inventoryMap = remember(inventories) { inventories.associateBy { it.id } }
                                val invName = inventoryMap[item.inventoryId]?.name ?: "Unknown Inventory"
                                Text(
                                    text = "${item.name} ($invName)",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$totalAvailableForThis available",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalAvailableForThis <= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    if (currentEventAllocated > 0) {
                                        Text(
                                            text = "Already allocated to this event: $currentEventAllocated",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "2. Enter Allocation Quantity:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = qtyString,
                    onValueChange = { qtyString = it.filter { char -> char.isDigit() } },
                    label = { Text("Quantity Dispatched") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("allocation_quantity"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = selectedItem != null
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("allocation_dialog_cancel")) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val item = selectedItem
                            if (item != null) {
                                val dispatchQty = qtyString.toIntOrNull() ?: 0
                                onAllocate(item.id, dispatchQty)
                            }
                        },
                        enabled = selectedItem != null && qtyString.isNotBlank() && (qtyString.toIntOrNull() ?: 0) >= 0,
                        modifier = Modifier.testTag("allocation_dialog_save")
                    ) {
                        Text("Allocate")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySelectionState(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.TouchApp,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun EditEventDialog(
    event: Event,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(event.name) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Edit Event Name",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Event Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_event_name_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.testTag("edit_event_cancel")) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(name.trim())
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.testTag("edit_event_save")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
