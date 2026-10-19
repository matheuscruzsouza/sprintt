package io.github.matheuscruzsouza.sprintt.data.model

enum class IssueType(val displayName: String) {
    EPIC("Epic"),
    STORY("Story"),
    TASK("Task"),
    BUG("Bug"),
    SUBTASK("Subtask")
}

enum class IssuePriority(val displayName: String) {
    HIGHEST("Highest"),
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low"),
    LOWEST("Lowest")
}

enum class IssueStatus(val displayName: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    DONE("Done");

    fun getAvailableTransitions(): List<IssueStatus> {
        return when (this) {
            TODO -> listOf(IN_PROGRESS)
            IN_PROGRESS -> listOf(TODO, DONE)
            DONE -> listOf(IN_PROGRESS, TODO)
        }
    }
}

data class Issue(
    val id: String,
    val projectKey: String,
    val sequenceNumber: Int,
    val key: String, // ex: TM-1, MOB-2
    var title: String,
    var description: String,
    var type: IssueType,
    var priority: IssuePriority,
    var status: IssueStatus,
    var storyPoints: Int = 0,
    var assignee: String = "Unassigned",
    var sprintId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    var parentKey: String? = null,
    var completedAt: Long? = null,
    var columnId: String? = null
) {
    val suggestedGitBranch: String
        get() {
            val prefix = when (type) {
                IssueType.BUG -> "bug"
                IssueType.SUBTASK -> "subtask"
                else -> "feature"
            }
            val idPart = if (type == IssueType.SUBTASK && !parentKey.isNullOrBlank()) {
                "${parentKey!!.lowercase()}-${key.lowercase()}"
            } else {
                key.lowercase()
            }
            val normalizedTitle = java.text.Normalizer.normalize(title, java.text.Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .lowercase()
                .replace("[^a-z0-9\\s-]".toRegex(), "")
                .trim()
                .replace("\\s+".toRegex(), "-")
                .replace("-+".toRegex(), "-")
                .trim('-')
            
            return if (normalizedTitle.isNotBlank()) {
                "$prefix/$idPart-$normalizedTitle"
            } else {
                "$prefix/$idPart"
            }
        }
}

data class SubtaskProgress(
    val total: Int,
    val done: Int,
    val inProgress: Int,
    val todo: Int,
    val percent: Int
)
