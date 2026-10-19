package io.github.matheuscruzsouza.sprintt.web

import com.github.matheuscruzsouza.nanospring.annotation.DeleteMethod
import com.github.matheuscruzsouza.nanospring.annotation.GetMethod
import com.github.matheuscruzsouza.nanospring.annotation.PathVariable
import com.github.matheuscruzsouza.nanospring.annotation.PostMethod
import com.github.matheuscruzsouza.nanospring.annotation.PutMethod
import com.github.matheuscruzsouza.nanospring.annotation.RequestBody
import com.github.matheuscruzsouza.nanospring.annotation.RequestParam
import com.github.matheuscruzsouza.nanospring.annotation.RestController
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity
import com.github.matheuscruzsouza.nanospring.openapi.annotation.Operation
import com.github.matheuscruzsouza.nanospring.openapi.annotation.Tag
import io.github.matheuscruzsouza.sprintt.SprinttApp
import io.github.matheuscruzsouza.sprintt.data.model.Issue
import io.github.matheuscruzsouza.sprintt.data.model.IssuePriority
import io.github.matheuscruzsouza.sprintt.data.model.IssueStatus
import io.github.matheuscruzsouza.sprintt.data.model.IssueType
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import java.net.URLDecoder

@RestController("/api/v1/issues")
@Tag(name = "Issues", description = "Gerenciamento e Fluxos de Status de Tickets por Projeto")
class IssueApiController {

    private val repository get() = SprinttApp.instance.ticketRepository

    @GetMethod("")
    @Operation(summary = "Listar tickets", description = "Retorna todos os tickets ou filtrados por projeto (?projectKey=TM)")
    fun getIssues(@RequestParam("projectKey") projectKey: String?): ResponseEntity<List<Issue>> {
        val issues = if (!projectKey.isNullOrBlank()) {
            repository.getIssuesByProject(projectKey)
        } else {
            repository.getAllIssues()
        }
        return ResponseEntity.ok(issues)
    }

    @GetMethod("/:key")
    @Operation(summary = "Obter ticket pela chave", description = "Retorna os detalhes de um ticket (ex: TM-1, MOB-2)")
    fun getIssueByKey(@PathVariable("key") key: String): ResponseEntity<Issue> {
        val issue = repository.getIssueByKey(key)
        return if (issue != null) {
            ResponseEntity.ok(issue)
        } else {
            ResponseEntity.status(404).build()
        }
    }

    @PostMethod("")
    @Operation(summary = "Criar novo ticket no projeto", description = "Cadastra um ticket associado ao projeto e responsável")
    fun createIssue(
        session: IHTTPSession,
        @RequestBody body: String?
    ): Any {
        val formParams = mutableMapOf<String, String>()

        // 1. Extrai parâmetros do NanoHTTPD session (formulários x-www-form-urlencoded do HTMX e query params)
        session.parms?.forEach { (k, v) ->
            if (v != null) formParams[k.trim()] = v.trim()
        }

        // 2. Se houver body JSON (ex: Swagger UI ou chamadas REST), faz o merge
        if (!body.isNullOrBlank()) {
            formParams.putAll(parseFormData(body))
        }

        // Validar usuário da sessão ativa via token assinado HMAC (X-Sprintt-Token ou Cookie)
        val sessionToken = session.headers?.get("x-sprintt-token")?.trim()?.ifBlank { null }
            ?: session.headers?.get("X-Sprintt-Token")?.trim()?.ifBlank { null }
            ?: session.headers?.get("authorization")?.removePrefix("Bearer ")?.trim()?.ifBlank { null }
            ?: extractCookieToken(session)

        val validatedUser = repository.validateSessionToken(sessionToken)
        val defaultAssignee = validatedUser?.username ?: "Matheus"

        val view = formParams["view"].orEmpty().trim().lowercase()
        val projectKey = formParams["projectKey"]?.trim()?.uppercase()?.ifBlank { null } ?: "TM"
        val title = formParams["title"]?.trim()?.ifBlank { null } ?: "Sem título"
        val description = formParams["description"]?.trim() ?: ""
        val typeStr = formParams["type"] ?: "TASK"
        val priorityStr = formParams["priority"] ?: "MEDIUM"
        val spStr = formParams["storyPoints"] ?: "1"
        val assignee = formParams["assignee"]?.trim()?.ifBlank { null } ?: defaultAssignee

        val type = try { IssueType.valueOf(typeStr) } catch (_: Exception) { IssueType.TASK }
        val priority = try { IssuePriority.valueOf(priorityStr) } catch (_: Exception) { IssuePriority.MEDIUM }
        val storyPoints = spStr.toIntOrNull() ?: 1

        var sprintId = formParams["sprintId"]?.trim()?.ifBlank { null }
        if (sprintId == null && (view == "board" || view.isBlank())) {
            val activeSprint = repository.getActiveSprint(projectKey)
            if (activeSprint != null) {
                sprintId = activeSprint.id
            }
        }

        val parentKey = formParams["parentKey"]?.trim()?.ifBlank { null }

        val createdIssue = repository.createIssue(
            projectKey = projectKey,
            title = title,
            description = description,
            type = type,
            priority = priority,
            storyPoints = storyPoints,
            assignee = assignee,
            sprintId = sprintId,
            parentKey = parentKey
        )

        return when {
            view == "backlog" -> {
                JiraWebController.buildBacklogModelAndView("jira_backlog_fragment", projectKey)
            }
            view == "board" -> {
                JiraWebController.buildBoardModelAndView("jira_kanban_grid", projectKey)
            }
            view == "modal" && !parentKey.isNullOrBlank() -> {
                JiraWebController.renderIssueDetail(parentKey)
            }
            body?.trim()?.startsWith("{") == true && !session.parms.containsKey("view") -> {
                ResponseEntity.ok(createdIssue)
            }
            else -> {
                JiraWebController.buildBoardModelAndView("jira_kanban_grid", projectKey)
            }
        }
    }

