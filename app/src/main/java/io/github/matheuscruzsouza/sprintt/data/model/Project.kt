package io.github.matheuscruzsouza.sprintt.data.model

data class Project(
    val id: String,
    val key: String,
    val name: String,
    val description: String = "",
    val leadUserName: String = "Matheus",
    val createdAt: Long = System.currentTimeMillis()
)
