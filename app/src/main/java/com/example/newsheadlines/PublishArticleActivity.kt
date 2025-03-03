package com.example.newsheadlines

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class PublishArticleActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var articlesListener: ValueEventListener? = null
    private var notificationsListener: ValueEventListener? = null

    private val photoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageSelection(it) }
    }

    private var selectedImageBase64 by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        setContent { PublishArticleScreen() }
    }

    override fun onDestroy() {
        super.onDestroy()
        articlesListener?.let { database.getReference("articles").removeEventListener(it) }
        notificationsListener?.let { database.getReference("notifications").child(auth.currentUser?.email?.substringBefore("@") ?: "").removeEventListener(it) }
    }

    private fun handleImageSelection(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
            val byteArray = baos.toByteArray()
            selectedImageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun PublishArticleScreen() {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("general") }
        var categoryExpanded by remember { mutableStateOf(false) }
        var articles by remember { mutableStateOf<List<NewsArticle>>(emptyList()) }
        var notifications by remember { mutableStateOf<List<Notification>>(emptyList()) }
        var error by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()
        val user = auth.currentUser
        val author = user?.email?.substringBefore("@") ?: "Unknown"
        val purpleColor = Color(0xFF6200EE)

        LaunchedEffect(Unit) {
            scope.launch(Dispatchers.IO) {
                articlesListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val updatedArticles = mutableListOf<NewsArticle>()
                        snapshot.children.forEach { child ->
                            try {
                                val article = child.getValue(NewsArticle::class.java)
                                if (article != null) {
                                    updatedArticles.add(article)
                                } else {
                                    println("Failed to parse article ${child.key}: Invalid data structure")
                                }
                            } catch (e: DatabaseException) {
                                println("Error parsing article ${child.key}: ${e.message}")
                            }
                        }
                        launch(Dispatchers.Main) {
                            articles = updatedArticles.sortedByDescending { it.timestamp }
                            println("Journalist fetched ${updatedArticles.size} articles: ${updatedArticles.map { it.title }}")
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        println("Articles fetch error: ${error.message}")
                    }
                }.also {
                    database.getReference("articles").orderByChild("author").equalTo(author).addValueEventListener(it)
                }

                notificationsListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val updatedNotifications = snapshot.children.mapNotNull { it.getValue(Notification::class.java) }
                        launch(Dispatchers.Main) {
                            notifications = updatedNotifications
                            println("Fetched ${updatedNotifications.size} notifications")
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {
                        println("Notifications fetch error: ${error.message}")
                    }
                }.also {
                    database.getReference("notifications").child(author).addValueEventListener(it)
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Publish Article", fontWeight = FontWeight.Bold, color = Color.White) },
                    backgroundColor = purpleColor
                )
            },
            backgroundColor = MaterialTheme.colors.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .background(MaterialTheme.colors.background),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = purpleColor)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    maxLines = 10,
                    colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = purpleColor)
                )
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = purpleColor)
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                        listOf("general", "sports", "politics", "games", "anime", "education").forEach { cat ->
                            DropdownMenuItem(
                                content = { Text(cat.replaceFirstChar { it.uppercase() }) },
                                onClick = { category = cat; categoryExpanded = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { photoPicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = purpleColor),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Select Photo", color = Color.White) }
                Spacer(modifier = Modifier.height(16.dp))
                if (selectedImageBase64 != null) Text("Photo selected", color = Color.Green)
                if (error != null) {
                    Text(text = error!!, color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = {
                        if (title.isEmpty() || description.isEmpty()) {
                            error = "Please fill all fields"
                            return@Button
                        }
                        if (user != null) {
                            val article = NewsArticle(
                                id = database.getReference("articles").push().key ?: "",
                                title = title,
                                description = description,
                                urlToImage = selectedImageBase64,
                                author = author,
                                category = category,
                                status = "pending"
                            )
                            database.getReference("articles").child(article.id).setValue(article)
                                .addOnSuccessListener { println("Article saved successfully") }
                                .addOnFailureListener { e ->
                                    println("Failed to save article: ${e.message}")
                                    error = "Failed to save article: ${e.message}"
                                }
                            title = ""
                            description = ""
                            selectedImageBase64 = null
                            error = null
                        } else {
                            error = "User not authenticated"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = purpleColor),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Submit for Approval", color = Color.White) }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your Articles", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (articles.isEmpty()) {
                    Text("No articles submitted yet", modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    LazyColumn { items(articles) { article -> JournalistArticleItem(article, notifications) } }
                }
            }
        }
    }

    @Composable
    fun JournalistArticleItem(article: NewsArticle, notifications: List<Notification>) {
        var editTitle by remember { mutableStateOf(article.title) }
        var editDescription by remember { mutableStateOf(article.description) }
        var showEditDialog by remember { mutableStateOf(false) }
        val rejectionMessage = article.rejectionReason ?: notifications.find { it.message.contains(article.title) }?.message ?: "No reason provided"

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), elevation = 4.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = article.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = article.description, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Status: ${article.status}", fontSize = 14.sp)
                if (article.status == "declined") {
                    Text(text = "Reason: $rejectionMessage", color = Color.Red, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = { showEditDialog = true }) { Text("Edit") }
                        Button(onClick = { database.getReference("articles").child(article.id).removeValue() }) { Text("Delete") }
                    }
                }
            }
        }

        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Edit Article") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editDescription,
                            onValueChange = { editDescription = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val updatedArticle = article.copy(
                            title = editTitle,
                            description = editDescription,
                            status = "pending",
                            rejectionReason = null
                        )
                        database.getReference("articles").child(article.id).setValue(updatedArticle)
                            .addOnSuccessListener { println("Article resubmitted") }
                            .addOnFailureListener { e -> println("Resubmit failed: ${e.message}") }
                        showEditDialog = false
                    }) { Text("Resubmit") }
                },
                dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } }
            )
        }
    }
}