    @GetMethod("/:key/subtasks")
    @Operation(summary = "Listar sub-tarefas do ticket", description = "Retorna todas as sub-tarefas vinculadas ao ticket informado")
    fun getSubtasks(@PathVariable("key") key: String): ResponseEntity<List<Issue>> {
        val subtasks = repository.getSubtasks(key)
        return ResponseEntity.ok(subtasks)
    }

    @PostMethod("/:key/subtasks")
    @Operation(summary = "Criar sub-tarefa para o ticket", description = "Adiciona uma sub-tarefa diretamente vinculada ao ticket informado")
    fun createSubtask(
        @PathVariable("key") key: String,
        session: IHTTPSession,
        @RequestBody body: String?
    ): Any {
        val formParams = mutableMapOf<String, String>()
        session.parms?.forEach { (k, v) -> if (v != null) formParams[k.trim()] = v.trim() }
        if (!body.isNullOrBlank()) formParams.putAll(parseFormData(body))

        val title = formParams["title"]?.trim()?.ifBlank { null } ?: "Nova sub-tarefa"
        val description = formParams["description"]?.trim() ?: ""
        val priorityStr = formParams["priority"] ?: "MEDIUM"
        val spStr = formParams["storyPoints"] ?: "1"
        val assignee = formParams["assignee"]?.trim()?.ifBlank { null } ?: "Unassigned"

        val priority = try { IssuePriority.valueOf(priorityStr) } catch (_: Exception) { IssuePriority.MEDIUM }
        val storyPoints = spStr.toIntOrNull() ?: 1

        val subtask = repository.createSubtask(
            parentKey = key,
            title = title,
            description = description,
            priority = priority,
            storyPoints = storyPoints,
            assignee = assignee
        )

        val view = formParams["view"].orEmpty().trim().lowercase()
        return when {
            body?.trim()?.startsWith("{") == true && !formParams.containsKey("view") -> {
                if (subtask != null) {
                    ResponseEntity.ok(subtask)
                } else {
                    ResponseEntity.status(400).body(mapOf("error" to "Não foi possível criar a sub-tarefa"))
                }
            }
            else -> {
                JiraWebController.renderIssueDetail(key)
            }
        }
    }

