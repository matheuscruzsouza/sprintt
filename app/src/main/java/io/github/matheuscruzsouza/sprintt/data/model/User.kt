package io.github.matheuscruzsouza.sprintt.data.model

data class User(
    val id: String,
    val username: String,
    val fullName: String,
    val email: String,
    val role: String = "Developer",
    val avatarColor: String = "#0052CC"
)
