package com.example.newsheadlines

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging

class AdminDashboardActivity : ComponentActivity() {
    private lateinit var database: FirebaseDatabase
    private lateinit var articlesRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = FirebaseDatabase.getInstance()
        articlesRef = database.getReference("articles")

        setContent {
            AdminDashboardScreen()
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun AdminDashboardScreen() {
        var articles by remember { mutableStateOf<List<NewsArticle>>(emptyList()) }
        var selectedCategory by remember { mutableStateOf("all") }
        var categoryExpanded by remember { mutableStateOf(false) }
        var selectedTab by remember { mutableStateOf(0) }
        var selectedArticles by remember { mutableStateOf(setOf<String>()) }
        var users by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
        var declineReason by remember { mutableStateOf("") }
        var showDeclineDialog by remember { mutableStateOf(false) }
        var articleToDecline by remember { mutableStateOf<NewsArticle?>(null) }
        var articleComments by remember { mutableStateOf<Map<String, List<Comment>>>(emptyMap()) }
        val commentListeners by remember { mutableStateOf(mutableMapOf<String, ValueEventListener>()) }

        LaunchedEffect(Unit) {
            articlesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val newArticles = snapshot.children.mapNotNull { it.getValue(NewsArticle::class.java) }
                        .sortedByDescending { it.timestamp }
                    articles = newArticles

                    // Update comment listeners based on new articles
                    val currentArticleIds = newArticles.map { it.id }.toSet()
                    // Remove listeners for articles no longer present
                    commentListeners.keys.filter { it !in currentArticleIds }.forEach { articleId ->
                        commentListeners[articleId]?.let {
                            database.getReference("comments").child(articleId).removeEventListener(it)
                            commentListeners.remove(articleId)
                        }
                    }
                    // Add listeners for new articles
                    newArticles.forEach { article ->
                        if (article.id !in commentListeners) {
                            val listener = object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val comments = snapshot.children.mapNotNull {
                                        it.getValue(Comment::class.java)?.copy(id = it.key ?: "")
                                    }
                                    articleComments = articleComments + (article.id to comments)
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            }
                            database.getReference("comments").child(article.id).addValueEventListener(listener)
                            commentListeners[article.id] = listener
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            database.getReference("users").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    users = snapshot.children.mapNotNull { it.value as? Map<String, Any> }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }

        if (showDeclineDialog && articleToDecline != null) {
            AlertDialog(
                onDismissRequest = { showDeclineDialog = false },
                title = { Text("Decline Reason") },
                text = {
                    OutlinedTextField(
                        value = declineReason,
                        onValueChange = { declineReason = it },
                        label = { Text("Reason for decline") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            articlesRef.child(articleToDecline!!.id).updateChildren(mapOf("status" to "declined"))
                            database.getReference("users").orderByChild("username").equalTo(articleToDecline!!.author)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        val userId = snapshot.children.firstOrNull()?.key
                                        userId?.let {
                                            database.getReference("notifications/$it").push()
                                                .setValue(mapOf("message" to "Article '${articleToDecline!!.title}' declined: $declineReason"))
                                            FirebaseMessaging.getInstance().subscribeToTopic("admin_notifications")
                                        }
                                    }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
                            showDeclineDialog = false
                            declineReason = ""
                            articleToDecline = null
                        }
                    ) {
                        Text("Submit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeclineDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Admin Dashboard", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = MaterialTheme.colors.onPrimary,
                    elevation = 8.dp,
                    actions = {
                        TextButton(onClick = {
                            FirebaseAuth.getInstance().signOut()
                            startActivity(Intent(this@AdminDashboardActivity, LoginActivity::class.java))
                            finish()
                        }) {
                            Text("Logout", color = MaterialTheme.colors.onPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colors.background)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Articles") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Analytics") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Users") })
                }

                when (selectedTab) {
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                elevation = 4.dp,
                                shape = RoundedCornerShape(12.dp),
                                backgroundColor = MaterialTheme.colors.surface
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Filter by Category",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colors.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    ExposedDropdownMenuBox(
                                        expanded = categoryExpanded,
                                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedCategory.replaceFirstChar { it.uppercase() },
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                                focusedBorderColor = MaterialTheme.colors.primary,
                                                unfocusedBorderColor = Color(0xFFB0BEC5),
                                                backgroundColor = MaterialTheme.colors.surface
                                            )
                                        )
                                        ExposedDropdownMenu(
                                            expanded = categoryExpanded,
                                            onDismissRequest = { categoryExpanded = false }
                                        ) {
                                            listOf("all", "sports", "politics", "games", "anime", "education").forEach { category ->
                                                DropdownMenuItem(
                                                    onClick = {
                                                        selectedCategory = category
                                                        categoryExpanded = false
                                                    }
                                                ) {
                                                    Text(
                                                        category.replaceFirstChar { it.uppercase() },
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colors.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = {
                                        selectedArticles.forEach { id ->
                                            articlesRef.child(id).updateChildren(
                                                mapOf("status" to "approved", "isPublished" to true)
                                            )
                                            FirebaseMessaging.getInstance().subscribeToTopic("admin_notifications")
                                        }
                                        selectedArticles = emptySet()
                                    },
                                    enabled = selectedArticles.isNotEmpty()
                                ) {
                                    Text("Bulk Approve")
                                }
                                Button(
                                    onClick = {
                                        selectedArticles.forEach { id ->
                                            articlesRef.child(id).updateChildren(mapOf("status" to "declined"))
                                        }
                                        selectedArticles = emptySet()
                                    },
                                    enabled = selectedArticles.isNotEmpty()
                                ) {
                                    Text("Bulk Decline")
                                }
                                Button(
                                    onClick = {
                                        selectedArticles.forEach { id ->
                                            articlesRef.child(id).removeValue()
                                        }
                                        selectedArticles = emptySet()
                                    },
                                    enabled = selectedArticles.isNotEmpty()
                                ) {
                                    Text("Bulk Delete")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "Pending Articles",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            val pendingArticles = if (selectedCategory == "all") articles.filter { it.status == "pending" } else articles.filter { it.status == "pending" && it.category == selectedCategory }
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(pendingArticles) { article ->
                                    AdminArticleItem(article, selectedArticles, { selectedArticles = it }, { articleToDecline = it; showDeclineDialog = true })
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                "Published Articles",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colors.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            val publishedArticles = if (selectedCategory == "all") articles.filter { it.published || it.status == "approved" } else articles.filter { (it.published || it.status == "approved") && it.category == selectedCategory }
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                items(publishedArticles) { article ->
                                    AdminArticleItem(article, selectedArticles, { selectedArticles = it }, { articleToDecline = it; showDeclineDialog = true })
                                }
                            }
                        }
                    }
                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text("Analytics", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn {
                                items(articles) { article ->
                                    val commentsForArticle = articleComments[article.id] ?: emptyList()
                                    val averageRating = if (commentsForArticle.isNotEmpty()) {
                                        commentsForArticle.map { it.rating }.average()
                                    } else 0.0
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        elevation = 4.dp
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Title: ${article.title}", fontWeight = FontWeight.Bold)
                                            Text("Number of Comments: ${commentsForArticle.size}")
                                            Text("Comment Ratings: ${commentsForArticle.map { it.rating }.joinToString(", ")}")
                                            Text("Average Comment Rating: %.2f".format(averageRating))
                                            Text("Overall Article Rating: ${article.rating}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Text("User Management", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn {
                                items(users) { user ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        elevation = 4.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Username: ${user["username"]}")
                                                Text("Email: ${user["email"]}")
                                                Text("Admin: ${user["admin"]}")
                                                Text("Journalist: ${user["journalist"]}")
                                            }
                                            Row {
                                                Button(onClick = {
                                                    database.getReference("users").child(user["uid"] as String)
                                                        .updateChildren(mapOf("admin" to !(user["admin"] as Boolean)))
                                                }) {
                                                    Text("Toggle Admin")
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(onClick = {
                                                    database.getReference("users").child(user["uid"] as String).removeValue()
                                                }) {
                                                    Text("Delete")
                                                }
                                            }
                                        }
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
    fun AdminArticleItem(
        article: NewsArticle,
        selectedArticles: Set<String>,
        onSelectionChange: (Set<String>) -> Unit,
        onDecline: (NewsArticle) -> Unit
    ) {
        var title by remember { mutableStateOf(article.title) }
        var description by remember { mutableStateOf(article.description) }
        val isSelected = article.id in selectedArticles

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(16.dp)),
            elevation = 6.dp,
            backgroundColor = MaterialTheme.colors.surface
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = {
                        onSelectionChange(if (isSelected) selectedArticles - article.id else selectedArticles + article.id)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = Color(0xFFB0BEC5),
                            backgroundColor = MaterialTheme.colors.surface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = Color(0xFFB0BEC5),
                            backgroundColor = MaterialTheme.colors.surface
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Author: ${article.author} | Status: ${article.status} | Category: ${article.category}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.padding(horizontal = 4.dp),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        if (article.status == "pending") {
                            Button(
                                onClick = {
                                    articlesRef.child(article.id).updateChildren(
                                        mapOf("status" to "approved", "isPublished" to true)
                                    )
                                    FirebaseMessaging.getInstance().subscribeToTopic("admin_notifications")
                                },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF43A047)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            ) {
                                Text("Approve", color = MaterialTheme.colors.onPrimary, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { onDecline(article) },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFE53935)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            ) {
                                Text("Decline", color = MaterialTheme.colors.onPrimary, fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = {
                                articlesRef.child(article.id).updateChildren(
                                    mapOf("title" to title, "description" to description, "status" to "approved", "isPublished" to true)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        ) {
                            Text("Edit & Approve", color = MaterialTheme.colors.onSecondary, fontSize = 12.sp)
                        }
                        if (article.status != "pending") {
                            Button(
                                onClick = { articlesRef.child(article.id).removeValue() },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFD81B60)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Delete", color = MaterialTheme.colors.onPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}