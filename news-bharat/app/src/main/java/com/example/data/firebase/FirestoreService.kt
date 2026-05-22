package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

object FirestoreService {
    private const val TAG = "FirestoreService"
    private var firestoreInstance: FirebaseFirestore? = null

    // Model structures
    data class ChatUser(
        val name: String = "",
        val uniqueName: String = ""
    )

    data class ChatMessage(
        val id: String = "",
        val senderId: String = "",
        val receiverId: String = "",
        val text: String = "",
        val timestamp: Long = 0L,
        val replyToId: String = "",
        val replyToText: String = "",
        val replyToUser: String = "",
        val isEdited: Boolean = false
    )

    fun initialize(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:575591561467:android:e4c8baae09e637b5a3185e")
                    .setApiKey("AIzaSyATuMbmdI1fa1gi-YxHyv9OP954EKreP2M")
                    .setProjectId("android-app-msg")
                    .setStorageBucket("android-app-msg.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
                Log.d(TAG, "Firebase initialized successfully programmatically")
            }
            firestoreInstance = FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase/Firestore: ${e.message}", e)
        }
    }

    private fun getDb(): FirebaseFirestore {
        return firestoreInstance ?: throw IllegalStateException("Firestore not initialized. Call initialize() first.")
    }

    // --- User Management (Admin CRUD) ---

    // Fetch all users
    suspend fun getAllUsers(): List<ChatUser> = suspendCancellableCoroutine { continuation ->
        try {
            getDb().collection("users")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val list = mutableListOf<ChatUser>()
                    for (doc in querySnapshot.documents) {
                        val name = doc.getString("name") ?: ""
                        val uniqueName = doc.getString("uniqueName") ?: doc.id
                        if (uniqueName != "admin") { // Hide admin from list of regular users
                            list.add(ChatUser(name, uniqueName))
                        }
                    }
                    if (continuation.isActive) continuation.resume(list)
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }

    // Add or Update user
    suspend fun saveUser(user: ChatUser): Unit = suspendCancellableCoroutine { continuation ->
        try {
            val db = getDb()
            val data = mapOf(
                "name" to user.name,
                "uniqueName" to user.uniqueName
            )
            // Use uniqueName as the document path/ID
            db.collection("users").document(user.uniqueName)
                .set(data)
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }

    // Delete user
    suspend fun deleteUser(uniqueName: String): Unit = suspendCancellableCoroutine { continuation ->
        try {
            getDb().collection("users").document(uniqueName)
                .delete()
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }

    // --- Real-time Chatting ---

    // Send a message
    suspend fun sendMessage(
        senderId: String, 
        receiverId: String, 
        text: String, 
        replyToId: String = "", 
        replyToText: String = "", 
        replyToUser: String = ""
    ): Unit = suspendCancellableCoroutine { continuation ->
        try {
            val db = getDb()
            val msgRef = db.collection("messages").document()
            val messageId = msgRef.id
            val data = mapOf(
                "id" to messageId,
                "senderId" to senderId,
                "receiverId" to receiverId,
                "text" to text,
                "timestamp" to System.currentTimeMillis(),
                "replyToId" to replyToId,
                "replyToText" to replyToText,
                "replyToUser" to replyToUser,
                "isEdited" to false
            )
            msgRef.set(data)
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }

    // Edit a message
    suspend fun editMessage(messageId: String, newText: String): Unit = suspendCancellableCoroutine { continuation ->
        try {
            val db = getDb()
            db.collection("messages").document(messageId)
                .update(
                    mapOf(
                        "text" to newText,
                        "isEdited" to true
                    )
                )
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }

    // Delete message for everyone
    suspend fun deleteMessage(messageId: String): Unit = suspendCancellableCoroutine { continuation ->
        try {
            val db = getDb()
            db.collection("messages").document(messageId)
                .delete()
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }

    // Observe messages between two users in real-time
    fun observeMessages(userA: String, userB: String): Flow<List<ChatMessage>> = callbackFlow {
        val db = getDb()
        
        // Listen to all messages in the common channel.
        // Since we want both directions (userA -> userB AND userB -> userA), we can fetch all and filter in memory,
        // or query the ones involving either A or B.
        // An easy, robust, and indexed-safe way without complex composite queries:
        // Query of all messages, filtering locally, or we can filter senderId/receiverId if needed.
        // Let's retrieve the last 150 messages and filter for userA and userB to keep it highly snappy without needing security indexes.
        val listenerRegistration = db.collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    close(exception)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = mutableListOf<ChatMessage>()
                    for (doc in snapshot.documents) {
                        val sender = doc.getString("senderId") ?: ""
                        val receiver = doc.getString("receiverId") ?: ""
                        if ((sender == userA && receiver == userB) || (sender == userB && receiver == userA)) {
                            list.add(
                                ChatMessage(
                                    id = doc.getString("id") ?: doc.id,
                                    senderId = sender,
                                    receiverId = receiver,
                                    text = doc.getString("text") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: 0L,
                                    replyToId = doc.getString("replyToId") ?: "",
                                    replyToText = doc.getString("replyToText") ?: "",
                                    replyToUser = doc.getString("replyToUser") ?: "",
                                    isEdited = doc.getBoolean("isEdited") ?: false
                                )
                            )
                        }
                    }
                    trySend(list)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }
}