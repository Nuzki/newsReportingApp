package com.example.newsheadlines

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DisplayNewsActivity : ComponentActivity() {
    private lateinit var database: FirebaseDatabase
    private lateinit var commentsRef: DatabaseReference
    private lateinit var ratingsRef: DatabaseReference
    private lateinit var articlesRef: DatabaseReference
    private lateinit var articleId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        articleId = intent.getStringExtra("articleId") ?: ""
        if (articleId.isEmpty()) {
            Toast.makeText(this, "Invalid article ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance()
        articlesRef = database.getReference("articles").child(articleId)
        commentsRef = database.getReference("comments").child(articleId)
        ratingsRef = database.getReference("ratings").child(articleId)

        setContent {
            var title by remember { mutableStateOf("Loading...") }
            var description by remember { mutableStateOf("Loading...") }
            var urlToImage by remember { mutableStateOf<String?>(null) }
            var author by remember { mutableStateOf("Unknown Author") }
            var isJournalist by remember { mutableStateOf(false) }

            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid ?: return@setContent // All users are authenticated

            // Fetch user role
            LaunchedEffect(userId) {
                database.getReference("users").child(userId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val user = snapshot.getValue(User::class.java)
                            isJournalist = user?.isJournalist == true
                        }
                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(this@DisplayNewsActivity, "Failed to load user role: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }

            // Fetch article data
            LaunchedEffect(Unit) {
                articlesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val article = snapshot.getValue(NewsArticle::class.java)
                        if (article != null) {
                            title = article.title
                            description = article.description
                            urlToImage = article.urlToImage
                            author = article.author
                        } else {
                            Toast.makeText(this@DisplayNewsActivity, "Article not found", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@DisplayNewsActivity, "Failed to load article: ${error.message}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                })
            }

            NewsDetailScreen(title, description, urlToImage, author, isJournalist)
        }
    }

    @Composable
    fun NewsDetailScreen(title: String, description: String, urlToImage: String?, author: String, isJournalist: Boolean) {
        val context = LocalContext.current
        var rating by remember { mutableStateOf(0f) }
        var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
        var newComment by remember { mutableStateOf("") }

        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return // All users are authenticated

        LaunchedEffect(Unit) {
            commentsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    comments = snapshot.children.mapNotNull {
                        val comment = it.getValue(Comment::class.java)
                        comment?.copy(id = it.key ?: "")
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load comments: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })

            if (!isJournalist) {
                ratingsRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userRating = snapshot.getValue(Float::class.java)
                        if (userRating != null) {
                            rating = userRating
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Failed to load rating: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }

        Scaffold(topBar = {
            TopAppBar(title = { Text("News Details") })
        }) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Apply Scaffold padding here
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(title, style = MaterialTheme.typography.h6)
                        if (!urlToImage.isNullOrEmpty()) {
                            val imageBitmap by remember(urlToImage) {
                                mutableStateOf(
                                    try {
                                        val decodedBytes = android.util.Base64.decode(urlToImage, android.util.Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                                    } catch (e: Exception) {
                                        null
                                    }
                                )
                            }
                            if (imageBitmap != null) {
                                Image(
                                    bitmap = imageBitmap!!,
                                    contentDescription = "News Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .padding(top = 8.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    "Image unavailable",
                                    style = MaterialTheme.typography.body2,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                        Text(
                            description,
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text("By $author", style = MaterialTheme.typography.body2, color = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Rating slider (hidden for journalists)
                if (!isJournalist) {
                    Text("Rate this Article", style = MaterialTheme.typography.subtitle1)
                    Slider(
                        value = rating,
                        onValueChange = { newRating ->
                            rating = newRating
                            ratingsRef.child(userId).setValue(newRating)
                            Toast.makeText(context, "You rated ${newRating.toInt()} stars", Toast.LENGTH_SHORT).show()
                        },
                        valueRange = 0f..5f,
                        steps = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${rating.toInt()} Stars", style = MaterialTheme.typography.body1)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Comments section (visible to all)
                Text("Comments", style = MaterialTheme.typography.subtitle1)
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(comments) { comment ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = MaterialTheme.shapes.medium,
                            elevation = 2.dp
                        ) {
                            Text(
                                text = "${comment.username}: ${comment.text} (${comment.rating} stars)",
                                modifier = Modifier.padding(8.dp),
                                style = MaterialTheme.typography.body1
                            )
                        }
                    }
                }

                // Comment input (hidden for journalists)
                if (!isJournalist) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newComment,
                        onValueChange = { newComment = it },
                        label = { Text("Add a comment") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            postComment(userId, newComment, rating)
                            newComment = ""
                        })
                    )
                    Button(
                        onClick = {
                            postComment(userId, newComment, rating)
                            newComment = ""
                        },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.End)
                    ) {
                        Text("Post Comment")
                    }
                }
            }
        }
    }

    private fun postComment(userId: String, text: String, userRating: Float) {
        if (text.isNotEmpty()) {
            val userName = FirebaseAuth.getInstance().currentUser?.displayName
                ?: FirebaseAuth.getInstance().currentUser?.email?.substringBefore("@")
                ?: "Anonymous"
            val comment = Comment(
                userId = userId,
                username = userName,
                text = text,
                rating = userRating.toInt()
            )
            commentsRef.push().setValue(comment)
                .addOnSuccessListener {
                    Toast.makeText(this, "Comment posted", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to post comment", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
        }
    }
}