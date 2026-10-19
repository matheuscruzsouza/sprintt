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
import io.github.matheuscruzsouza.sprintt.data.model.BoardColumn
import io.github.matheuscruzsouza.sprintt.data.model.ColumnCategory
import io.github.matheuscruzsouza.sprintt.data.model.Project
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import java.net.URLDecoder

@RestController("/api/v1/projects")
@Tag(name = "Projects", description = "Gerenciamento de Projetos Segregados estilo Jira")
class ProjectApiController {

    private val repository get() = SprinttApp.instance.ticketRepository

    @GetMethod("")
    @Operation(summary = "Listar projetos", description = "Retorna todos os projetos cadastrados no sistema")
    fun getAllProjects(): ResponseEntity<List<Project>> {
        return ResponseEntity.ok(repository.getAllProjects())
    }

    @GetMethod("/:key")
    @Operation(summary = "Buscar projeto pela chave", description = "Retorna os detalhes de um projeto (ex: TM, MOB)")
    fun getProjectByKey(@PathVariable("key") key: String): ResponseEntity<Project> {
        val project = repository.getProjectByKey(key)
        return if (project != null) {
            ResponseEntity.ok(project)
        } else {
            ResponseEntity.status(404).build()
        }
    }

    @PostMethod("")
    @Operation(summary = "Criar novo projeto", description = "Cadastra um novo projeto com chave única")
    fun createProject(
        session: IHTTPSession,
        @RequestBody body: String?
    ): ResponseEntity<Project> {
        val params = mutableMapOf<String, String>()
        session.parms?.forEach { (k, v) -> if (v != null) params[k.trim()] = v.trim() }
        if (!body.isNullOrBlank()) params.putAll(parseFormData(body))
        val key = params["key"]?.trim()?.uppercase()?.ifBlank { null } ?: "NEW"
        val name = params["name"]?.trim()?.ifBlank { null } ?: "Novo Projeto"
        val desc = params["description"]?.trim() ?: ""
        val lead = params["leadUserName"]?.trim()?.ifBlank { null } ?: "Matheus"

        val project = repository.createProject(key, name, desc, lead)
        return ResponseEntity.status(201).body(project)
    }

    // ==========================================
    // ENDPOINTS DE COLUNAS CONFIGURÁVEIS DO KANBAN
    // ==========================================

    @GetMethod("/:key/columns")
    @Operation(summary = "Listar colunas do projeto", description = "Retorna as colunas do quadro Kanban do projeto")
    fun getProjectColumns(@PathVariable("key") key: String): ResponseEntity<List<BoardColumn>> {
        return ResponseEntity.ok(repository.getColumnsForProject(key))
    }

    @PostMethod("/:key/columns")
    @Operation(summary = "Criar nova coluna no projeto", description = "Adiciona uma nova coluna ao quadro Kanban")
    fun addProjectColumn(
        @PathVariable("key") key: String,
        session: IHTTPSession,
        @RequestBody body: String?
    ): ResponseEntity<BoardColumn> {
        val params = mutableMapOf<String, String>()
        session.parms?.forEach { (k, v) -> if (v != null) params[k.trim()] = v.trim() }
        if (!body.isNullOrBlank()) params.putAll(parseFormData(body))
        val title = params["title"]?.trim()?.ifBlank { null } ?: "Nova Coluna"
        val categoryStr = params["category"]?.trim()?.uppercase() ?: "TO_DO"
        val category = try { ColumnCategory.valueOf(categoryStr) } catch (_: Exception) { ColumnCategory.TO_DO }
        val wipLimit = params["wipLimit"]?.toIntOrNull() ?: 0

        val col = repository.addColumn(key, title, category, wipLimit)
        return ResponseEntity.status(201).body(col)
    }

    @PutMethod("/:key/columns/:id")
    @Operation(summary = "Atualizar coluna do projeto", description = "Atualiza nome, categoria ou limite WIP da coluna")
    fun updateProjectColumn(
        @PathVariable("key") key: String,
        @PathVariable("id") id: String,
        session: IHTTPSession,
        @RequestBody body: String?
    ): ResponseEntity<BoardColumn> {
        val params = mutableMapOf<String, String>()
        session.parms?.forEach { (k, v) -> if (v != null) params[k.trim()] = v.trim() }
        if (!body.isNullOrBlank()) params.putAll(parseFormData(body))
        val title = params["title"]?.trim()?.ifBlank { null } ?: "Coluna"
        val categoryStr = params["category"]?.trim()?.uppercase() ?: "IN_PROGRESS"
        val category = try { ColumnCategory.valueOf(categoryStr) } catch (_: Exception) { ColumnCategory.IN_PROGRESS }
        val wipLimit = params["wipLimit"]?.toIntOrNull() ?: 0

        val updated = repository.updateColumn(id, title, category, wipLimit)
        return if (updated != null) {
            ResponseEntity.ok(updated)
        } else {
            ResponseEntity.status(404).build()
        }
    }

    @DeleteMethod("/:key/columns/:id")
    @Operation(summary = "Excluir coluna do projeto", description = "Remove a coluna e migra tarefas para targetColumnId")
    fun deleteProjectColumn(
        @PathVariable("key") key: String,
        @PathVariable("id") id: String,
        @RequestParam("targetColumnId") targetColumnIdQuery: String?,
        session: IHTTPSession
    ): ResponseEntity<Map<String, Any>> {
        val targetColumnId = targetColumnIdQuery ?: session.parms?.get("targetColumnId")
        val success = repository.deleteColumn(id, targetColumnId)
        return if (success) {
            ResponseEntity.ok(mapOf("success" to true))
        } else {
            ResponseEntity.status(400).body(mapOf("success" to false, "message" to "Não é possível excluir a única coluna restante"))
        }
    }

    @PostMethod("/:key/columns-reorder")
    @Operation(summary = "Reordenar colunas", description = "Define a nova ordem das colunas por lista de IDs")
    fun reorderProjectColumns(
        @PathVariable("key") key: String,
        session: IHTTPSession,
        @RequestBody orderedIds: Array<String>?
    ): ResponseEntity<List<BoardColumn>> {
        val ids = orderedIds?.toList() ?: emptyList()
        val reordered = repository.reorderColumns(key, ids)
        return ResponseEntity.ok(reordered)
    }

    private fun parseFormData(rawBody: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (rawBody.isBlank()) return map
        rawBody.split("&").forEach { pair ->
            val parts = pair.split("=", limit = 2)
            if (parts.isNotEmpty()) {
                val key = URLDecoder.decode(parts[0], "UTF-8")
                val value = if (parts.size > 1) URLDecoder.decode(parts[1], "UTF-8") else ""
                map[key] = value
            }
        }
        return map
    }
}
