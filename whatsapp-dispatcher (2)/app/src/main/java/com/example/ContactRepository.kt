package com.example

import kotlinx.coroutines.flow.Flow

class ContactRepository(private val contactDao: ContactDao) {
    val allContactsFlow: Flow<List<ContactMessage>> = contactDao.getAllContactsFlow()
    val pendingCountFlow: Flow<Int> = contactDao.getPendingCountFlow()

    suspend fun getPendingContacts(): List<ContactMessage> {
        return contactDao.getPendingContacts()
    }

    suspend fun insertAll(contacts: List<ContactMessage>) {
        contactDao.insertAll(contacts)
    }

    suspend fun updateStatus(id: Long, status: String) {
        contactDao.updateStatus(id, status)
    }

    suspend fun update(contact: ContactMessage) {
        contactDao.update(contact)
    }

    suspend fun deleteAll() {
        contactDao.deleteAll()
    }
}
