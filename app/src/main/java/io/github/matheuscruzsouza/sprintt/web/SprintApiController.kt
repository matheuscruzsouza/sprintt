package io.github.matheuscruzsouza.sprintt.web

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
import io.github.matheuscruzsouza.sprintt.data.model.Sprint
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import java.net.URLDecoder

@RestController("/api/v1/sprints")
@Tag(name = "Sprints", description = "Ciclos e Planejamento de Sprints por Projeto")
class SprintApiController {

    private val repository get() = SprinttApp.instance.ticketRepository

    @GetMethod("")
    @Operation(summary = "Listar sprints", description = "Retorna as sprints filtradas por projeto (?projectKey=TM)")
    fun getSprints(@RequestParam("projectKey") projectKey: String?): ResponseEntity<List<Sprint>> {
        val sprints = if (!projectKey.isNullOrBlank()) {
            repository.getSprintsByProject(projectKey)
        } else {
            repository.getAllSprints()
        }
        return ResponseEntity.ok(sprints)
    }

    @PostMethod("")
    @Operation(summary = "Criar nova sprint", description = "Cadastra uma sprint no estado de planejamento")
    fun createSprint(
        session: IHTTPSession,
        @RequestBody body: String?
    ): Any {
        val params = mutableMapOf<String, String>()
        session.parms?.forEach { (k, v) -> if (v != null) params[k.trim()] = v.trim() }
        if (!body.isNullOrBlank()) {
            params.putAll(parseFormData(body))
        }
        val projectKey = params["projectKey"]?.trim()?.uppercase()?.ifBlank { null } ?: "TM"
        val name = params["name"]?.trim()?.ifBlank { null } ?: "Nova Sprint"
        val goal = params["goal"]?.trim() ?: ""

        repository.createSprint(projectKey, name, goal)
        return JiraWebController.buildBacklogModelAndView("jira_backlog_fragment", projectKey)
    }

    @PutMethod("/:id/start")
    @Operation(summary = "Iniciar sprint", description = "Ativa a sprint para execução no Kanban")
    fun startSprint(@PathVariable("id") id: String): Any {
        repository.startSprint(id)
        val sprint = repository.getSprintById(id)
        val projectKey = sprint?.projectKey ?: "TM"
        return JiraWebController.buildBacklogModelAndView("jira_backlog_fragment", projectKey)
    }

    @PutMethod("/:id/complete")
    @Operation(summary = "Encerrar sprint", description = "Marca a sprint como concluída")
    fun completeSprint(@PathVariable("id") id: String): Any {
        repository.completeSprint(id)
        val sprint = repository.getSprintById(id)
        val projectKey = sprint?.projectKey ?: "TM"
        return JiraWebController.buildBacklogModelAndView("jira_backlog_fragment", projectKey)
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
}
