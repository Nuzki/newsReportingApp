package com.example.newsheadlines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val articlesRef: DatabaseReference = database.getReference("articles")
    private var articlesListener: ValueEventListener? = null

    private val _articleList = MutableStateFlow<List<NewsArticle>>(emptyList())
    val articleList = _articleList.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow("newest")
    val sortOption = _sortOption.asStateFlow()

    fun getArticles() {
        viewModelScope.launch {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            println("Attempting to fetch articles for user: ${currentUser?.email ?: "No user"}")
            if (currentUser != null) {
                articlesListener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        println("Raw data snapshot received: ${snapshot.children.count()} children")
                        val allArticles = mutableListOf<NewsArticle>()
                        snapshot.children.forEach { child ->
                            try {
                                val rawValue = child.value
                                println("Raw article data for ${child.key}: $rawValue")
                                val article = child.getValue(NewsArticle::class.java)
                                if (article != null) {
                                    allArticles.add(article)
                                } else {
                                    println("Failed to parse article ${child.key}: Invalid data structure")
                                }
                            } catch (e: DatabaseException) {
                                println("Error parsing article ${child.key}: ${e.message}")
                            }
                        }
                        println("All articles fetched: ${allArticles.map { it.title }} (Count: ${allArticles.size})")
                        updateArticles(snapshot)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        println("Articles fetch error in ViewModel: ${error.message} (Code: ${error.code})")
                    }
                }.also { listener ->
                    articlesRef.addValueEventListener(listener)
                    println("Listener attached successfully for user: ${currentUser.email}")
                }
            } else {
                println("No authenticated user, cannot fetch articles")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        updateArticlesFromCurrentListener()
    }

    fun setSortOption(option: String) {
        _sortOption.value = option
        updateArticlesFromCurrentListener()
    }

    private fun updateArticlesFromCurrentListener() {
        articlesListener?.let { articlesRef.removeEventListener(it) }
        articlesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                println("Updating articles after filter/sort trigger")
                updateArticles(snapshot)
                println("Updated articles: ${_articleList.value.map { it.title }} (Count: ${_articleList.value.size})")
            }
            override fun onCancelled(error: DatabaseError) {
                println("Articles update error in ViewModel: ${error.message} (Code: ${error.code})")
            }
        }.also { articlesRef.addValueEventListener(it) }
    }

    private fun updateArticles(snapshot: DataSnapshot) {
        var articles = snapshot.children.mapNotNull { child ->
            try {
                val rawValue = child.value
                println("Raw article data for ${child.key} in update: $rawValue")
                child.getValue(NewsArticle::class.java)
            } catch (e: DatabaseException) {
                println("Error parsing article ${child.key} in update: ${e.message}")
                null
            }
        }.filter { it.published || it.status == "approved" } // Only published or approved articles
        println("Filtered articles (published/approved): ${articles.map { it.title }} (Count: ${articles.size})")
        if (_searchQuery.value.isNotEmpty()) {
            articles = articles.filter {
                it.title.contains(_searchQuery.value, ignoreCase = true) ||
                        it.description.contains(_searchQuery.value, ignoreCase = true) ||
                        it.category.contains(_searchQuery.value, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(_searchQuery.value, ignoreCase = true) }
            }
            println("After search filter: ${articles.map { it.title }} (Count: ${articles.size})")
        }
        _articleList.value = when (_sortOption.value) {
            "newest" -> articles.sortedByDescending { it.timestamp }
            "oldest" -> articles.sortedBy { it.timestamp }
            "rating" -> articles.sortedByDescending { it.rating }
            else -> articles.sortedByDescending { it.timestamp }
        }
        println("Final sorted articles list: ${_articleList.value.map { it.title }} (Count: ${_articleList.value.size})")
    }

    override fun onCleared() {
        articlesListener?.let { articlesRef.removeEventListener(it) }
        println("ViewModel cleared, listener removed")
        super.onCleared()
    }
}