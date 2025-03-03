package com.example.newsheadlines

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.newsheadlines.components.ArticleItem
import com.example.newsheadlines.ui.theme.NewsHeadlinesTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainPage : ComponentActivity() {
    private lateinit var database: FirebaseDatabase
    private lateinit var articlesRef: DatabaseReference
    private var articlesListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance()
        articlesRef = database.getReference("articles")
        setContent {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("dark_theme", false)) }
            NewsHeadlinesTheme(darkTheme = isDarkTheme) {
                Scaffold(
                    topBar = { TopBar(isDarkTheme) { isDarkTheme = it; prefs.edit { putBoolean("dark_theme", it) } } },
                    bottomBar = { BottomNavigationBar(this) },
                    floatingActionButton = { PublishFab() }
                ) { paddingValues ->
                    MainContent(paddingValues)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        articlesListener?.let { articlesRef.removeEventListener(it) }
    }

    @Composable
    fun TopBar(isDarkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
        val purpleColor = Color(0xFF6200EE)
        TopAppBar(
            title = { Text("Latest News", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White) },
            actions = {
                IconButton(onClick = { onThemeChange(!isDarkTheme) }) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = Color.White
                    )
                }
            },
            backgroundColor = purpleColor,
            contentColor = Color.White
        )
    }

    @Composable
    fun BottomNavigationBar(activity: MainPage) {
        val context = LocalContext.current
        val auth = FirebaseAuth.getInstance()
        val purpleColor = Color(0xFF6200EE)

        BottomNavigation(backgroundColor = purpleColor, contentColor = Color.White) {
            BottomNavigationItem(
                icon = { Icon(Icons.Default.Home, "Home", tint = Color.White) },
                label = { Text("Home", color = Color.White) },
                selected = true,
                onClick = { }
            )
            BottomNavigationItem(
                icon = { Icon(Icons.Default.Person, "Profile", tint = Color.White) },
                label = { Text("Profile", color = Color.White) },
                selected = false,
                onClick = { context.startActivity(Intent(context, ProfileActivity::class.java)) }
            )
            BottomNavigationItem(
                icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout", tint = Color.White) },
                label = { Text("Logout", color = Color.White) },
                selected = false,
                onClick = {
                    auth.signOut()
                    context.startActivity(Intent(context, LoginActivity::class.java))
                    activity.finish()
                }
            )
        }
    }

    @Composable
    fun PublishFab() {
        val context = LocalContext.current
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        var isJournalist by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val purpleColor = Color(0xFF6200EE)

        LaunchedEffect(user) {
            if (user != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val result = user.getIdToken(true).result
                        isJournalist = result.claims["role"] == "journalist"
                    } catch (e: Exception) {
                        println("Error fetching token: ${e.message}")
                    }
                    if (!isJournalist) {
                        database.getReference("users").child(user.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val userData = snapshot.getValue(User::class.java)
                                isJournalist = userData?.isJournalist == true
                                println("Database check - isJournalist: $isJournalist")
                            }
                            override fun onCancelled(error: DatabaseError) {
                                println("Database error checking journalist status: ${error.message}")
                            }
                        })
                    }
                }
            }
        }

        if (isJournalist) {
            FloatingActionButton(
                onClick = { context.startActivity(Intent(context, PublishArticleActivity::class.java)) },
                backgroundColor = purpleColor,
                contentColor = Color.White,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, "Publish Article") }
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun MainContent(paddingValues: PaddingValues) {
        val viewModel: MainViewModel = viewModel()
        val articles by viewModel.articleList.collectAsState()
        var searchQuery by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("all") }
        var categoryExpanded by remember { mutableStateOf(false) }
        var sortOption by remember { mutableStateOf("newest") }
        var sortExpanded by remember { mutableStateOf(false) }
        var selectedTab by remember { mutableStateOf(0) }
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        val scope = rememberCoroutineScope()
        val purpleColor = Color(0xFF6200EE)

        LaunchedEffect(Unit) {
            viewModel.getArticles()
            println("MainPage started fetching articles for user: ${user?.email ?: "No user"}")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colors.background)
        ) {
            TabRow(selectedTabIndex = selectedTab, backgroundColor = purpleColor) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("All Articles", color = Color.White) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Favorites", color = Color.White) })
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { newQuery ->
                        searchQuery = newQuery
                        viewModel.setSearchQuery(newQuery)
                    },
                    label = { Text("Search Articles") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, "Search", tint = purpleColor) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = purpleColor)
                )
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = purpleColor)
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        listOf("all", "sports", "politics", "games", "anime", "education").forEach { category ->
                            DropdownMenuItem(
                                content = { Text(category.replaceFirstChar { it.uppercase() }) },
                                onClick = { selectedCategory = category; categoryExpanded = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = sortExpanded,
                    onExpandedChange = { sortExpanded = !sortExpanded }
                ) {
                    OutlinedTextField(
                        value = sortOption.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = purpleColor)
                    )
                    ExposedDropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                        listOf("newest", "oldest", "rating").forEach { option -> // Removed "likes" and "views"
                            DropdownMenuItem(
                                content = { Text(option.replaceFirstChar { it.uppercase() }) },
                                onClick = { sortOption = option; viewModel.setSortOption(option) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                val filteredArticles = when (selectedTab) {
                    0 -> articles.filter {
                        it.title.contains(searchQuery, ignoreCase = true) &&
                                (selectedCategory == "all" || it.category == selectedCategory)
                    }
                    1 -> {
                        val favoritesRef = database.getReference("users/${user?.uid}/favorites")
                        var favorites by remember { mutableStateOf<List<String>>(emptyList()) }
                        LaunchedEffect(Unit) {
                            scope.launch(Dispatchers.IO) {
                                favoritesRef.addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val updatedFavorites = snapshot.children.mapNotNull { it.key }
                                        launch(Dispatchers.Main) {
                                            favorites = updatedFavorites
                                            println("Fetched ${updatedFavorites.size} favorite articles for ${user?.email ?: "anonymous"}: $updatedFavorites")
                                        }
                                    }
                                    override fun onCancelled(error: DatabaseError) {
                                        println("Favorites fetch error: ${error.message}")
                                    }
                                })
                            }
                        }
                        articles.filter { it.id in favorites }
                    }
                    else -> emptyList()
                }.sortedWith(when (sortOption) {
                    "newest" -> compareByDescending { it.timestamp }
                    "oldest" -> compareBy { it.timestamp }
                    "rating" -> compareByDescending { it.rating }
                    else -> compareByDescending { it.timestamp }
                })

                if (filteredArticles.isEmpty()) {
                    Text("No articles available", modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(filteredArticles) { article ->
                            ArticleItem(article)
                            println("Displaying article: ${article.title} (ID: ${article.id}, Status: ${article.status}, Published: ${article.published})")
                        }
                    }
                }
            }
        }
    }
}