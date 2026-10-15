package io.github.matheuscruzsouza.sprintt.data.model

enum class SprintStatus(val displayName: String) {
    PLANNING("Planejamento"),
    ACTIVE("Em Andamento"),
    COMPLETED("Concluída")
}

data class Sprint(
    val id: String,
    val projectKey: String,
    var name: String,
    var goal: String = "",
    var status: SprintStatus = SprintStatus.PLANNING,
    var startDate: Long? = null,
    var endDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
