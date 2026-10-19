package io.github.matheuscruzsouza.sprintt.web

import com.github.matheuscruzsouza.nanospring.annotation.GetMethod
import com.github.matheuscruzsouza.nanospring.annotation.PostMethod
import com.github.matheuscruzsouza.nanospring.annotation.RequestBody
import com.github.matheuscruzsouza.nanospring.annotation.RequestHeader
import com.github.matheuscruzsouza.nanospring.annotation.RestController
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity
import com.github.matheuscruzsouza.nanospring.openapi.annotation.Operation
import com.github.matheuscruzsouza.nanospring.openapi.annotation.Tag
import io.github.matheuscruzsouza.sprintt.SprinttApp
import io.github.matheuscruzsouza.sprintt.data.model.User
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import java.net.URLDecoder

@RestController("/api/v1/auth")
@Tag(name = "Auth", description = "Autenticação, Sessão e Primeiro Acesso com Código de 6 Dígitos")
class AuthApiController {

    private val repository get() = SprinttApp.instance.ticketRepository

    @PostMethod("/login")
    @Operation(summary = "Login de usuário", description = "Autentica usuário com username e senha")
    fun login(
        session: IHTTPSession,
        @RequestBody body: String?
    ): ResponseEntity<Map<String, Any?>> {
        val params = parseParams(session, body)
        val username = params["username"]?.trim() ?: ""
        val password = params["password"]?.trim() ?: ""

        val user = repository.getUserByUsername(username)
        if (user == null) {
            return ResponseEntity.status(401).body(mapOf(
                "success" to false,
                "error" to "USER_NOT_FOUND",
                "message" to "Usuário @$username não encontrado."
            ))
        }

        if (user.status == "PENDING_SETUP") {
            return ResponseEntity.status(403).body(mapOf(
                "success" to false,
                "error" to "PENDING_SETUP",
                "message" to "Esta conta ainda não possui senha definida. Utilize o código de 6 dígitos para o primeiro acesso."
            ))
        }

        val authenticated = repository.authenticateUser(username, password)
        return if (authenticated != null) {
            val token = repository.createSessionToken(authenticated.username)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "token" to token,
                "user" to authenticated,
                "message" to "Login efetuado com sucesso!"
            ))
        } else {
            ResponseEntity.status(401).body(mapOf(
                "success" to false,
                "error" to "INVALID_CREDENTIALS",
                "message" to "Senha incorreta."
            ))
        }
    }

    @PostMethod("/activate")
    @Operation(summary = "Ativar conta e definir senha", description = "Primeiro acesso com código alfanumérico de 6 dígitos")
    fun activate(
        session: IHTTPSession,
        @RequestBody body: String?
    ): ResponseEntity<Map<String, Any?>> {
        val params = parseParams(session, body)
        val username = params["username"]?.trim() ?: ""
        val code = params["code"]?.trim()?.uppercase() ?: ""
        val password = params["password"]?.trim() ?: ""

        if (username.isBlank() || code.isBlank() || password.isBlank()) {
            return ResponseEntity.status(400).body(mapOf(
                "success" to false,
                "message" to "Preencha o usuário, o código de 6 dígitos e a nova senha."
            ))
        }

        when (val result = repository.activateUserWithCode(username, code, password)) {
            is io.github.matheuscruzsouza.sprintt.data.repository.TicketRepository.ActivationResult.Success -> {
                val user = repository.getUserByUsername(username)
                val token = user?.let { repository.createSessionToken(it.username) } ?: ""
                return ResponseEntity.ok(mapOf(
                    "success" to true,
                    "token" to token,
                    "user" to user,
                    "message" to "Senha definida e conta ativada com sucesso! Você já está autenticado."
                ))
            }
            is io.github.matheuscruzsouza.sprintt.data.repository.TicketRepository.ActivationResult.Error -> {
                return ResponseEntity.status(400).body(mapOf(
                    "success" to false,
                    "message" to result.message
                ))
            }
        }
    }

    @GetMethod("/me")
    @Operation(summary = "Obter dados da sessão atual", description = "Retorna o usuário identificado pelo token de sessão assinado")
    fun me(
        @RequestHeader(name = "X-Sprintt-Token", required = false) authToken: String?,
        session: IHTTPSession
    ): ResponseEntity<Map<String, Any?>> {
        val token = authToken ?: session.headers?.get("x-sprintt-token")
            ?: session.headers?.get("authorization")?.removePrefix("Bearer ")?.trim()

        val user = repository.validateSessionToken(token)
        return if (user != null) {
            ResponseEntity.ok(mapOf(
                "authenticated" to true,
                "user" to user
            ))
        } else {
            ResponseEntity.status(401).body(mapOf(
                "authenticated" to false,
                "user" to null
            ))
        }
    }

    private fun parseParams(session: IHTTPSession, body: String?): Map<String, String> {
        val params = mutableMapOf<String, String>()
        session.parms?.forEach { (k, v) -> if (v != null) params[k.trim()] = v.trim() }
        if (!body.isNullOrBlank()) {
            val cleaned = body.trim().removeSurrounding("\"")
            if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
                try {
                    val map = com.google.gson.Gson().fromJson(cleaned, Map::class.java)
                    map.forEach { (k, v) -> if (k != null && v != null) params[k.toString()] = v.toString() }
                    return params
                } catch (_: Exception) {}
            }
            cleaned.split("&").forEach { pair ->
                val parts = pair.split("=", limit = 2)
                if (parts.isNotEmpty()) {
                    val key = URLDecoder.decode(parts[0], "UTF-8").trim()
                    val value = if (parts.size > 1) URLDecoder.decode(parts[1], "UTF-8").trim() else ""
                    params[key] = value
                }
            }
        }
        return params
    }
}
