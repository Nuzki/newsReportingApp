package com.example.newsheadlines

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.newsheadlines.components.ArticleItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileActivity : ComponentActivity() {
    private lateinit var database: FirebaseDatabase
    private lateinit var articlesRef: DatabaseReference
    private var articlesListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = FirebaseDatabase.getInstance()
        articlesRef = database.getReference("articles")
        setContent { ProfileScreen() }
    }

    override fun onDestroy() {
        super.onDestroy()
        articlesListener?.let { articlesRef.removeEventListener(it) }
    }

    @Composable
    fun ProfileScreen() {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        var username by remember { mutableStateOf(user?.email?.substringBefore("@") ?: "Anonymous") }
        var email by remember { mutableStateOf(user?.email ?: "") }
        var articles by remember { mutableStateOf<List<NewsArticle>>(emptyList()) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(user) {
            if (user != null) {
                scope.launch(Dispatchers.IO) {
                    articlesListener = object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val updatedArticles = snapshot.children.mapNotNull { it.getValue(NewsArticle::class.java) }
                                .filter { it.published && it.status == "approved" && it.author == username }
                            launch(Dispatchers.Main) {
                                articles = updatedArticles
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    }.also { articlesRef.orderByChild("author").equalTo(username).addValueEventListener(it) }
                }
            }
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp) }, backgroundColor = MaterialTheme.colors.primary) }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = username, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Email: $email", fontSize = 16.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("My Published Articles", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                if (articles.isEmpty()) Text("No articles published yet")
                else LazyColumn {
                    items(articles) { article ->
                        ArticleItem(article)
                    }
                }
            }
        }
    }
}