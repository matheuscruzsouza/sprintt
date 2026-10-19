package io.github.matheuscruzsouza.sprintt.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.matheuscruzsouza.sprintt.data.model.BoardColumn
import io.github.matheuscruzsouza.sprintt.data.model.ColumnCategory
import io.github.matheuscruzsouza.sprintt.data.model.Issue
import io.github.matheuscruzsouza.sprintt.data.model.IssuePriority
import io.github.matheuscruzsouza.sprintt.data.model.IssueStatus
import io.github.matheuscruzsouza.sprintt.data.model.IssueType
import io.github.matheuscruzsouza.sprintt.data.model.Project
import io.github.matheuscruzsouza.sprintt.data.model.User
import java.io.File
import java.util.UUID

class TicketRepository(private val context: Context) {

    private val gson = Gson()
    private val projectsFile by lazy { File(context.filesDir, "projects_cache.json") }
    private val issuesFile by lazy { File(context.filesDir, "issues_cache.json") }
    private val usersFile by lazy { File(context.filesDir, "users_cache.json") }
    private val sprintsFile by lazy { File(context.filesDir, "sprints_cache.json") }
    private val columnsFile by lazy { File(context.filesDir, "columns_cache.json") }

    private val projectsList = mutableListOf<Project>()
    private val issuesList = mutableListOf<Issue>()
    private val usersList = mutableListOf<User>()
    private val sprintsList = mutableListOf<io.github.matheuscruzsouza.sprintt.data.model.Sprint>()
    private val columnsList = mutableListOf<BoardColumn>()

    init {
        loadData()
        if (projectsList.isEmpty()) {
            seedInitialData()
        } else if (sprintsList.isEmpty()) {
            seedSprintsOnly()
        }
    }

    private fun seedSprintsOnly() {
        val sTm1 = io.github.matheuscruzsouza.sprintt.data.model.Sprint(UUID.randomUUID().toString(), "TM", "Sprint 1 - Core Setup", "Setup do nano-spring e OpenAPI", io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.ACTIVE)
        val sTm2 = io.github.matheuscruzsouza.sprintt.data.model.Sprint(UUID.randomUUID().toString(), "TM", "Sprint 2 - Notificações", "Alertas e WebSockets", io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.PLANNING)

        val sMob1 = io.github.matheuscruzsouza.sprintt.data.model.Sprint(UUID.randomUUID().toString(), "MOB", "Sprint 1 - MVP Moto G4", "Otimização de memória RAM", io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.ACTIVE)
        val sMob2 = io.github.matheuscruzsouza.sprintt.data.model.Sprint(UUID.randomUUID().toString(), "MOB", "Sprint 2 - Modo Offline", "Sincronização em background", io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.PLANNING)
        sprintsList.addAll(listOf(sTm1, sTm2, sMob1, sMob2))
        
        // Atribuir sTm1 a TM-1 e TM-2 se ainda sem sprint
        issuesList.find { it.key == "TM-1" }?.sprintId = sTm1.id
        issuesList.find { it.key == "TM-2" }?.sprintId = sTm1.id
        issuesList.find { it.key == "TM-3" }?.sprintId = sTm2.id
        issuesList.find { it.key == "MOB-1" }?.sprintId = sMob1.id
        issuesList.find { it.key == "MOB-2" }?.sprintId = sMob1.id
        issuesList.find { it.key == "MOB-3" }?.sprintId = sMob2.id
        saveData()
    }

