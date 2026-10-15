package io.github.matheuscruzsouza.sprintt.web

import com.github.matheuscruzsouza.nanospring.annotation.GetMethod
import com.github.matheuscruzsouza.nanospring.annotation.PathVariable
import com.github.matheuscruzsouza.nanospring.annotation.PostMethod
import com.github.matheuscruzsouza.nanospring.annotation.RequestBody
import com.github.matheuscruzsouza.nanospring.annotation.RestController
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity
import com.github.matheuscruzsouza.nanospring.openapi.annotation.Operation
import com.github.matheuscruzsouza.nanospring.openapi.annotation.Tag
import io.github.matheuscruzsouza.sprintt.SprinttApp
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
