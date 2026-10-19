package io.github.matheuscruzsouza.sprintt.data.model

import java.util.UUID

enum class ColumnCategory(val displayName: String, val dotClass: String) {
    TO_DO("A Fazer", "dot-todo"),
    IN_PROGRESS("Em Progresso", "dot-inprogress"),
    DONE("Concluído", "dot-done")
}

data class BoardColumn(
    val id: String = UUID.randomUUID().toString(),
    val projectKey: String,
    var title: String,
    var category: ColumnCategory = ColumnCategory.TO_DO,
    var orderIndex: Int = 0,
    var colorHex: String? = null,
    var wipLimit: Int = 0
)
