package com.example.newsheadlines

import com.google.firebase.database.PropertyName

data class NewsArticle(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val urlToImage: String? = null,
    val author: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val rating: Float = 0f,
    val comments: Map<String, Comment> = emptyMap(),
    val published: Boolean = false, // Matches Firebase "published" field, confirmed as boolean
    val status: String = "pending",
    val category: String = "general",
    val tags: List<String> = emptyList(),
    val rejectionReason: String? = null
)

data class Comment(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val rating: Int = 0
)

data class Notification(
    val message: String = "",
    val timestamp: Long = 0
)