    @PutMethod("/:key/sprint")
    @Operation(summary = "Mover ticket para uma sprint ou backlog", description = "Atribui o ticket a uma sprint ou desassocia para o backlog geral")
    fun assignSprint(
        @PathVariable("key") key: String,
        @RequestParam("sprintId") sprintIdQuery: String?,
        session: IHTTPSession,
        @RequestBody body: String?
    ): Any {
        val params = mutableMapOf<String, String>()
        session.parms?.forEach { (k, v) -> if (v != null) params[k.trim()] = v.trim() }
        if (!body.isNullOrBlank()) params.putAll(parseFormData(body))
        val sprintId = sprintIdQuery ?: params["sprintId"]
        repository.assignIssueToSprint(key, sprintId)

        val issue = repository.getIssueByKey(key)
        val projectKey = issue?.projectKey ?: "TM"
        return JiraWebController.buildBacklogModelAndView("jira_backlog_fragment", projectKey)
    }

    @PutMethod("/:key/status")
    @Operation(summary = "Mover status do ticket", description = "Transiciona o status do ticket (TODO, IN_PROGRESS, DONE)")
    fun updateStatus(
        @PathVariable("key") key: String,
        @RequestParam("status") statusQuery: String?,
        session: IHTTPSession,
        @RequestBody body: String?
    ): Any {
        val params = mutableMapOf<String, String>()
        session.parms?.forEach { (k, v) -> if (v != null) params[k.trim()] = v.trim() }
        if (!body.isNullOrBlank()) params.putAll(parseFormData(body))
        val statusStr = statusQuery ?: params["status"].orEmpty()
        val columnId = params["columnId"]

        if (!columnId.isNullOrBlank()) {
            repository.updateIssueColumn(key, columnId)
        } else {
            val col = repository.getColumnById(statusStr)
            if (col != null) {
                repository.updateIssueColumn(key, col.id)
            } else {
                val targetStatus = try {
                    IssueStatus.valueOf(statusStr)
                } catch (_: Exception) {
                    null
                }

                if (targetStatus != null) {
                    repository.updateIssueStatus(key, targetStatus)
                }
            }
        }

        val issue = repository.getIssueByKey(key)
        val projectKey = issue?.projectKey ?: "TM"
        val view = params["view"]?.trim()?.lowercase()
        val redirectParentKey = params["parentKey"] ?: issue?.parentKey
        return when {
            view == "modal" && !redirectParentKey.isNullOrBlank() -> {
                JiraWebController.renderIssueDetail(redirectParentKey)
            }
            view == "backlog" -> {
                JiraWebController.buildBacklogModelAndView("jira_backlog_fragment", projectKey)
            }
            else -> {
                JiraWebController.buildBoardModelAndView("jira_kanban_grid", projectKey)
            }
        }
    }

    @DeleteMethod("/:key")
    @Operation(summary = "Excluir ticket", description = "Remove um ticket do projeto e suas sub-tarefas em cascata")
    fun deleteIssue(
        @PathVariable("key") key: String,
        session: IHTTPSession
    ): Any {
        val parent = repository.getParent(key)
        val deleted = repository.deleteIssue(key)
        val view = session.parms?.get("view")?.trim()?.lowercase()
        if (view == "modal" && parent != null) {
            return JiraWebController.renderIssueDetail(parent.key)
        }
        return if (deleted) {
            ResponseEntity.ok(mapOf("success" to true, "deletedKey" to key))
        } else {
            ResponseEntity.status(404).body(mapOf("success" to false, "message" to "Ticket não encontrado"))
        }
    }

    private fun parseFormData(rawBody: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val cleaned = rawBody.trim().removeSurrounding("\"")
        if (cleaned.isBlank()) return map

        if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
            try {
                val jsonMap = com.google.gson.Gson().fromJson(cleaned, Map::class.java)
                jsonMap.forEach { (k, v) ->
                    if (k != null && v != null) map[k.toString()] = v.toString()
                }
                return map
            } catch (_: Exception) {}
        }

        cleaned.split("&").forEach { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.isNotEmpty()) {
                val key = URLDecoder.decode(parts[0], "UTF-8").trim()
                val value = if (parts.size > 1) URLDecoder.decode(parts[1], "UTF-8").trim() else ""
                map[key] = value
            }
        }
        return map
    }

    private fun extractCookieToken(session: IHTTPSession): String? {
        val cookieHeader = session.headers?.get("cookie") ?: session.headers?.get("Cookie") ?: return null
        cookieHeader.split(";").forEach { pair ->
            val parts = pair.trim().split("=", limit = 2)
            if (parts.size == 2 && parts[0].trim() == "sprintt_token") {
                return parts[1].trim()
            }
        }
        return null
    }
}
