package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirestoreService
import com.example.data.firebase.FirestoreService.ChatMessage
import com.example.data.firebase.FirestoreService.ChatUser
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import android.content.Context
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretLoginScreen(
    onLoginSuccess: (name: String, uniqueName: String, isAdmin: Boolean) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var uniqueName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("गुप्त चैट लॉगिन", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0F172A)) // Deep space dark theme
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock Logo",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "सुरक्षित चैट सर्वर",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "चैट करने के लिए अपनी साख (credentials) दर्ज करें",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("नाम (Name)", color = Color(0xFF94A3B8)) },
                placeholder = { Text("उदा. Ramesh", color = Color(0xFF475569)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF334155),
                    cursorColor = Color(0xFF3B82F6)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Unique Name Field (serves as ID/password depending on context)
            OutlinedTextField(
                value = uniqueName,
                onValueChange = { uniqueName = it },
                label = { Text("यूनिक नाम (Unique Name)", color = Color(0xFF94A3B8)) },
                placeholder = { Text("उदा. ramesh123", color = Color(0xFF475569)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(), // Keep it secret!
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF3B82F6),
                    unfocusedBorderColor = Color(0xFF334155),
                    cursorColor = Color(0xFF3B82F6)
                )
            )

            Spacer(modifier = Modifier.height(30.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF3B82F6))
            } else {
                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        val trimmedUnique = uniqueName.trim()

                        if (trimmedName.isEmpty() || trimmedUnique.isEmpty()) {
                            Toast.makeText(context, "कृपया दोनों फ़ील्ड भरें!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (trimmedName == "admin" && trimmedUnique == "admin123") {
                            onLoginSuccess(trimmedName, trimmedUnique, true)
                        } else {
                            isLoading = true
                            scope.launch {
                                try {
                                    val users = FirestoreService.getAllUsers()
                                    val matchedUser = users.find {
                                        it.uniqueName.lowercase() == trimmedUnique.lowercase() &&
                                                it.name.lowercase() == trimmedName.lowercase()
                                    }
                                    if (matchedUser != null) {
                                        onLoginSuccess(matchedUser.name, matchedUser.uniqueName, false)
                                    } else {
                                        Toast.makeText(context, "पंजीकृत यूजर नहीं मिला! एडमीन से संपर्क करें।", Toast.LENGTH_LONG).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "त्रुटि: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("प्रवेश करें / Login", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    onBack: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var uniqueNameInput by remember { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(false) }
    var editingUserUniqueName by remember { mutableStateOf("") }
    
    var usersList by remember { mutableStateOf<List<ChatUser>>(emptyList()) }
    var isLoadingList by remember { mutableStateOf(false) }
    var isSavingUser by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Helper to fetch list
    fun fetchUsers() {
        isLoadingList = true
        scope.launch {
            try {
                usersList = FirestoreService.getAllUsers()
            } catch (e: Exception) {
                Toast.makeText(context, "यूजर लोड करने में विफल: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoadingList = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("एडमीन पैनल (User Manager)", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0F172A))
                .padding(16.dp)
        ) {
            // Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isEditMode) "यूजर विवरण बदलें" else "नया यूजर जोड़ें",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("यूजर का नाम (Name)", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("उदा. Inderjeet Rao", color = Color(0xFF475569)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = uniqueNameInput,
                        onValueChange = { uniqueNameInput = it },
                        label = { Text("यूनिक नाम (ID)", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("उदा. inderjeet544", color = Color(0xFF475569)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isEditMode, // Unique name/ID doc reference can't be changed during edit mode
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isEditMode) {
                            OutlinedButton(
                                onClick = {
                                    isEditMode = false
                                    nameInput = ""
                                    uniqueNameInput = ""
                                    editingUserUniqueName = ""
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("कैंसिल")
                            }
                        }

                        Button(
                            onClick = {
                                val name = nameInput.trim()
                                val unique = uniqueNameInput.trim()

                                if (name.isEmpty() || unique.isEmpty()) {
                                    Toast.makeText(context, "कृपया सभी विवरण भरें!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                if (unique.lowercase() == "admin") {
                                    Toast.makeText(context, "यह नाम आरक्षित है!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isSavingUser = true
                                scope.launch {
                                    try {
                                        FirestoreService.saveUser(ChatUser(name, unique))
                                        Toast.makeText(context, if (isEditMode) "यूजर संशोधित किया गया!" else "यूजर सुरक्षित किया गया!", Toast.LENGTH_SHORT).show()
                                        
                                        // Reset
                                        nameInput = ""
                                        uniqueNameInput = ""
                                        isEditMode = false
                                        editingUserUniqueName = ""
                                        
                                        // Refresh List
                                        fetchUsers()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "सुरक्षित करने में त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSavingUser = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                            enabled = !isSavingUser
                        ) {
                            if (isSavingUser) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text(if (isEditMode) "संशोधित करें (Edit)" else "सुरक्षित करें (Save)", color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "सुरक्षित पंजीकृत यूजर्स की सूची:",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoadingList) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6))
                }
            } else if (usersList.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("कोई यूजर नहीं मिला! कृपया जोड़ें।", color = Color(0xFF94A3B8), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(usersList) { u ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = u.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(text = "@${u.uniqueName}", color = Color(0xFF3B82F6), fontWeight = FontWeight.Normal, fontSize = 12.sp)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        nameInput = u.name
                                        uniqueNameInput = u.uniqueName
                                        isEditMode = true
                                        editingUserUniqueName = u.uniqueName
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit User", tint = Color(0xFFF59E0B))
                                }

                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                FirestoreService.deleteUser(u.uniqueName)
                                                Toast.makeText(context, "यूजर हटाया गया!", Toast.LENGTH_SHORT).show()
                                                fetchUsers()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "हटाने में त्रुटि: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete User", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserChatSelectionScreen(
    currentUserName: String,
    currentUserUniqueName: String,
    onUserSelected: (targetName: String, targetUniqueName: String) -> Unit,
    onLogout: () -> Unit
) {
    var usersList by remember { mutableStateOf<List<ChatUser>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        isLoading = true
        scope.launch {
            try {
                // Fetch other registered users
                usersList = FirestoreService.getAllUsers().filter { it.uniqueName != currentUserUniqueName }
            } catch (e: Exception) {
                Toast.makeText(context, "यूजर लोड करने में विफल: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("सुरक्षित चैट्स", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("लॉगिन: $currentUserName", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Exit to app", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0F172A))
                .padding(16.dp)
        ) {
            Text(
                text = "चैट करने के लिए किसी एक ऑनलाइन यूजर को चुनें:",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF3B82F6))
                }
            } else if (usersList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "कोई अन्य पंजीकृत यूजर नहीं मिला!\nएडमिन से कहें कि दूसरा यूजर बनाएँ।",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(usersList) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .clickable { onUserSelected(user.name, user.uniqueName) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .background(Color(0xFF10B981).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "User avatar", tint = Color(0xFF10B981))
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(text = user.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "@${user.uniqueName}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LinkifiedText(text: String, textColor: Color) {
    val context = LocalContext.current
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val urlPattern = remember {
        java.util.regex.Pattern.compile(
            "https?://[\\w-]+(\\.[\\w-]+)+([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?"
        )
    }
    val matcher = urlPattern.matcher(text)
    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            
            if (start > lastIndex) {
                append(text.substring(lastIndex, start))
            }
            
            val url = text.substring(start, end)
            pushStringAnnotation(tag = "URL", annotation = url)
            withStyle(style = SpanStyle(color = Color(0xFF60A5FA), textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold)) {
                append(url)
            }
            pop()
            
            lastIndex = end
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
    
    val annotations = annotatedString.getStringAnnotations(tag = "URL", start = 0, end = text.length)
    if (annotations.isEmpty()) {
        Text(text = text, color = textColor, fontSize = 14.sp)
    } else {
        ClickableText(
            text = annotatedString,
            style = TextStyle(color = textColor, fontSize = 14.sp),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        try {
                            uriHandler.openUri(annotation.item)
                        } catch (e: Exception) {
                            Toast.makeText(context, "लिंक खोलने में असमर्थ", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatItemRow(
    msg: ChatMessage,
    currentUniqueName: String,
    onReply: (ChatMessage) -> Unit,
    onLongClick: (ChatMessage) -> Unit
) {
    val isMe = msg.senderId == currentUniqueName
    val alignment = if (isMe) Alignment.End else Alignment.Start
    val bubbleBg = if (isMe) Color(0xFF3B82F6) else Color(0xFF1E293B)
    val textColor = Color.White
    val timeString = try {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(msg.timestamp))
    } catch (e: Exception) {
        ""
    }

    var offsetX by remember { mutableStateOf(0f) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < -100f || offsetX > 100f) {
                                onReply(msg)
                            }
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-150f, 150f)
                        }
                    )
                }
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isMe) 12.dp else 0.dp,
                        bottomEnd = if (isMe) 0.dp else 12.dp
                    )
                )
                .background(bubbleBg)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { onLongClick(msg) }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 270.dp)
        ) {
            Column {
                if (msg.replyToId.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.25f))
                            .padding(6.dp)
                    ) {
                        Column {
                            Text(
                                text = "जवाब: @${msg.replyToUser}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color(0xFF60A5FA)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = msg.replyToText,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1
                            )
                        }
                    }
                }

                LinkifiedText(text = msg.text, textColor = textColor)

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (msg.isEdited) {
                        Text(
                            text = "संशोधित (edited) • ",
                            color = Color(0xFF94A3B8),
                            fontSize = 8.sp
                        )
                    }
                    Text(
                        text = timeString,
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChattingScreen(
    currentUniqueName: String,
    targetName: String,
    targetUniqueName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("secret_chat_prefs", Context.MODE_PRIVATE) }
    
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var typedMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Notification Mute states
    var isMuted by remember { mutableStateOf(prefs.getBoolean("mute_$targetUniqueName", false)) }

    // Actions state
    var replyToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var showOptionsDialog by remember { mutableStateOf<ChatMessage?>(null) }

    // Real-time subscribe
    LaunchedEffect(targetUniqueName) {
        FirestoreService.observeMessages(currentUniqueName, targetUniqueName)
            .collectLatest { msgList ->
                messages = msgList
                // Scroll to latest message
                if (msgList.isNotEmpty()) {
                    listState.animateScrollToItem(msgList.size - 1)
                }
            }
    }

    // Dynamic auto scroll on soft-keyboard toggle
    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(targetName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Text("@$targetUniqueName", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isMuted = !isMuted
                        prefs.edit().putBoolean("mute_$targetUniqueName", isMuted).apply()
                        Toast.makeText(context, if (isMuted) "सूचनाएँ बंद हुईं (Notifications muted)" else "सूचनाएँ चालू हुईं (Notifications unmuted)", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Mute notifications",
                            tint = if (isMuted) Color(0xFFEF4444) else Color(0xFF22C55E)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .background(Color(0xFF0F172A))
        ) {
            // Chat history list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(messages) { msg ->
                    ChatItemRow(
                        msg = msg,
                        currentUniqueName = currentUniqueName,
                        onReply = { replyToMessage = it },
                        onLongClick = { showOptionsDialog = it }
                    )
                }
            }

            // Reply Info Banner
            if (replyToMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "जवाब दिया जा रहा है: @${replyToMessage?.senderId}",
                            color = Color(0xFF60A5FA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = replyToMessage?.text ?: "",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = { replyToMessage = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                }
            }

            // Edit Info Banner
            if (editingMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "संदेश सुधारा जा रहा है",
                            color = Color(0xFFF59E0B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = editingMessage?.text ?: "",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = { 
                        editingMessage = null 
                        typedMessage = ""
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                    }
                }
            }

            // Message typing send bar
            Surface(
                color = Color(0xFF1E293B),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = typedMessage,
                        onValueChange = { typedMessage = it },
                        placeholder = { Text("सुरक्षित संदेश लिखें...", color = Color(0xFF64748B)) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val text = typedMessage.trim()
                            if (text.isNotEmpty()) {
                                scope.launch {
                                    try {
                                        if (editingMessage != null) {
                                            FirestoreService.editMessage(editingMessage!!.id, text)
                                            editingMessage = null
                                        } else if (replyToMessage != null) {
                                            FirestoreService.sendMessage(
                                                senderId = currentUniqueName,
                                                receiverId = targetUniqueName,
                                                text = text,
                                                replyToId = replyToMessage!!.id,
                                                replyToText = replyToMessage!!.text,
                                                replyToUser = replyToMessage!!.senderId
                                            )
                                            replyToMessage = null
                                        } else {
                                            FirestoreService.sendMessage(
                                                senderId = currentUniqueName,
                                                receiverId = targetUniqueName,
                                                text = text
                                            )
                                        }
                                        typedMessage = ""
                                    } catch (e: Exception) {
                                        // Error handled
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF3B82F6), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    // Modal AlertDialog for option selection on Long Press
    if (showOptionsDialog != null) {
        val selectedMsg = showOptionsDialog!!
        val isMe = selectedMsg.senderId == currentUniqueName

        AlertDialog(
            onDismissRequest = { showOptionsDialog = null },
            title = { Text("विकल्प चुनें (Action Menu)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp) },
            text = { Text("कृपया अपनी रुचि के अनुसार कार्रवाई का चयन करें।", color = Color(0xFF94A3B8), fontSize = 13.sp) },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color.White,
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Reply Option (For all messages)
                    Button(
                        onClick = {
                            replyToMessage = selectedMsg
                            editingMessage = null
                            showOptionsDialog = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("जवाब दें (Reply)", color = Color.White)
                        }
                    }

                    // 2. Edit Option (For sent messages only)
                    if (isMe) {
                        Button(
                            onClick = {
                                editingMessage = selectedMsg
                                typedMessage = selectedMsg.text
                                replyToMessage = null
                                showOptionsDialog = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("संदेश सुधारें (Edit)", color = Color.White)
                            }
                        }

                        // 3. Delete Option (For sent messages only)
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        FirestoreService.deleteMessage(selectedMsg.id)
                                        Toast.makeText(context, "मैसेज डिलीट हो गया!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "पूर्ण नहीं हुआ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                showOptionsDialog = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("सभी के लिए डिलीट करें (Delete For Everyone)", color = Color.White)
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showOptionsDialog = null }) {
                    Text("रद्द करें (Cancel)", color = Color(0xFF64748B))
                }
            }
        )
    }
}