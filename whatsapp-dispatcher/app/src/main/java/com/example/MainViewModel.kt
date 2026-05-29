package com.example

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val contactDao = AppDatabase.getDatabase(application).contactDao()
    private val repository = ContactRepository(contactDao)

    val allContacts: StateFlow<List<ContactMessage>> = repository.allContactsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pendingCount: StateFlow<Int> = repository.pendingCountFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // Keeps track of the contact that is currently launched towards WhatsApp
    var currentlyDispatchingId: Long? = null

    fun importCsv(uri: Uri, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val contacts = withContext(Dispatchers.IO) {
                try {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { inputStream ->
                        CsvHelper.parseCsv(inputStream)
                    } ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            if (contacts.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    repository.deleteAll()
                    repository.insertAll(contacts)
                }
            }
            onComplete(contacts.size)
        }
    }

    suspend fun getNextPendingContact(): ContactMessage? {
        return withContext(Dispatchers.IO) {
            val pending = repository.getPendingContacts()
            if (pending.isNotEmpty()) pending[0] else null
        }
    }

    fun markCurrentAsSent() {
        val dispatchId = currentlyDispatchingId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStatus(dispatchId, "Sent")
            currentlyDispatchingId = null
        }
    }

    fun resetDispatchState() {
        currentlyDispatchingId = null
    }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAll()
        }
    }
}
