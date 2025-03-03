package com.example.newsheadlines

import com.google.firebase.database.PropertyName

data class User(
    @PropertyName("uid") val uid: String = "",
    @PropertyName("username") val username: String = "",
    @PropertyName("email") val email: String = "",
    @PropertyName("admin") val isAdmin: Boolean = false,
    @PropertyName("journalist") val isJournalist: Boolean = false,
    @PropertyName("user") val isUser: Boolean = true,
    @PropertyName("bio") val bio: String? = "No bio yet"
) {
    constructor() : this("", "", "", false, false, true, null)
}