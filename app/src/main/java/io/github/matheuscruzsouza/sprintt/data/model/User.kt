package io.github.matheuscruzsouza.sprintt.data.model

data class User(
    val id: String,
    val username: String,
    val fullName: String,
    val email: String,
    val role: String = "Developer",
    val avatarColor: String = "#0052CC",
    var status: String = "ACTIVE",
    var setupCode: String? = null,
    var passwordHash: String? = null,
    var passwordSalt: String? = null,
    var codeGeneratedAt: Long? = null
)
