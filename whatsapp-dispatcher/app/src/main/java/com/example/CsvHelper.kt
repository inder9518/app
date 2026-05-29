package com.example

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object CsvHelper {
    fun parseCsv(inputStream: InputStream): List<ContactMessage> {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val lines = mutableListOf<List<String>>()
        var line: String? = reader.readLine()

        while (line != null) {
            if (line.trim().isNotEmpty()) {
                lines.add(parseCsvLine(line))
            }
            line = reader.readLine()
        }

        if (lines.isEmpty()) return emptyList()

        val headers = lines[0].map { it.trim().lowercase() }
        val dataRows = lines.drop(1)

        // Find index of columns based on common names
        val sNoIndex = headers.indexOfFirst { it.contains("s.no") || it.contains("sno") || it.contains("id") || it.contains("serial") }
        val phoneIndex = headers.indexOfFirst { it.contains("phone") || it.contains("num") || it.contains("mobile") || it.contains("contact") }
        val nameIndex = headers.indexOfFirst { it.contains("name") || it.contains("customer") || it.contains("client") }
        val messageIndex = headers.indexOfFirst { it.contains("message") || it.contains("msg") || it.contains("text") || it.contains("content") }

        // Default indices if headers are not found or simple backup
        val finalSNoIndex = if (sNoIndex != -1) sNoIndex else 0
        val finalPhoneIndex = if (phoneIndex != -1) phoneIndex else (if (lines[0].size > 1) 1 else 0)
        val finalNameIndex = if (nameIndex != -1) nameIndex else (if (lines[0].size > 2) 2 else 0)
        val finalMessageIndex = if (messageIndex != -1) messageIndex else (if (lines[0].size > 3) 3 else 0)

        val contacts = mutableListOf<ContactMessage>()

        for (row in dataRows) {
            if (row.isEmpty() || row.size <= 1) continue // Skip invalid empty rows

            val sNo = if (row.size > finalSNoIndex) row[finalSNoIndex].trim() else ""
            val phone = if (row.size > finalPhoneIndex) row[finalPhoneIndex].trim() else ""
            val name = if (row.size > finalNameIndex) row[finalNameIndex].trim() else ""
            val message = if (row.size > finalMessageIndex) row[finalMessageIndex].trim() else ""

            // Clean up phone number: keep only digits and +
            val cleanPhone = phone.replace(Regex("[^0-9+]"), "")

            if (cleanPhone.isNotEmpty()) {
                contacts.add(
                    ContactMessage(
                        sNo = sNo,
                        phoneNumber = cleanPhone,
                        customerName = name,
                        customMessage = message,
                        status = "Pending"
                    )
                )
            }
        }
        return contacts
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '\"') {
                    // If next value is also a quote, treat as escaped quote
                    if (i + 1 < line.length && line[i + 1] == '\"') {
                        curVal.append('\"')
                        i++ // Skip next quote
                    } else {
                        inQuotes = false
                    }
                } else {
                    curVal.append(ch)
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true
                } else if (ch == ',') {
                    result.add(curVal.toString())
                    curVal = StringBuilder()
                } else {
                    curVal.append(ch)
                }
            }
            i++
        }
        result.add(curVal.toString())
        return result
    }
}
