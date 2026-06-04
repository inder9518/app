package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.InventoryItem
import com.example.data.Event
import com.example.data.EventItemWithDetails
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object InventoryExporter {

    fun generateHtmlReport(
        items: List<InventoryItem>,
        events: List<Event>,
        eventItemMap: Map<Int, List<EventItemWithDetails>>,
        remainingMap: Map<Int, Int>
    ): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
        val generatedTime = sdf.format(Date())

        val totalItems = items.size
        val totalStockUnits = items.sumOf { it.quantity }
        val remainingStockUnits = items.sumOf { remainingMap[it.id] ?: 0 }
        val allocatedUnits = totalStockUnits - remainingStockUnits

        val htmlBuilder = StringBuilder()
        htmlBuilder.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Inventory &amp; Event Stock Report</title>
                <style>
                    body {
                        font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                        color: #1a1a1a;
                        margin: 20px;
                        padding: 0;
                        font-size: 14px;
                        line-height: 1.5;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                        border-bottom: 2px solid #5a6b7c;
                        padding-bottom: 15px;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 26px;
                        color: #1a252f;
                    }
                    .header p {
                        margin: 5px 0 0;
                        color: #7f8c8d;
                    }
                    .summary {
                        display: flex;
                        justify-content: space-between;
                        margin-bottom: 30px;
                        background: #f8f9fa;
                        border: 1px solid #e2e8f0;
                        border-radius: 8px;
                        padding: 15px 20px;
                    }
                    .summary-card {
                        text-align: center;
                        flex: 1;
                    }
                    .summary-card:not(:last-child) {
                        border-right: 1px solid #e2e8f0;
                    }
                    .summary-card h3 {
                        margin: 0;
                        font-size: 12px;
                        text-transform: uppercase;
                        color: #718096;
                        letter-spacing: 0.5px;
                    }
                    .summary-card p {
                        margin: 5px 0 0;
                        font-size: 20px;
                        font-weight: bold;
                        color: #2d3748;
                    }
                    h2 {
                        color: #2c3e50;
                        font-size: 18px;
                        margin-top: 30px;
                        border-bottom: 1px solid #ecf0f1;
                        padding-bottom: 5px;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 30px;
                    }
                    th {
                        background-color: #34495e;
                        color: #ffffff;
                        text-align: left;
                        padding: 10px;
                        font-weight: 600;
                    }
                    td {
                        padding: 10px;
                        border-bottom: 1px solid #e2e8f0;
                        color: #2d3748;
                    }
                    tr:nth-child(even) td {
                        background-color: #f8fafc;
                    }
                    .badge {
                        display: inline-block;
                        padding: 3px 8px;
                        border-radius: 4px;
                        font-size: 11px;
                        font-weight: bold;
                    }
                    .badge-active {
                        background-color: #d1fae5;
                        color: #065f46;
                    }
                    .badge-low {
                        background-color: #fee2e2;
                        color: #991b1b;
                    }
                    .event-section {
                        background: #fdfdfd;
                        border: 1px solid #edf2f7;
                        border-radius: 8px;
                        padding: 15px;
                        margin-bottom: 20px;
                    }
                    .event-title {
                        font-weight: bold;
                        font-size: 16px;
                        color: #2c3e50;
                        margin-top: 0;
                        margin-bottom: 10px;
                        display: flex;
                        justify-content: space-between;
                    }
                    .event-time {
                        font-size: 12px;
                        color: #7f8c8d;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>INVENTORY &amp; STOCK REPORT</h1>
                    <p>Generated on: $generatedTime</p>
                </div>

                <div class="summary">
                    <div class="summary-card">
                        <h3>Total Items</h3>
                        <p>$totalItems</p>
                    </div>
                    <div class="summary-card">
                        <h3>Total Main Stock</h3>
                        <p>$totalStockUnits</p>
                    </div>
                    <div class="summary-card">
                        <h3>Total Allocated</h3>
                        <p>$allocatedUnits</p>
                    </div>
                    <div class="summary-card">
                        <h3>Remaining Available</h3>
                        <p>$remainingStockUnits</p>
                    </div>
                </div>

                <h2>1. Main Inventory Stock Summary</h2>
                <table>
                    <thead>
                        <tr>
                            <th style="width: 10%;">ID</th>
                            <th style="width: 40%;">Item Name</th>
                            <th style="width: 16%; text-align: center;">Total Stock</th>
                            <th style="width: 16%; text-align: center;">Allocated Stock</th>
                            <th style="width: 18%; text-align: center;">Remaining Available</th>
                        </tr>
                    </thead>
                    <tbody>
        """.trimIndent())

        items.forEachIndexed { index, item ->
            val rem = remainingMap[item.id] ?: item.quantity
            val alloc = item.quantity - rem
            val badgeClass = if (rem <= 5) "badge-low" else "badge-active"
            htmlBuilder.append("""
                <tr>
                    <td>#${item.id}</td>
                    <td><strong>${item.name}</strong></td>
                    <td style="text-align: center;">${item.quantity}</td>
                    <td style="text-align: center; color: #7f8c8d;">${alloc}</td>
                    <td style="text-align: center;"><span class="badge $badgeClass">${rem} units left</span></td>
                </tr>
            """.trimIndent())
        }

        htmlBuilder.append("""
                    </tbody>
                </table>

                <h2>2. Event-Wise Allocations</h2>
        """.trimIndent())

        if (events.isEmpty()) {
            htmlBuilder.append("<p style='color:#7f8c8d;font-style:italic;'>No events created yet.</p>")
        } else {
            events.forEach { event ->
                val eventTimeStr = sdf.format(Date(event.timestamp))
                val allocations = eventItemMap[event.id] ?: emptyList()
                val totalAllocVal = allocations.sumOf { it.allocatedQuantity }

                htmlBuilder.append("""
                    <div class="event-section">
                        <div class="event-title">
                            <span>${event.name}</span>
                            <span class="event-time">$eventTimeStr</span>
                        </div>
                        <p style="margin: 0 0 10px 0; color:#5a6b7c; font-size:12px;">Allocated $totalAllocVal items to this event</p>
                """.trimIndent())

                if (allocations.isEmpty()) {
                    htmlBuilder.append("<p style='color:#a0aec0;font-style:italic;font-size:12px;margin: 5px 0 0 0;'>No items allocated to this event.</p>")
                } else {
                    htmlBuilder.append("""
                        <table style="margin-bottom: 0px; font-size: 13px;">
                            <thead>
                                <tr style="background:#eaedf1;">
                                    <th style="color:#2d3748; padding:6px 10px; font-size:12px; width:65%;">Item Name</th>
                                    <th style="color:#2d3748; padding:6px 10px; font-size:12px; width:35%; text-align:center;">Allocated Qty</th>
                                </tr>
                            </thead>
                            <tbody>
                    """.trimIndent())

                    allocations.forEach { allocation ->
                        htmlBuilder.append("""
                            <tr>
                                <td style="padding:6px 10px;">${allocation.itemName}</td>
                                <td style="padding:6px 10px; text-align:center; font-weight:bold;">${allocation.allocatedQuantity}</td>
                            </tr>
                        """.trimIndent())
                    }

                    htmlBuilder.append("""
                            </tbody>
                        </table>
                    """.trimIndent())
                }

                htmlBuilder.append("</div>")
            }
        }

        htmlBuilder.append("""
            </body>
            </html>
        """.trimIndent())

        return htmlBuilder.toString()
    }

    fun printInventoryReport(
        context: Context,
        items: List<InventoryItem>,
        events: List<Event>,
        eventItemMap: Map<Int, List<EventItemWithDetails>>,
        remainingMap: Map<Int, Int>
    ) {
        try {
            val htmlContent = generateHtmlReport(items, events, eventItemMap, remainingMap)
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val printAdapter = webView.createPrintDocumentAdapter("Inventory_Report_${System.currentTimeMillis()}")
                    printManager.print("Inventory and Event Report", printAdapter, PrintAttributes.Builder().build())
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            Toast.makeText(context, "Printing failure: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareCsvReport(
        context: Context,
        items: List<InventoryItem>,
        remainingMap: Map<Int, Int>
    ) {
        try {
            val csvBuilder = StringBuilder()
            csvBuilder.append("Item ID,Item Name,Total Stock,Allocated Quantity,Remaining Available\n")
            items.forEach { item ->
                val rem = remainingMap[item.id] ?: item.quantity
                val alloc = item.quantity - rem
                val escapedName = if (item.name.contains(",")) "\"${item.name}\"" else item.name
                csvBuilder.append("${item.id},$escapedName,${item.quantity},$alloc,$rem\n")
            }

            val tempDir = context.cacheDir
            val csvFile = File(tempDir, "Inventory_Stock_Report.csv")
            val writer = FileWriter(csvFile)
            writer.write(csvBuilder.toString())
            writer.flush()
            writer.close()

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, csvFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Inventory Stock CSV Report")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Download/Share Inventory Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "CSV Share failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareEventCsvReport(
        context: Context,
        event: Event,
        allocations: List<EventItemWithDetails>
    ) {
        try {
            val csvBuilder = java.lang.StringBuilder()
            val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
            val eventTimeStr = sdf.format(Date(event.timestamp))

            csvBuilder.append("Event Name,${if (event.name.contains(",")) "\"${event.name}\"" else event.name}\n")
            csvBuilder.append("Created Date,$eventTimeStr\n\n")

            csvBuilder.append("Item ID,Item Name,Total Stock,Allocated Quantity\n")
            allocations.forEach { alloc ->
                val escapedItemName = if (alloc.itemName.contains(",")) "\"${alloc.itemName}\"" else alloc.itemName
                csvBuilder.append("${alloc.inventoryItemId},$escapedItemName,${alloc.totalQuantity},${alloc.allocatedQuantity}\n")
            }

            // Sanitized file name for the event
            val sanitizedEventName = event.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val fileName = "Event_${sanitizedEventName}_Report.csv"

            val tempDir = context.cacheDir
            val csvFile = File(tempDir, fileName)
            val writer = FileWriter(csvFile)
            writer.write(csvBuilder.toString())
            writer.flush()
            writer.close()

            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, csvFile)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Event Allocations: ${event.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Download/Share Event Inventory CSV"))
        } catch (e: Exception) {
            Toast.makeText(context, "Event CSV Share failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun generateSimpleInventoryHtml(items: List<InventoryItem>): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val dateStr = sdf.format(Date())
        val htmlBuilder = java.lang.StringBuilder()
        htmlBuilder.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                        color: #000000;
                        margin: 40px;
                        font-size: 16px;
                        line-height: 1.4;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                        border-bottom: 2px solid #000;
                        padding-bottom: 15px;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 24px;
                        text-transform: uppercase;
                    }
                    .header p {
                        margin: 5px 0 0;
                        color: #555;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-top: 20px;
                    }
                    th {
                        border-bottom: 2px solid #000;
                        text-align: left;
                        padding: 12px;
                        font-weight: bold;
                        font-size: 16px;
                    }
                    td {
                        padding: 12px;
                        border-bottom: 1px solid #ddd;
                        font-size: 15px;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Warehouse Inventory Report</h1>
                    <p>Date: $dateStr</p>
                </div>
                <table>
                    <thead>
                        <tr>
                            <th style="width: 70%;">Item Name</th>
                            <th style="width: 30%; text-align: right;">Quantity</th>
                        </tr>
                    </thead>
                    <tbody>
        """.trimIndent())

        items.forEach { item ->
            htmlBuilder.append("""
                <tr>
                    <td>${item.name}</td>
                    <td style="text-align: right; font-weight: bold;">${item.quantity}</td>
                </tr>
            """.trimIndent())
        }

        htmlBuilder.append("""
                    </tbody>
                </table>
            </body>
            </html>
        """.trimIndent())
        return htmlBuilder.toString()
    }

    fun printSimpleInventoryPdf(context: Context, items: List<InventoryItem>) {
        try {
            val htmlContent = generateSimpleInventoryHtml(items)
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val printAdapter = webView.createPrintDocumentAdapter("Simple_Inventory_Report")
                    printManager.print("Simple Warehouse Inventory PDF", printAdapter, PrintAttributes.Builder().build())
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