    @Synchronized
    private fun loadData() {
        try {
            if (projectsFile.exists()) {
                val pType = object : TypeToken<List<Project>>() {}.type
                val pLoaded: List<Project>? = gson.fromJson(projectsFile.readText(), pType)
                if (pLoaded != null) projectsList.addAll(pLoaded)
            }
            if (issuesFile.exists()) {
                val iType = object : TypeToken<List<Issue>>() {}.type
                val iLoaded: List<Issue>? = gson.fromJson(issuesFile.readText(), iType)
                if (iLoaded != null) {
                    iLoaded.forEach { iss ->
                        if (iss.status == IssueStatus.DONE && iss.completedAt == null) {
                            iss.completedAt = iss.createdAt
                        }
                    }
                    issuesList.addAll(iLoaded)
                }
            }
            if (usersFile.exists()) {
                val uType = object : TypeToken<List<User>>() {}.type
                val uLoaded: List<User>? = gson.fromJson(usersFile.readText(), uType)
                if (uLoaded != null) usersList.addAll(uLoaded)
            }
            if (sprintsFile.exists()) {
                val sType = object : TypeToken<List<io.github.matheuscruzsouza.sprintt.data.model.Sprint>>() {}.type
                val sLoaded: List<io.github.matheuscruzsouza.sprintt.data.model.Sprint>? = gson.fromJson(sprintsFile.readText(), sType)
                if (sLoaded != null) {
                    sLoaded.forEach { sp ->
                        if (sp.status == io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.ACTIVE && sp.startDate == null) {
                            sp.startDate = sp.createdAt
                        }
                    }
                    sprintsList.addAll(sLoaded)
                }
            }
            if (columnsFile.exists()) {
                val cType = object : TypeToken<List<BoardColumn>>() {}.type
                val cLoaded: List<BoardColumn>? = gson.fromJson(columnsFile.readText(), cType)
                if (cLoaded != null) columnsList.addAll(cLoaded)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    private fun saveData() {
        try {
            projectsFile.writeText(gson.toJson(projectsList))
            issuesFile.writeText(gson.toJson(issuesList))
            usersFile.writeText(gson.toJson(usersList))
            sprintsFile.writeText(gson.toJson(sprintsList))
            columnsFile.writeText(gson.toJson(columnsList))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun seedInitialData() {
        // Usuários Iniciais
        val u1 = User(UUID.randomUUID().toString(), "matheus", "Matheus Cruz", "matheus@sprintt.local", "Tech Lead", "#0052CC")
        val u2 = User(UUID.randomUUID().toString(), "ana", "Ana Silva", "ana@sprintt.local", "Product Owner", "#36B37E")
        val u3 = User(UUID.randomUUID().toString(), "carlos", "Carlos Dev", "carlos@sprintt.local", "Developer", "#FF8B00")
        val u4 = User(UUID.randomUUID().toString(), "qa_bot", "QA Automator", "qa@sprintt.local", "QA", "#6554C0")
        usersList.addAll(listOf(u1, u2, u3, u4))

        // Projetos Iniciais
        val p1 = Project(UUID.randomUUID().toString(), "TM", "TicketMaster Core", "Plataforma central e APIs", "Matheus")
        val p2 = Project(UUID.randomUUID().toString(), "MOB", "Mobile Android", "Aplicativo nativo para Moto G4 Play", "Matheus")
        val p3 = Project(UUID.randomUUID().toString(), "WEB", "Web Frontend", "Interface HTMX e JMustache", "Carlos Dev")
        projectsList.addAll(listOf(p1, p2, p3))

        val now = System.currentTimeMillis()
        val oneDay = 86_400_000L
        val threeDaysAgo = now - (3 * oneDay)

        // Sprints Iniciais
        val sTm1 = io.github.matheuscruzsouza.sprintt.data.model.Sprint(
            UUID.randomUUID().toString(), "TM", "Sprint 1 - Core Setup", "Setup do nano-spring e OpenAPI",
            io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.ACTIVE,
            startDate = threeDaysAgo
        )
        val sTm2 = io.github.matheuscruzsouza.sprintt.data.model.Sprint(
            UUID.randomUUID().toString(), "TM", "Sprint 2 - Notificações", "Alertas e WebSockets",
            io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.PLANNING
        )

        val sMob1 = io.github.matheuscruzsouza.sprintt.data.model.Sprint(
            UUID.randomUUID().toString(), "MOB", "Sprint 1 - MVP Moto G4", "Otimização de memória RAM",
            io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.ACTIVE,
            startDate = threeDaysAgo
        )
        val sMob2 = io.github.matheuscruzsouza.sprintt.data.model.Sprint(
            UUID.randomUUID().toString(), "MOB", "Sprint 2 - Modo Offline", "Sincronização em background",
            io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.PLANNING
        )
        sprintsList.addAll(listOf(sTm1, sTm2, sMob1, sMob2))

        // Tickets segregados (com sprint ativa, futura e backlog geral)
        issuesList.addAll(
            listOf(
                Issue(UUID.randomUUID().toString(), "TM", 1, "TM-1", "Configurar mDNS sprintt.local na porta 8000", "Ativar nano-spring-discovery", IssueType.TASK, IssuePriority.HIGHEST, IssueStatus.DONE, 5, "Matheus", sTm1.id, createdAt = threeDaysAgo, completedAt = threeDaysAgo + (1 * oneDay)),
                Issue(UUID.randomUUID().toString(), "TM", 2, "TM-2", "Implementar documentação OpenAPI Swagger UI", "Disponibilizar /swagger-ui", IssueType.STORY, IssuePriority.HIGH, IssueStatus.IN_PROGRESS, 5, "Matheus", sTm1.id, createdAt = threeDaysAgo),
                Issue(UUID.randomUUID().toString(), "TM", 3, "TM-3", "Implementar notificações locais para tickets", "Alertar quando atribuído", IssueType.STORY, IssuePriority.MEDIUM, IssueStatus.TODO, 3, "Matheus", sTm2.id),
                Issue(UUID.randomUUID().toString(), "TM", 4, "TM-4", "Otimizar compressão dos assets estáticos", "Reduzir tempo de download", IssueType.TASK, IssuePriority.LOW, IssueStatus.TODO, 2, "Carlos Dev", null),
                Issue(UUID.randomUUID().toString(), "TM", 5, "TM-5", "Configurar schemas OpenAPI das entidades", "Anotar classes com @Schema", IssueType.SUBTASK, IssuePriority.MEDIUM, IssueStatus.DONE, 2, "Matheus", sTm1.id, parentKey = "TM-2", createdAt = threeDaysAgo + (1 * oneDay), completedAt = threeDaysAgo + (2 * oneDay)),
                Issue(UUID.randomUUID().toString(), "TM", 6, "TM-6", "Validar endpoint /swagger-ui no Chrome", "Conferir layout responsivo", IssueType.SUBTASK, IssuePriority.LOW, IssueStatus.IN_PROGRESS, 1, "Matheus", sTm1.id, parentKey = "TM-2", createdAt = threeDaysAgo + (1 * oneDay)),

                Issue(UUID.randomUUID().toString(), "MOB", 1, "MOB-1", "Otimizar telas nativas para o Moto G4 Play", "Manter consumo abaixo de 30MB PSS", IssueType.TASK, IssuePriority.HIGH, IssueStatus.DONE, 3, "Matheus", sMob1.id, createdAt = threeDaysAgo, completedAt = threeDaysAgo + (2 * oneDay)),
                Issue(UUID.randomUUID().toString(), "MOB", 2, "MOB-2", "Painel gerencial administrativo", "Criar abas de servidores, projetos e usuários", IssueType.STORY, IssuePriority.MEDIUM, IssueStatus.IN_PROGRESS, 5, "Carlos Dev", sMob1.id, createdAt = threeDaysAgo),
                Issue(UUID.randomUUID().toString(), "MOB", 3, "MOB-3", "Suporte a cache offline de tickets em disco", "Armazenamento SQLite / JSON rápido", IssueType.STORY, IssuePriority.HIGH, IssueStatus.TODO, 5, "Matheus", sMob2.id),
                Issue(UUID.randomUUID().toString(), "MOB", 4, "MOB-4", "Suporte a tema escuro no app nativo Android", "Tema escuro nativo com Material3", IssueType.TASK, IssuePriority.LOW, IssueStatus.TODO, 3, "Carlos Dev", null),

                Issue(UUID.randomUUID().toString(), "WEB", 1, "WEB-1", "Seletor de projetos dinâmico via HTMX", "Filtrar Kanban sem recarregar tela", IssueType.STORY, IssuePriority.MEDIUM, IssueStatus.TODO, 3, "Carlos Dev", null),
                Issue(UUID.randomUUID().toString(), "WEB", 2, "WEB-2", "Atribuição de tickets por usuário", "Dropdown de membros da equipe", IssueType.TASK, IssuePriority.LOW, IssueStatus.TODO, 2, "Ana Silva", null)
            )
        )
        saveData()
    }

    // --- Projetos ---
    @Synchronized
    fun getAllProjects(): List<Project> = projectsList.toList()

    @Synchronized
    fun getProjectByKey(key: String): Project? = projectsList.find { it.key.equals(key, ignoreCase = true) }

    @Synchronized
    fun createProject(key: String, name: String, description: String = "", lead: String = "Matheus"): Project {
        val upperKey = key.uppercase().trim()
        val existing = getProjectByKey(upperKey)
        if (existing != null) return existing

        val project = Project(UUID.randomUUID().toString(), upperKey, name, description, lead)
        projectsList.add(project)
        saveData()
        return project
    }

    // --- Usuários e Segurança ---
    @Synchronized
    fun getAllUsers(): List<User> = usersList.toList()

    @Synchronized
    fun getUserById(id: String): User? = usersList.find { it.id == id }

    @Synchronized
    fun getUserByUsername(username: String): User? =
        usersList.find { it.username.equals(username.trim(), ignoreCase = true) }

    // Segredo do Servidor para Assinatura HMAC de Sessões
    private val hmacSecret by lazy {
        val secretFile = File(context.filesDir, "session_secret.key")
        if (secretFile.exists()) {
            secretFile.readText().trim()
        } else {
            val key = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString()
            secretFile.writeText(key)
            key
        }
    }

    // Rate Limiting para Ativação de Código: Map de username/IP -> Pair<tentativas, timestamp_bloqueio>
    private val activationAttempts = java.util.concurrent.ConcurrentHashMap<String, Pair<Int, Long>>()

    fun checkAndRecordActivationAttempt(identifier: String): Pair<Boolean, String?> {
        val now = System.currentTimeMillis()
        val record = activationAttempts[identifier]
        if (record != null) {
            val (attempts, blockUntil) = record
            if (now < blockUntil) {
                val remainingSec = ((blockUntil - now) / 1000).coerceAtLeast(1)
                return Pair(false, "Muitas tentativas incorretas. Tente novamente em $remainingSec segundos.")
            }
            if (now >= blockUntil && attempts >= 5) {
                // Bloqueio expirou, reseta contador
                activationAttempts.remove(identifier)
            }
        }
        return Pair(true, null)
    }

    fun recordFailedActivation(identifier: String): String {
        val now = System.currentTimeMillis()
        val current = activationAttempts[identifier]?.first ?: 0
        val next = current + 1
        if (next >= 5) {
            val blockTime = now + (15 * 60 * 1000) // 15 minutos de bloqueio
            activationAttempts[identifier] = Pair(next, blockTime)
            return "Limite de 5 tentativas excedido. Conta bloqueada temporariamente para ativação por 15 minutos."
        } else {
            activationAttempts[identifier] = Pair(next, 0L)
            val remaining = 5 - next
            return "Código inválido. Você tem mais $remaining tentativa(s) antes do bloqueio temporário."
        }
    }

    fun clearActivationAttempts(identifier: String) {
        activationAttempts.remove(identifier)
    }

    fun generateAlphanumericCode(length: Int = 6): String {
        // Caracteres legíveis e não ambíguos (evitando 0, O, 1, I)
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val rnd = java.security.SecureRandom()
        return (1..length)
            .map { chars[rnd.nextInt(chars.length)] }
            .joinToString("")
    }

    fun generateSalt(): String {
        val saltBytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPasswordWithSalt(password: String, salt: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val combined = "$salt:$password".toByteArray(Charsets.UTF_8)
        val hashBytes = digest.digest(combined)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    // Assinatura e Validação de Tokens de Sessão HMAC: formato "username.timestamp.signature"
    fun createSessionToken(username: String): String {
        val cleanUser = username.lowercase().trim()
        val timestamp = System.currentTimeMillis()
        val payload = "$cleanUser.$timestamp"
        val signature = computeHmac(payload, hmacSecret)
        return "$payload.$signature"
    }

    fun validateSessionToken(token: String?): User? {
        if (token.isNullOrBlank()) return null
        val parts = token.trim().split(".")
        if (parts.size != 3) return null
        val username = parts[0]
        val timestampStr = parts[1]
        val signature = parts[2]

        val timestamp = timestampStr.toLongOrNull() ?: return null
        val maxAge = 30L * 24 * 3600 * 1000 // 30 dias de validade
        if (System.currentTimeMillis() - timestamp > maxAge) return null

        val expectedSig = computeHmac("$username.$timestamp", hmacSecret)
        if (expectedSig != signature) return null

        val user = getUserByUsername(username) ?: return null
        if (user.status == "PENDING_SETUP") return null
        return user
    }

    private fun computeHmac(data: String, secret: String): String {
        val sha256Hmac = javax.crypto.Mac.getInstance("HmacSHA256")
        val secretKey = javax.crypto.spec.SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        sha256Hmac.init(secretKey)
        val signedBytes = sha256Hmac.doFinal(data.toByteArray(Charsets.UTF_8))
        return signedBytes.joinToString("") { "%02x".format(it) }
    }

    @Synchronized
    fun createUser(username: String, fullName: String, email: String, role: String): User {
        val code = generateAlphanumericCode(6)
        val user = User(
            id = UUID.randomUUID().toString(),
            username = username.lowercase().trim(),
            fullName = fullName.trim(),
            email = email.trim(),
            role = role,
            status = "PENDING_SETUP",
            setupCode = code,
            passwordHash = null,
            passwordSalt = null,
            codeGeneratedAt = System.currentTimeMillis()
        )
        usersList.add(user)
        saveData()
        return user
    }

    @Synchronized
    fun generateSetupCode(userId: String): String? {
        val user = getUserById(userId) ?: return null
        val code = generateAlphanumericCode(6)
        user.setupCode = code
        user.status = "PENDING_SETUP"
        user.codeGeneratedAt = System.currentTimeMillis()
        clearActivationAttempts(user.username)
        saveData()
        return code
    }

    @Synchronized
    fun generateSetupCodeByUsername(username: String): String? {
        val user = getUserByUsername(username) ?: return null
        return generateSetupCode(user.id)
    }

    sealed class ActivationResult {
        object Success : ActivationResult()
        data class Error(val message: String) : ActivationResult()
    }

    @Synchronized
    fun activateUserWithCode(username: String, code: String, newPassword: String): ActivationResult {
        val user = getUserByUsername(username) ?: return ActivationResult.Error("Usuário @$username não encontrado.")
        
        // 1. Verifica Rate Limiting
        val (allowed, blockMsg) = checkAndRecordActivationAttempt(user.username)
        if (!allowed) {
            return ActivationResult.Error(blockMsg ?: "Muitas tentativas. Aguarde para tentar novamente.")
        }

        // 2. Verifica se o código expirou (24 horas)
        val generatedAt = user.codeGeneratedAt ?: 0L
        val twentyFourHours = 24L * 60 * 60 * 1000
        if (System.currentTimeMillis() - generatedAt > twentyFourHours) {
            return ActivationResult.Error("O código de ativação expirou (válido por 24 horas). Solicite a geração de um novo código.")
        }

        val cleanCode = code.trim().uppercase()
        if (user.setupCode == null || !user.setupCode.equals(cleanCode, ignoreCase = true)) {
            val failMsg = recordFailedActivation(user.username)
            return ActivationResult.Error(failMsg)
        }
        if (newPassword.isBlank() || newPassword.length < 4) {
            return ActivationResult.Error("A senha deve ter no mínimo 4 caracteres.")
        }

        // 3. Salva senha com Salt individual
        val salt = generateSalt()
        user.passwordSalt = salt
        user.passwordHash = hashPasswordWithSalt(newPassword, salt)
        user.setupCode = null
        user.status = "ACTIVE"
        clearActivationAttempts(user.username)
        saveData()
        return ActivationResult.Success
    }

    @Synchronized
    fun authenticateUser(username: String, password: String): User? {
        val user = getUserByUsername(username) ?: return null
        if (user.status == "PENDING_SETUP") {
            return null
        }
        // Migração automática de usuários pré-existentes sem salt
        if (user.passwordSalt == null) {
            val newSalt = generateSalt()
            user.passwordSalt = newSalt
            user.passwordHash = hashPasswordWithSalt(password, newSalt)
            saveData()
            return user
        }
        val targetHash = hashPasswordWithSalt(password, user.passwordSalt!!)
        return if (user.passwordHash == targetHash) user else null
    }

    // --- Tickets / Issues ---
    @Synchronized
    fun getAllIssues(): List<Issue> = issuesList.toList()

    @Synchronized
    fun getIssuesByProject(projectKey: String): List<Issue> {
        return issuesList.filter { it.projectKey.equals(projectKey, ignoreCase = true) }
    }

    @Synchronized
    fun getIssuesByProjectAndStatus(projectKey: String, status: IssueStatus): List<Issue> {
        return issuesList.filter { it.projectKey.equals(projectKey, ignoreCase = true) && it.status == status }
    }

    @Synchronized
    fun getIssueByKey(key: String): Issue? {
        return issuesList.find { it.key.equals(key, ignoreCase = true) }
    }

    @Synchronized
    fun getIssuesByStatus(status: IssueStatus): List<Issue> {
        return issuesList.filter { it.status == status }
    }

    @Synchronized
    fun createIssue(
        projectKey: String = "TM",
        title: String,
        description: String = "",
        type: IssueType = IssueType.TASK,
        priority: IssuePriority = IssuePriority.MEDIUM,
        storyPoints: Int = 1,
        assignee: String = "Unassigned",
        sprintId: String? = null,
        parentKey: String? = null
    ): Issue {
        val upperProjectKey = projectKey.uppercase().trim()
        val projectIssues = getIssuesByProject(upperProjectKey)
        val nextSeq = (projectIssues.maxOfOrNull { it.sequenceNumber } ?: projectIssues.size) + 1
        val issueKey = "$upperProjectKey-$nextSeq"

        val issue = Issue(
            id = UUID.randomUUID().toString(),
            projectKey = upperProjectKey,
            sequenceNumber = nextSeq,
            key = issueKey,
            title = title,
            description = description,
            type = type,
            priority = priority,
            status = IssueStatus.TODO,
            storyPoints = storyPoints,
            assignee = assignee,
            sprintId = sprintId,
            parentKey = parentKey?.trim()?.ifBlank { null }
        )
        issuesList.add(0, issue)
        saveData()
        return issue
    }

    // --- Sub-tarefas ---
    @Synchronized
    fun getSubtasks(parentKey: String): List<Issue> {
        val cleanKey = parentKey.trim().uppercase()
        return issuesList.filter { it.parentKey?.uppercase() == cleanKey }
    }

    @Synchronized
    fun getParent(subtaskKey: String): Issue? {
        val subtask = getIssueByKey(subtaskKey) ?: return null
        val pKey = subtask.parentKey ?: return null
        return getIssueByKey(pKey)
    }

    @Synchronized
    fun getSubtaskProgress(parentKey: String): io.github.matheuscruzsouza.sprintt.data.model.SubtaskProgress {
        val subtasks = getSubtasks(parentKey)
        val total = subtasks.size
        val done = subtasks.count { it.status == IssueStatus.DONE }
        val inProgress = subtasks.count { it.status == IssueStatus.IN_PROGRESS }
        val todo = subtasks.count { it.status == IssueStatus.TODO }
        val percent = if (total > 0) (done * 100) / total else 0
        return io.github.matheuscruzsouza.sprintt.data.model.SubtaskProgress(
            total = total,
            done = done,
            inProgress = inProgress,
            todo = todo,
            percent = percent
        )
    }

    @Synchronized
    fun createSubtask(
        parentKey: String,
        title: String,
        description: String = "",
        priority: IssuePriority = IssuePriority.MEDIUM,
        storyPoints: Int = 1,
        assignee: String = "Unassigned"
    ): Issue? {
        val parent = getIssueByKey(parentKey) ?: return null
        // Não permitir que uma sub-tarefa tenha sub-tarefas (profundidade máxima = 1)
        if (parent.type == IssueType.SUBTASK || !parent.parentKey.isNullOrBlank()) {
            return null
        }

        return createIssue(
            projectKey = parent.projectKey,
            title = title,
            description = description,
            type = IssueType.SUBTASK,
            priority = priority,
            storyPoints = storyPoints,
            assignee = if (assignee.isNotBlank()) assignee else parent.assignee,
            sprintId = parent.sprintId,
            parentKey = parent.key
        )
    }

    @Synchronized
    fun updateIssueStatus(key: String, newStatus: IssueStatus): Boolean {
        val issue = getIssueByKey(key) ?: return false
        issue.status = newStatus
        if (newStatus == IssueStatus.DONE) {
            if (issue.completedAt == null) {
                issue.completedAt = System.currentTimeMillis()
            }
        } else {
            issue.completedAt = null
        }
        saveData()
        return true
    }

    @Synchronized
    fun deleteIssue(key: String): Boolean {
        val cleanKey = key.trim().uppercase()
        // Remove sub-tarefas vinculadas em cascata
        issuesList.removeAll { it.parentKey?.uppercase() == cleanKey }
        val removed = issuesList.removeAll { it.key.equals(cleanKey, ignoreCase = true) }
        if (removed) saveData()
        return removed
    }

    // --- Sprints e Backlog ---
    @Synchronized
    fun getAllSprints(): List<io.github.matheuscruzsouza.sprintt.data.model.Sprint> = sprintsList.toList()

    @Synchronized
    fun getSprintsByProject(projectKey: String): List<io.github.matheuscruzsouza.sprintt.data.model.Sprint> {
        return sprintsList.filter { it.projectKey.equals(projectKey, ignoreCase = true) }
    }

    @Synchronized
    fun getActiveSprint(projectKey: String): io.github.matheuscruzsouza.sprintt.data.model.Sprint? {
        return sprintsList.find { 
            it.projectKey.equals(projectKey, ignoreCase = true) && 
            it.status == io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.ACTIVE 
        }
    }

    @Synchronized
    fun getFutureSprints(projectKey: String): List<io.github.matheuscruzsouza.sprintt.data.model.Sprint> {
        return sprintsList.filter { 
            it.projectKey.equals(projectKey, ignoreCase = true) && 
            it.status == io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.PLANNING 
        }
    }

    @Synchronized
    fun getSprintById(sprintId: String): io.github.matheuscruzsouza.sprintt.data.model.Sprint? {
        return sprintsList.find { it.id == sprintId }
    }

    @Synchronized
    fun createSprint(projectKey: String, name: String, goal: String = ""): io.github.matheuscruzsouza.sprintt.data.model.Sprint {
        val sprint = io.github.matheuscruzsouza.sprintt.data.model.Sprint(
            id = UUID.randomUUID().toString(),
            projectKey = projectKey.uppercase().trim(),
            name = name.trim(),
            goal = goal.trim(),
            status = io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.PLANNING
        )
        sprintsList.add(sprint)
        saveData()
        return sprint
    }

    @Synchronized
    fun startSprint(sprintId: String): Boolean {
        val sprint = getSprintById(sprintId) ?: return false
        // Fecha qualquer sprint ativa atual do mesmo projeto se houver
        sprintsList.filter { it.projectKey.equals(sprint.projectKey, ignoreCase = true) && it.status == io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.ACTIVE }
            .forEach { it.status = io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.COMPLETED }

        sprint.status = io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.ACTIVE
        sprint.startDate = System.currentTimeMillis()
        saveData()
        return true
    }

    @Synchronized
    fun completeSprint(sprintId: String): Boolean {
        val sprint = getSprintById(sprintId) ?: return false
        sprint.status = io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.COMPLETED
        sprint.endDate = System.currentTimeMillis()
        saveData()
        return true
    }

    @Synchronized
    fun getIssuesBySprint(sprintId: String): List<Issue> {
        return issuesList.filter { it.sprintId == sprintId }
    }

    @Synchronized
    fun getTopLevelIssuesBySprint(sprintId: String): List<Issue> {
        return issuesList.filter { it.sprintId == sprintId && it.parentKey.isNullOrBlank() }
    }

    @Synchronized
    fun getProductBacklogIssues(projectKey: String): List<Issue> {
        return issuesList.filter { 
            it.projectKey.equals(projectKey, ignoreCase = true) && it.sprintId.isNullOrBlank() 
        }
    }

    @Synchronized
    fun getTopLevelProductBacklogIssues(projectKey: String): List<Issue> {
        return issuesList.filter { 
            it.projectKey.equals(projectKey, ignoreCase = true) && 
            it.sprintId.isNullOrBlank() && 
            it.parentKey.isNullOrBlank() 
        }
    }

    @Synchronized
    fun assignIssueToSprint(issueKey: String, sprintId: String?): Boolean {
        val issue = getIssueByKey(issueKey) ?: return false
        val newSprintId = if (sprintId.isNullOrBlank() || sprintId == "null") null else sprintId
        issue.sprintId = newSprintId
        // Sincroniza todas as sub-tarefas com a sprint da tarefa principal
        getSubtasks(issue.key).forEach { sub ->
            sub.sprintId = newSprintId
        }
        saveData()
        return true
    }

    // ==========================================
    // GESTÃO DE COLUNAS CONFIGURÁVEIS DO KANBAN
    // ==========================================

    @Synchronized
    fun getColumnsForProject(projectKey: String): List<BoardColumn> {
        val existing = columnsList.filter { it.projectKey.equals(projectKey, ignoreCase = true) }
        if (existing.isNotEmpty()) {
            return existing.sortedBy { it.orderIndex }
        }
        return seedDefaultColumns(projectKey)
    }

    @Synchronized
    fun seedDefaultColumns(projectKey: String): List<BoardColumn> {
        val keyUpper = projectKey.uppercase()
        val c1 = BoardColumn(id = "${keyUpper}_col_todo", projectKey = keyUpper, title = "A Fazer", category = ColumnCategory.TO_DO, orderIndex = 0)
        val c2 = BoardColumn(id = "${keyUpper}_col_prog", projectKey = keyUpper, title = "Em Progresso", category = ColumnCategory.IN_PROGRESS, orderIndex = 1)
        val c3 = BoardColumn(id = "${keyUpper}_col_done", projectKey = keyUpper, title = "Concluído", category = ColumnCategory.DONE, orderIndex = 2)
        columnsList.addAll(listOf(c1, c2, c3))
        
        // Migra tarefas legadas deste projeto que não possuíam columnId
        issuesList.filter { it.projectKey.equals(projectKey, ignoreCase = true) && it.columnId.isNullOrBlank() }.forEach { iss ->
            iss.columnId = when (iss.status) {
                IssueStatus.TODO -> c1.id
                IssueStatus.IN_PROGRESS -> c2.id
                IssueStatus.DONE -> c3.id
            }
        }
        saveData()
        return listOf(c1, c2, c3)
    }

    @Synchronized
    fun addColumn(projectKey: String, title: String, category: ColumnCategory, wipLimit: Int = 0): BoardColumn {
        val current = getColumnsForProject(projectKey)
        val nextOrder = if (current.isEmpty()) 0 else current.maxOf { it.orderIndex } + 1
        val col = BoardColumn(
            projectKey = projectKey.uppercase(),
            title = title.trim(),
            category = category,
            orderIndex = nextOrder,
            wipLimit = wipLimit
        )
        columnsList.add(col)
        saveData()
        return col
    }

    @Synchronized
    fun updateColumn(id: String, title: String, category: ColumnCategory, wipLimit: Int): BoardColumn? {
        val col = columnsList.find { it.id == id } ?: return null
        col.title = title.trim()
        col.category = category
        col.wipLimit = wipLimit

        // Sincroniza o status das tarefas dessa coluna caso a categoria tenha mudado
        val targetStatus = when (category) {
            ColumnCategory.TO_DO -> IssueStatus.TODO
            ColumnCategory.IN_PROGRESS -> IssueStatus.IN_PROGRESS
            ColumnCategory.DONE -> IssueStatus.DONE
        }
        issuesList.filter { it.columnId == id }.forEach { iss ->
            iss.status = targetStatus
            if (targetStatus == IssueStatus.DONE && iss.completedAt == null) {
                iss.completedAt = System.currentTimeMillis()
            } else if (targetStatus != IssueStatus.DONE) {
                iss.completedAt = null
            }
        }
        saveData()
        return col
    }

    @Synchronized
    fun deleteColumn(id: String, targetColumnId: String?): Boolean {
        val col = columnsList.find { it.id == id } ?: return false
        val projectCols = getColumnsForProject(col.projectKey)
        if (projectCols.size <= 1) return false // Não pode deletar a única coluna restante

        val effectiveTargetId = targetColumnId ?: projectCols.firstOrNull { it.id != id }?.id ?: return false
        val targetCol = columnsList.find { it.id == effectiveTargetId } ?: return false

        val targetStatus = when (targetCol.category) {
            ColumnCategory.TO_DO -> IssueStatus.TODO
            ColumnCategory.IN_PROGRESS -> IssueStatus.IN_PROGRESS
            ColumnCategory.DONE -> IssueStatus.DONE
        }
        // Migra tarefas da coluna excluída para a coluna de destino
        issuesList.filter { it.columnId == id }.forEach { iss ->
            iss.columnId = targetCol.id
            iss.status = targetStatus
            if (targetStatus == IssueStatus.DONE && iss.completedAt == null) {
                iss.completedAt = System.currentTimeMillis()
            } else if (targetStatus != IssueStatus.DONE) {
                iss.completedAt = null
            }
        }
        columnsList.remove(col)
        saveData()
        return true
    }

    @Synchronized
    fun reorderColumns(projectKey: String, orderedIds: List<String>): List<BoardColumn> {
        orderedIds.forEachIndexed { index, id ->
            columnsList.find { it.id == id && it.projectKey.equals(projectKey, ignoreCase = true) }?.orderIndex = index
        }
        saveData()
        return getColumnsForProject(projectKey)
    }

    @Synchronized
    fun getColumnById(id: String): BoardColumn? {
        return columnsList.find { it.id == id }
    }

    @Synchronized
    fun updateIssueColumn(issueKey: String, targetColumnId: String): Boolean {
        val issue = getIssueByKey(issueKey) ?: return false
        val targetCol = getColumnById(targetColumnId) ?: return false
        issue.columnId = targetCol.id
        val targetStatus = when (targetCol.category) {
            ColumnCategory.TO_DO -> IssueStatus.TODO
            ColumnCategory.IN_PROGRESS -> IssueStatus.IN_PROGRESS
            ColumnCategory.DONE -> IssueStatus.DONE
        }
        issue.status = targetStatus
        if (targetStatus == IssueStatus.DONE && issue.completedAt == null) {
            issue.completedAt = System.currentTimeMillis()
        } else if (targetStatus != IssueStatus.DONE) {
            issue.completedAt = null
        }
        saveData()
        return true
    }
}

