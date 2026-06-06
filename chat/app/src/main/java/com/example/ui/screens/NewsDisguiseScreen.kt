package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

data class NewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val category: String,
    val date: String,
    val imageUrl: String
)

val mockNewsArticles = listOf(
    NewsArticle(
        id = "1",
        title = "India Sets New Milestone in Semi-Conductor Chip Production",
        summary = "A major multi-billion venture in Gujarat receives cabinet approval, aiming to put the country on the global silicon supply chain map.",
        content = "NEW DELHI — In a landmark decision, the Union Cabinet has approved a series of ultra-modern semiconductor fab initiatives, estimated to attract cumulative investments exceeding $12 billion. Industry specialists view this as a pivotal step in establishing self-reliance in high-technology electronics manufacturing.\n\nAccording to official statements, construction of the signature fabrication plant is set to commence in the coming quarter. High-level partnerships with major global technology conglomerates are being secured to enable rapid knowledge-transfer and technology transfer today.\n\nThe infrastructure will also include dedicated research centers focusing on sub-nanometer chip designs and clean-room technologies. Over 20,000 highly skilled jobs are projected to be generated, boosting tech development dramatically across the region.",
        category = "Tech & Business",
        date = "2 hours ago",
        imageUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=500&auto=format&fit=crop&q=60"
    ),
    NewsArticle(
        id = "2",
        title = "Global Tech Summit 2026 Spotlights Advancements in Edge AI",
        summary = "Leading tech companies and researchers gather in Bengaluru to discuss next-generation Edge AI and decentralised system security.",
        content = "BENGALURU — The annual Global Tech Summit kicked off today in Bengaluru, with edge computing, privacy-centric AI ecosystems, and energy-efficient data storage architectures taking center stage.\n\nKeynote speakers from foundational technology institutions highlighted the critical transition from remote cloud clusters to local-edge computing architectures. This enables processing to happen right on the user's terminal, lowering latencies and drastically enhancing user privacy.\n\nInteroperable hardware protocols and standardized quantum-safe encryption wrappers were also unveiled during the afternoon panel discussions. More than 450 tech startups showcased their latest prototypes, attracting eager venture funding.",
        category = "Technology",
        date = "5 hours ago",
        imageUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500&auto=format&fit=crop&q=60"
    ),
    NewsArticle(
        id = "3",
        title = "Central Bank Retains Target Rates to Build Steady Fiscal Growth",
        summary = "The central regulatory authority emphasizes cautious optimism, holding interest rates steady to maintain balanced domestic inflation rates.",
        content = "MUMBAI — Delivering their second quarterly policy address, the central banking committee unanimously voted to maintain existing benchmark interest rates, citing stable consumer expenditure indices and robust industrial production.\n\nThe governor pointed out that while energy rates and imported goods saw mild fluctuations, domestic core inflation remains firmly within the targeted range of 4.1%. Maintaining a neutral stance ensures that businesses can secure credit predictably to sustain developmental expansion.\n\nMarket analysts reacted positively to the cautious but steady monetary guidelines, with main commercial indices showing subtle upswings. The board will inspect indicators again in Q3.",
        category = "Finance",
        date = "Yesterday",
        imageUrl = "https://images.unsplash.com/photo-1559526324-4b87b5e36e44?w=500&auto=format&fit=crop&q=60"
    ),
    NewsArticle(
        id = "4",
        title = "National Athletics Squad Triumphs at world Championship Finals",
        summary = "Athletes secure historical gold and silver medals in relays and long jump events, establishing national athletic supremacy.",
        content = "TOKYO — The national athletics contingent has made history at the championship finals, winning two gold medals and one silver during a spectacular series of athletic events tonight.\n\nThe women's 4x400m relay team stole the show, executing precision baton-passes to finish ahead of traditional track powerhouses in a thrilling finish. Our lead runner registered a career-best individual split time, sparking jubilant celebrations in the arena.\n\nIn the field events, a spectacular 8.45-meter leap in the long jump final secured a comfortable silver. Total cumulative medals have exceeded expectations, setting up a stellar precedent for upcoming global tournaments.",
        category = "Sports",
        date = "1 day ago",
        imageUrl = "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=500&auto=format&fit=crop&q=60"
    ),
    NewsArticle(
        id = "5",
        title = "Space Expedition Spacecraft Safely Enters Lower Orbit",
        summary = "Ground operations team confirms successful orbital insertion maneuver, starting next phase of deep space imaging and mapping.",
        content = "MISSION CONTROL — Space scientists cheered today as telemetry confirmed that the signature deep-space research orbiter has safely adjusted its path to settle into lower planetary orbit.\n\nFollowing a tense 25-minute automated thruster-burn phase where communication was temporarily obscured, the spacecraft sent back diagnostic packages indicating all onboard systems are in peak execution status.\n\nThe satellite carries specialized high-spectrum cameras and soil analysis sensors designed to study chemical compositions from orbit. The first batch of high-resolution images is expected to arrive within seventy-two hours.",
        category = "National",
        date = "2 days ago",
        imageUrl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=500&auto=format&fit=crop&q=60"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDisguiseScreen(
    onSecretTrigger: () -> Unit,
    onArticleSelected: (NewsArticle) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Tech & Business", "Technology", "Finance", "Sports", "National")
    val context = LocalContext.current

    val filteredArticles = remember(searchQuery, selectedCategory) {
        mockNewsArticles.filter { article ->
            val matchesCategory = (selectedCategory == "All" || article.category == selectedCategory)
            val matchesSearch = (article.title.contains(searchQuery, ignoreCase = true) ||
                    article.summary.contains(searchQuery, ignoreCase = true))
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        Toast.makeText(context, "Welcome back, Operator.", Toast.LENGTH_SHORT).show()
                                        onSecretTrigger()
                                    }
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFB91C1C), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "News Bharat",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color(0xFFB91C1C)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "• English",
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "News feed updated", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Feed", tint = Color(0xFFB91C1C))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1E293B)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC)) // Light aesthetic background
                .padding(paddingValues)
        ) {
            // Search Bar Disguise + Secret Word interceptor
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { newValue ->
                    searchQuery = newValue
                    if (newValue.trim() == "7777" || newValue.trim() == "9999") {
                        Toast.makeText(context, "Initializing secure systems...", Toast.LENGTH_SHORT).show()
                        onSecretTrigger()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search latest news, trending topics...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFB91C1C),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            // Category Filter Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFB91C1C),
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color(0xFF475569)
                        )
                    )
                }
            }

            // Article List View
            if (filteredArticles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No articles matching search query.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
                ) {
                    items(filteredArticles) { article ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onArticleSelected(article) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column {
                                AsyncImage(
                                    model = article.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = article.category.uppercase(),
                                            color = Color(0xFFB91C1C),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = article.date,
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = article.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFF1E293B),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = article.summary,
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
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
fun NewsDetailScreen(
    article: NewsArticle,
    onBack: () -> Unit,
    onSecretTrigger: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Article View", fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "Headline saved to bookmarks", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite")
                    }
                    IconButton(onClick = {
                        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            item {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    Toast.makeText(context, "Decoding payload...", Toast.LENGTH_SHORT).show()
                                    onSecretTrigger()
                                }
                            )
                        },
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Category & Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = article.category.uppercase(),
                            color = Color(0xFFB91C1C),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${article.date} • Verified Source",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Title
                    Text(
                        text = article.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 26.sp,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = Color(0xFFE2E2E6))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Body
                    Text(
                        text = article.content,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Advertisements placeholder / related wrapper
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Trending Next",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tech investments expected to push nationwide connectivity indices to historic margins. Tap back to check the latest news feed.",
                                fontSize = 13.sp,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            }
        }
    }
}
