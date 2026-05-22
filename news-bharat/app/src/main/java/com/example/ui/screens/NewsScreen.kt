package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.db.NewsEntity
import com.example.ui.viewmodel.NewsUiState
import com.example.ui.viewmodel.NewsViewModel
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(
    viewModel: NewsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    // Highly stable Core Material Icons for categories
    val categories = listOf(
        CategoryItem("general", "मुख्य समाचार", Icons.Default.Home),
        CategoryItem("business", "व्यापार", Icons.Default.Star),
        CategoryItem("technology", "तकनीक", Icons.Default.Build),
        CategoryItem("sports", "खेल", Icons.Default.PlayArrow),
        CategoryItem("entertainment", "मनोरंजन", Icons.Default.PlayArrow),
        CategoryItem("science", "विज्ञान", Icons.Default.Info),
        CategoryItem("health", "स्वास्थ्य", Icons.Default.Favorite)
    )

    var currentSelectedBottomTab by remember { mutableStateOf(0) }

    var activeSecretScreen by remember { mutableStateOf("NEWS") }
    var currentUserName by remember { mutableStateOf("") }
    var currentUserUniqueName by remember { mutableStateOf("") }
    var chatTargetName by remember { mutableStateOf("") }
    var chatTargetUniqueName by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    if (activeSecretScreen != "NEWS") {
        when (activeSecretScreen) {
            "SECRET_LOGIN" -> {
                SecretLoginScreen(
                    onLoginSuccess = { name, uniqueName, isAdmin ->
                        currentUserName = name
                        currentUserUniqueName = uniqueName
                        if (isAdmin) {
                            activeSecretScreen = "ADMIN_PANEL"
                        } else {
                            activeSecretScreen = "USER_CHAT_SELECTION"
                        }
                    },
                    onBack = {
                        activeSecretScreen = "NEWS"
                    }
                )
            }
            "ADMIN_PANEL" -> {
                AdminPanelScreen(
                    onBack = {
                        activeSecretScreen = "SECRET_LOGIN"
                    }
                )
            }
            "USER_CHAT_SELECTION" -> {
                UserChatSelectionScreen(
                    currentUserName = currentUserName,
                    currentUserUniqueName = currentUserUniqueName,
                    onUserSelected = { tName, tUnique ->
                        chatTargetName = tName
                        chatTargetUniqueName = tUnique
                        activeSecretScreen = "CHATTING"
                    },
                    onLogout = {
                        currentUserName = ""
                        currentUserUniqueName = ""
                        activeSecretScreen = "SECRET_LOGIN"
                    }
                )
            }
            "CHATTING" -> {
                ChattingScreen(
                    currentUniqueName = currentUserUniqueName,
                    targetName = chatTargetName,
                    targetUniqueName = chatTargetUniqueName,
                    onBack = {
                        activeSecretScreen = "USER_CHAT_SELECTION"
                    }
                )
            }
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                ) {
                    // Professional Design Header Layout matching Top App Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Custom Round Badge Logo representing "N" for Newspaper
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFD3E4FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "N",
                                    color = Color(0xFF001D36),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            
                            Column {
                                Text(
                                    text = "NEWS BHARAT",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "हौसला सच का  •  24x7 News Channel",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Top Refresh Button styled cleanly with transparent and dark-grey action colors
                        IconButton(
                            onClick = {}, // Intercepted by pointerInput for dual hold-or-click actions
                            modifier = Modifier
                                .testTag("refresh_button")
                                .size(48.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            var completed = false
                                            val job = coroutineScope.launch {
                                                delay(3000)
                                                completed = true
                                                activeSecretScreen = "SECRET_LOGIN"
                                            }
                                            try {
                                                awaitRelease()
                                                if (!completed) {
                                                    job.cancel()
                                                    viewModel.refreshCurrentCategory()
                                                    Toast.makeText(context, "अपडेट किया जा रहा है...", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                job.cancel()
                                            }
                                        }
                                    )
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh News",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                
                // Fine separator line as bg-slate-100 border-b
                HorizontalDivider(color = Color(0xFFE2E2E6).copy(alpha = 0.5f), thickness = 1.dp)

                if (isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF005FAF),
                        trackColor = Color(0xFFD3E4FF)
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFF3F4F9),
                modifier = Modifier.navigationBarsPadding(),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentSelectedBottomTab == 0,
                    onClick = { currentSelectedBottomTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001D36),
                        selectedTextColor = Color(0xFF001D36),
                        indicatorColor = Color(0xFFD3E4FF),
                        unselectedIconColor = Color(0xFF43474E),
                        unselectedTextColor = Color(0xFF43474E)
                    )
                )
                NavigationBarItem(
                    selected = currentSelectedBottomTab == 1,
                    onClick = { currentSelectedBottomTab = 1 },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Discover") },
                    label = { Text("Discover", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001D36),
                        selectedTextColor = Color(0xFF001D36),
                        indicatorColor = Color(0xFFD3E4FF),
                        unselectedIconColor = Color(0xFF43474E),
                        unselectedTextColor = Color(0xFF43474E)
                    )
                )
                NavigationBarItem(
                    selected = currentSelectedBottomTab == 2,
                    onClick = { currentSelectedBottomTab = 2 },
                    icon = { Icon(Icons.Default.FavoriteBorder, contentDescription = "Saved") },
                    label = { Text("Saved", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001D36),
                        selectedTextColor = Color(0xFF001D36),
                        indicatorColor = Color(0xFFD3E4FF),
                        unselectedIconColor = Color(0xFF43474E),
                        unselectedTextColor = Color(0xFF43474E)
                    )
                )
                NavigationBarItem(
                    selected = currentSelectedBottomTab == 3,
                    onClick = { currentSelectedBottomTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001D36),
                        selectedTextColor = Color(0xFF001D36),
                        indicatorColor = Color(0xFFD3E4FF),
                        unselectedIconColor = Color(0xFF43474E),
                        unselectedTextColor = Color(0xFF43474E)
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header Active API Badge Block & Category Search Block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.getEquivalentDp(), vertical = 8.getEquivalentDp()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // API status Active green indicator
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFFE2E2E6))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF22C55E), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "लोकल डेटाबेस: सक्रिय",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF43474E)
                    )
                }
            }

            // Search Bar Input Field
            TextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("search_input"),
                placeholder = { Text("समाचार खोजें (सर्च)...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon", tint = Color(0xFF005FAF)) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.performSearch() }),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = Color(0xFFF3F4F9),
                    unfocusedContainerColor = Color(0xFFF3F4F9)
                )
            )

            // Dynamic Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { item ->
                    val isSelected = selectedCategory == item.id && searchQuery.isBlank()
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(item.id) },
                        label = { Text(item.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        leadingIcon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) Color.White else Color(0xFF005FAF)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF005FAF),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = Color(0xFFF3F4F9),
                            labelColor = Color(0xFF1A1C1E)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.testTag("category_chip_${item.id}")
                    )
                }
            }

            // Content Area displaying customized news feed
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (val state = uiState) {
                    is NewsUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFF005FAF))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "ताज़ा ख़बरें लोड हो रही हैं...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is NewsUiState.Success -> {
                        if (state.articles.isEmpty()) {
                            EmptyState(isSearch = state.isSearchActive) {
                                viewModel.refreshCurrentCategory()
                            }
                        } else {
                            ArticleList(articles = state.articles, context = context)
                        }
                    }

                    is NewsUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alert Error",
                                tint = Color(0xFF005FAF),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "खबरें लोड करने में समस्या आई",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.refreshCurrentCategory() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF))
                            ) {
                                Text("पुनः प्रयास करें (Retry)", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun ArticleList(
    articles: List<NewsEntity>,
    context: Context
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(
            items = articles,
            key = { _, article -> article.url }
        ) { index, article ->
            if (index == 0) {
                // Top Featured Story Card style with 28dp corners and custom gradient backdrop
                FeaturedStoryCard(article = article, context = context)
            } else {
                // Compact Recents List Card design with 16dp corners and horizontal layout
                RecentStoryCard(article = article, context = context)
            }
        }
    }
}

@Composable
fun FeaturedStoryCard(
    article: NewsEntity,
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("featured_article_card_${article.url.hashCode()}")
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "लिंक खोलने में त्रुटि", Toast.LENGTH_SHORT).show()
                }
            },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3F0F4)
        ),
        border = BorderStroke(1.dp, Color(0xFFE2E2E6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                if (article.urlToImage.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(article.urlToImage)
                            .crossfade(true)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery)
                            .build(),
                        contentDescription = article.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF6366F1), Color(0xFFA855F7)) // Indigo-500 to Purple-600
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Image Placeholder",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEEF2F6))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "BREAKING",
                            color = Color(0xFF005FAF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    
                    Text(
                        text = formatPublishDate(article.publishedAt),
                        color = Color(0xFF43474E),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = article.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = article.description,
                    fontSize = 14.sp,
                    color = Color(0xFF43474E),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, article.title)
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "${article.title}\n\nपढ़ें विस्तृत समाचार News Bharat पर:\n${article.url}"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "समाचार साझा करें"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "साझा करने में असफल", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share article",
                            tint = Color(0xFF005FAF)
                        )
                    }

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "लिंक खोलने में त्रुटि", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF)),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("आगे पढ़ें", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Read More Arrow",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecentStoryCard(
    article: NewsEntity,
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("article_card_${article.url.hashCode()}")
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "लिंक खोलने में त्रुटि", Toast.LENGTH_SHORT).show()
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, Color(0xFFE2E2E6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE2E2E6))
            ) {
                if (article.urlToImage.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(article.urlToImage)
                            .crossfade(true)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery)
                            .build(),
                        contentDescription = article.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFE2E2E6)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Placeholder",
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = article.sourceName.uppercase(Locale.ROOT),
                        color = Color(0xFFD97706), // Beautiful orange accent
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    
                    Text(
                        text = formatPublishDate(article.publishedAt),
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = article.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = article.description,
                    fontSize = 11.sp,
                    color = Color(0xFF43474E),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    isSearch: Boolean,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Info",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isSearch) "खोजे गए विषय पर कोई न्यूज़ नहीं मिली" else "कोई ताज़ा न्यूज़ उपलब्ध नहीं है",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "कृपया इंटरनेट कनेक्शन की जांच करें या ऊपर दिए रिफ्रेश बटन पर क्लिक करें।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005FAF))
            ) {
                Text("पुनः लोड करें (Reload)", color = Color.White)
            }
        }
    }
}

// Help resolve dp issues by explicitly converting values smoothly
private fun Int.getEquivalentDp() = this.dp

private fun formatPublishDate(rawDate: String): String {
    if (rawDate.isBlank()) return "Breaking"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(rawDate) ?: return rawDate
        val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        rawDate.substringBefore("T")
    }
}

data class CategoryItem(val id: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
