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
import io.github.matheuscruzsouza.sprintt.data.model.User
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import java.net.URLDecoder

@RestController("/api/v1/users")
@Tag(name = "Users", description = "Gestão de Membros da Equipe")
class UserApiController {

    private val repository get() = SprinttApp.instance.ticketRepository

    @GetMethod("")
    @Operation(summary = "Listar membros da equipe", description = "Retorna todos os usuários cadastrados")
    fun getAllUsers(): ResponseEntity<List<User>> {
        return ResponseEntity.ok(repository.getAllUsers())
    }

    @GetMethod("/:id")
    @Operation(summary = "Buscar usuário por ID", description = "Retorna os dados do membro da equipe")
    fun getUserById(@PathVariable("id") id: String): ResponseEntity<User> {
        val user = repository.getUserById(id)
        return if (user != null) {
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.status(404).build()
        }
    }

    @PostMethod("")
    @Operation(summary = "Adicionar membro da equipe", description = "Cadastra um novo usuário")
    fun createUser(
        session: IHTTPSession,
        @RequestBody body: String?
    ): ResponseEntity<User> {
        val params = mutableMapOf<String, String>()
        session.parms?.forEach { (k, v) -> if (v != null) params[k.trim()] = v.trim() }
        if (!body.isNullOrBlank()) params.putAll(parseFormData(body))
        val username = params["username"]?.trim()?.ifBlank { null } ?: "dev"
        val fullName = params["fullName"]?.trim()?.ifBlank { null } ?: "Novo Usuário"
        val email = params["email"]?.trim()?.ifBlank { null } ?: "$username@sprintt.local"
        val role = params["role"]?.trim()?.ifBlank { null } ?: "Developer"

        val user = repository.createUser(username, fullName, email, role)
        return ResponseEntity.status(201).body(user)
    }

    @PostMethod("/:id/reset-code")
    @Operation(summary = "Forçar geração de novo código de ativação", description = "Gera um novo código de 6 dígitos para o usuário redefinir a senha")
    fun forceResetCode(
        @PathVariable("id") id: String
    ): ResponseEntity<Map<String, Any?>> {
        val newCode = repository.generateSetupCode(id)
        val user = repository.getUserById(id)
        return if (newCode != null && user != null) {
            ResponseEntity.ok(mapOf(
                "success" to true,
                "userId" to user.id,
                "username" to user.username,
                "setupCode" to newCode,
                "status" to user.status,
                "message" to "Novo código de 6 dígitos gerado com sucesso!"
            ))
        } else {
            ResponseEntity.status(404).body(mapOf(
                "success" to false,
                "message" to "Usuário não encontrado"
            ))
        }
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
