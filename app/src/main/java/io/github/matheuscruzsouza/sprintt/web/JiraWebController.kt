package io.github.matheuscruzsouza.sprintt.web

import com.github.matheuscruzsouza.nanospring.annotation.GetMethod
import com.github.matheuscruzsouza.nanospring.annotation.PathVariable
import com.github.matheuscruzsouza.nanospring.annotation.RequestParam
import com.github.matheuscruzsouza.nanospring.annotation.RestController
import com.github.matheuscruzsouza.nanospring.ui.ModelAndView
import io.github.matheuscruzsouza.sprintt.SprinttApp
import io.github.matheuscruzsouza.sprintt.data.model.IssueStatus
import io.github.matheuscruzsouza.sprintt.service.ServerBatteryProvider

@RestController("/projetos")
class JiraWebController {

    @GetMethod("")
    fun projectsList(): ModelAndView {
        return renderProjectsList()
    }

    @GetMethod("/")
    fun projectsListSlash(): ModelAndView {
        return renderProjectsList()
    }

    @GetMethod("/sprint")
    fun defaultSprint(): ModelAndView {
        val repo = SprinttApp.instance.ticketRepository
        val firstKey = repo.getAllProjects().firstOrNull()?.key ?: "TM"
        return projectSprint(firstKey)
    }

    @GetMethod("/backlog")
    fun defaultBacklog(): ModelAndView {
        val repo = SprinttApp.instance.ticketRepository
        val firstKey = repo.getAllProjects().firstOrNull()?.key ?: "TM"
        return projectBacklog(firstKey)
    }

    @GetMethod("/relatorios")
    fun defaultReports(@RequestParam("sprintId") sprintId: String?): ModelAndView {
        val repo = SprinttApp.instance.ticketRepository
        val firstKey = repo.getAllProjects().firstOrNull()?.key ?: "TM"
        return projectReports(firstKey, sprintId)
    }

    @GetMethod("/reports")
    fun defaultReportsAlias(@RequestParam("sprintId") sprintId: String?): ModelAndView {
        return defaultReports(sprintId)
    }

    @GetMethod("/:key")
    fun projectRoot(@PathVariable("key") key: String): ModelAndView {
        return projectSprint(key)
    }

    @GetMethod("/:key/sprint")
    fun projectSprint(@PathVariable("key") key: String): ModelAndView {
        return buildBoardModelAndView("jira_board", key.uppercase())
    }

    @GetMethod("/:key/quadro")
    fun projectQuadro(@PathVariable("key") key: String): ModelAndView {
        return projectSprint(key)
    }

    @GetMethod("/:key/backlog")
    fun projectBacklog(@PathVariable("key") key: String): ModelAndView {
        return buildBacklogModelAndView("jira_backlog", key.uppercase())
    }

    @GetMethod("/:key/backlog-fragment")
    fun projectBacklogFragment(@PathVariable("key") key: String): ModelAndView {
        return buildBacklogModelAndView("jira_backlog_fragment", key.uppercase())
    }

    @GetMethod("/:key/board-grid")
    fun projectBoardGrid(@PathVariable("key") key: String): ModelAndView {
        return buildBoardModelAndView("jira_kanban_grid", key.uppercase())
    }

    @GetMethod("/:key/relatorios")
    fun projectReports(
        @PathVariable("key") key: String,
        @RequestParam("sprintId") sprintId: String?
    ): ModelAndView {
        return buildReportsModelAndView("jira_reports", key.uppercase(), sprintId)
    }

    @GetMethod("/:key/reports")
    fun projectReportsAlias(
        @PathVariable("key") key: String,
        @RequestParam("sprintId") sprintId: String?
    ): ModelAndView {
        return projectReports(key, sprintId)
    }

    @GetMethod("/issues/:key/detail")
    fun issueDetail(@PathVariable("key") key: String): ModelAndView {
        val repo = SprinttApp.instance.ticketRepository
        val issue = repo.getIssueByKey(key)
        val mv = ModelAndView("jira_issue_detail_modal")
        if (issue != null) {
            mv.addObject("issue", issue)
            mv.addObject("hasIssue", true)
        } else {
            mv.addObject("hasIssue", false)
            mv.addObject("errorMessage", "Ticket não encontrado")
        }
        return mv
    }

    companion object {
        private fun appendBatteryInfo(mv: ModelAndView): ModelAndView {
            val battery = ServerBatteryProvider.getBatteryInfo(SprinttApp.instance)
            return mv.addObject("serverBatteryLevel", battery.level)
                .addObject("serverBatteryFormatted", battery.formatted)
                .addObject("serverBatteryIcon", battery.icon)
                .addObject("serverBatteryCharging", battery.isCharging)
                .addObject("serverBatteryLow", battery.isLow)
                .addObject("serverBatteryStatus", battery.statusText)
                .addObject("serverBatteryPlug", battery.plugType)
        }

        fun renderProjectsList(): ModelAndView {
            val repo = SprinttApp.instance.ticketRepository
            val projects = repo.getAllProjects()
            val allIssues = repo.getAllIssues()

            val projectCards = projects.map { p ->
                val pIssues = allIssues.filter { it.projectKey.equals(p.key, ignoreCase = true) }
                val todo = pIssues.count { it.status == IssueStatus.TODO }
                val inProgress = pIssues.count { it.status == IssueStatus.IN_PROGRESS }
                val done = pIssues.count { it.status == IssueStatus.DONE }
                val total = pIssues.size
                val progressPercent = if (total > 0) (done * 100) / total else 0

                mapOf(
                    "key" to p.key,
                    "name" to p.name,
                    "description" to if (p.description.isBlank()) "Sem descrição informada" else p.description,
                    "leadUserName" to p.leadUserName,
                    "totalCount" to total,
                    "todoCount" to todo,
                    "inProgressCount" to inProgress,
                    "doneCount" to done,
                    "progressPercent" to progressPercent
                )
            }

            val mv = ModelAndView("jira_projects")
                .addObject("projects", projectCards)
                .addObject("totalProjects", projects.size)
                .addObject("totalIssues", allIssues.size)
            return appendBatteryInfo(mv)
        }

        fun buildBoardModelAndView(templateName: String, projectKey: String): ModelAndView {
            val repo = SprinttApp.instance.ticketRepository
            val projects = repo.getAllProjects()
            val currentProject = repo.getProjectByKey(projectKey) ?: projects.firstOrNull()
            val activeKey = currentProject?.key ?: "TM"

            val activeSprint = repo.getActiveSprint(activeKey)
            val users = repo.getAllUsers()
            
            // Apenas tickets que pertencem estritamente à sprint ativa são exibidos no quadro Kanban da Sprint Ativa
            val baseIssues = if (activeSprint != null) {
                repo.getIssuesBySprint(activeSprint.id)
            } else {
                emptyList()
            }

            val todoIssues = baseIssues.filter { it.status == IssueStatus.TODO }
            val inProgressIssues = baseIssues.filter { it.status == IssueStatus.IN_PROGRESS }
            val doneIssues = baseIssues.filter { it.status == IssueStatus.DONE }

            val projectOptions = projects.map { p ->
                mapOf(
                    "key" to p.key,
                    "name" to p.name,
                    "isSelected" to (p.key.equals(activeKey, ignoreCase = true))
                )
            }

            val mv = ModelAndView(templateName)
                .addObject("projects", projectOptions)
                .addObject("users", users)
                .addObject("currentProjectKey", activeKey)
                .addObject("currentProjectName", currentProject?.name ?: activeKey)
                .addObject("activeSprint", activeSprint)
                .addObject("activeSprintId", activeSprint?.id ?: "")
                .addObject("activeSprintName", activeSprint?.name ?: "Nenhuma Sprint Ativa")
                .addObject("activeSprintGoal", activeSprint?.goal ?: "")
                .addObject("hasActiveSprint", activeSprint != null)
                .addObject("todoIssues", todoIssues)
                .addObject("inProgressIssues", inProgressIssues)
                .addObject("doneIssues", doneIssues)
                .addObject("todoCount", todoIssues.size)
                .addObject("inProgressCount", inProgressIssues.size)
                .addObject("doneCount", doneIssues.size)
            return appendBatteryInfo(mv)
        }

        fun buildBacklogModelAndView(templateName: String, projectKey: String): ModelAndView {
            val repo = SprinttApp.instance.ticketRepository
            val projects = repo.getAllProjects()
            val currentProject = repo.getProjectByKey(projectKey) ?: projects.firstOrNull()
            val activeKey = currentProject?.key ?: "TM"

            val projectOptions = projects.map { p ->
                mapOf(
                    "key" to p.key,
                    "name" to p.name,
                    "isSelected" to (p.key.equals(activeKey, ignoreCase = true))
                )
            }

            val users = repo.getAllUsers()
            val activeSprint = repo.getActiveSprint(activeKey)
            val futureSprints = repo.getFutureSprints(activeKey)
            val backlogIssues = repo.getProductBacklogIssues(activeKey)

            // Tickets da sprint ativa
            val activeSprintIssues = if (activeSprint != null) repo.getIssuesBySprint(activeSprint.id) else emptyList()

            // Map das sprints futuras com seus tickets
            val futureSprintsData = futureSprints.map { s ->
                val sIssues = repo.getIssuesBySprint(s.id)
                val points = sIssues.sumOf { it.storyPoints }
                mapOf(
                    "id" to s.id,
                    "name" to s.name,
                    "goal" to if (s.goal.isBlank()) "Sem meta definida" else s.goal,
                    "status" to s.status.name,
                    "issues" to sIssues,
                    "issuesCount" to sIssues.size,
                    "totalPoints" to points
                )
            }

            // Opções de destino para mover tickets
            val sprintOptions = mutableListOf<Map<String, String>>()
            if (activeSprint != null) {
                sprintOptions.add(mapOf("id" to activeSprint.id, "name" to "Sprint Ativa: ${activeSprint.name}"))
            }
            futureSprints.forEach { s ->
                sprintOptions.add(mapOf("id" to s.id, "name" to "Sprint Futura: ${s.name}"))
            }

            val activeSprintPoints = activeSprintIssues.sumOf { it.storyPoints }
            val backlogPoints = backlogIssues.sumOf { it.storyPoints }

            val mv = ModelAndView(templateName)
                .addObject("projects", projectOptions)
                .addObject("users", users)
                .addObject("currentProjectKey", activeKey)
                .addObject("currentProjectName", currentProject?.name ?: activeKey)
                .addObject("activeSprint", activeSprint)
                .addObject("activeSprintId", activeSprint?.id ?: "")
                .addObject("activeSprintName", activeSprint?.name ?: "Nenhuma Sprint Ativa")
                .addObject("activeSprintGoal", activeSprint?.goal ?: "")
                .addObject("hasActiveSprint", activeSprint != null)
                .addObject("activeSprintIssues", activeSprintIssues)
                .addObject("activeSprintPoints", activeSprintPoints)
                .addObject("futureSprints", futureSprintsData)
                .addObject("hasFutureSprints", futureSprintsData.isNotEmpty())
                .addObject("backlogIssues", backlogIssues)
                .addObject("backlogIssuesCount", backlogIssues.size)
                .addObject("backlogPoints", backlogPoints)
                .addObject("sprintOptions", sprintOptions)
            return appendBatteryInfo(mv)
        }

        fun buildReportsModelAndView(templateName: String, projectKey: String, requestedSprintId: String? = null): ModelAndView {
            val repo = SprinttApp.instance.ticketRepository
            val projects = repo.getAllProjects()
            val currentProject = repo.getProjectByKey(projectKey) ?: projects.firstOrNull()
            val activeKey = currentProject?.key ?: "TM"

            val projectSprints = repo.getSprintsByProject(activeKey)
            val selectedSprint = if (!requestedSprintId.isNullOrBlank()) {
                projectSprints.find { it.id == requestedSprintId }
            } else {
                repo.getActiveSprint(activeKey) ?: projectSprints.firstOrNull()
            }

            val allProjectIssues = repo.getIssuesByProject(activeKey)
            val sprintIssues = if (selectedSprint != null) {
                repo.getIssuesBySprint(selectedSprint.id)
            } else {
                allProjectIssues
            }

            val totalPoints = sprintIssues.sumOf { it.storyPoints }
            val doneIssues = sprintIssues.filter { it.status == IssueStatus.DONE }
            val inProgressIssues = sprintIssues.filter { it.status == IssueStatus.IN_PROGRESS }
            val todoIssues = sprintIssues.filter { it.status == IssueStatus.TODO }

            val donePoints = doneIssues.sumOf { it.storyPoints }
            val inProgressPoints = inProgressIssues.sumOf { it.storyPoints }
            val todoPoints = todoIssues.sumOf { it.storyPoints }
            val completionRate = if (totalPoints > 0) (donePoints * 100) / totalPoints else 0

            // 1. Cálculo de Pontos do Burndown Chart (10 dias de ciclo)
            val totalDays = 10
            val chartW = 700.0
            val chartH = 280.0
            val ox = 50.0
            val oy = 20.0
            val plotW = chartW - ox - 20.0
            val plotH = chartH - oy - 40.0

            val idealCoords = mutableListOf<String>()
            val actualCoords = mutableListOf<String>()
            val burndownDays = mutableListOf<Map<String, Any>>()
            val actualPointsList = mutableListOf<Map<String, Any>>()

            val maxPointsVal = if (totalPoints > 0) totalPoints.toDouble() else 10.0
            val midPointsVal = (maxPointsVal / 2.0).toInt()

            // Definir o dia atual de progresso (ex: dia 4 se houver sprint ativa, ou dia correspondente à taxa de entrega)
            val currentDay = if (selectedSprint != null && selectedSprint.status == io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.COMPLETED) {
                totalDays
            } else if (totalPoints > 0) {
                // Progresso baseado em tarefas concluídas e em andamento
                val progressDays = 1 + (donePoints * 7 / totalPoints).coerceIn(0, 7)
                progressDays.coerceAtMost(8)
            } else {
                3
            }

            for (day in 0..totalDays) {
                val cx = ox + (day.toDouble() / totalDays.toDouble()) * plotW
                val idealRemaining = maxPointsVal * (1.0 - (day.toDouble() / totalDays.toDouble()))
                // cy = oy quando remaining = maxPointsVal; cy = oy + plotH quando remaining = 0
                val cyIdeal = oy + (1.0 - (idealRemaining / maxPointsVal)) * plotH
                idealCoords.add(String.format(java.util.Locale.US, "%.1f,%.1f", cx, cyIdeal))

                if (day <= currentDay) {
                    val remainingPoints = if (day == 0) {
                        maxPointsVal
                    } else if (day == currentDay) {
                        (totalPoints - donePoints).coerceAtLeast(0).toDouble()
                    } else {
                        val fraction = day.toDouble() / currentDay.toDouble()
                        val burnedSoFar = donePoints * fraction
                        (maxPointsVal - burnedSoFar).coerceAtLeast(0.0)
                    }

                    val cyActual = oy + (1.0 - (remainingPoints / maxPointsVal)) * plotH
                    actualCoords.add(String.format(java.util.Locale.US, "%.1f,%.1f", cx, cyActual))

                    actualPointsList.add(
                        mapOf(
                            "cx" to String.format(java.util.Locale.US, "%.1f", cx),
                            "cy" to String.format(java.util.Locale.US, "%.1f", cyActual),
                            "pts" to remainingPoints.toInt(),
                            "day" to day
                        )
                    )
                }

                burndownDays.add(
                    mapOf(
                        "dayNumber" to day,
                        "dayLabel" to if (day == 0) "Início" else "D$day",
                        "x" to cx.toInt()
                    )
                )
            }

            val idealLinePoints = idealCoords.joinToString(" ")
            val actualLinePoints = actualCoords.joinToString(" ")
            
            // Área sob a curva real para preenchimento com gradiente moderno
            val lastActualX = ox + (currentDay.toDouble() / totalDays.toDouble()) * plotW
            val actualAreaPoints = if (actualCoords.isNotEmpty()) {
                val baseLeft = String.format(java.util.Locale.US, "%.1f,%.1f", ox, oy + plotH)
                val baseRight = String.format(java.util.Locale.US, "%.1f,%.1f", lastActualX, oy + plotH)
                "$baseLeft $actualLinePoints $baseRight"
            } else ""

            // 2. Distribuição por Tipo de Demanda
            val typeCountMap = sprintIssues.groupBy { it.type }
            val totalCount = sprintIssues.size
            val typeDistribution = listOf(
                io.github.matheuscruzsouza.sprintt.data.model.IssueType.STORY,
                io.github.matheuscruzsouza.sprintt.data.model.IssueType.TASK,
                io.github.matheuscruzsouza.sprintt.data.model.IssueType.BUG,
                io.github.matheuscruzsouza.sprintt.data.model.IssueType.EPIC
            ).map { t ->
                val count = typeCountMap[t]?.size ?: 0
                val pts = typeCountMap[t]?.sumOf { it.storyPoints } ?: 0
                val pct = if (totalCount > 0) (count * 100) / totalCount else 0
                mapOf(
                    "typeName" to t.displayName,
                    "typeCode" to t.name,
                    "count" to count,
                    "points" to pts,
                    "percent" to pct
                )
            }

            // 3. Distribuição e Carga por Membro da Equipe
            val users = repo.getAllUsers()
            val memberWorkload = users.map { u ->
                val userIssues = sprintIssues.filter { it.assignee.equals(u.username, ignoreCase = true) || it.assignee.equals(u.fullName, ignoreCase = true) }
                val uDone = userIssues.count { it.status == IssueStatus.DONE }
                val uPts = userIssues.sumOf { it.storyPoints }
                val uDonePts = userIssues.filter { it.status == IssueStatus.DONE }.sumOf { it.storyPoints }
                val uRate = if (uPts > 0) (uDonePts * 100) / uPts else 0
                mapOf(
                    "name" to u.fullName,
                    "username" to u.username,
                    "avatarColor" to u.avatarColor,
                    "initial" to u.fullName.take(1).uppercase(),
                    "role" to u.role,
                    "ticketCount" to userIssues.size,
                    "doneCount" to uDone,
                    "totalPoints" to uPts,
                    "donePoints" to uDonePts,
                    "completionRate" to uRate
                )
            }

            // Sprints formatadas para o seletor
            val sprintSelectOptions = projectSprints.map { s ->
                mapOf(
                    "id" to s.id,
                    "name" to s.name,
                    "status" to s.status.displayName,
                    "isSelected" to (selectedSprint?.id == s.id)
                )
            }

            val projectOptions = projects.map { p ->
                mapOf(
                    "key" to p.key,
                    "name" to p.name,
                    "isSelected" to (p.key.equals(activeKey, ignoreCase = true))
                )
            }

            val mv = ModelAndView(templateName)
                .addObject("projects", projectOptions)
                .addObject("currentProjectKey", activeKey)
                .addObject("currentProjectName", currentProject?.name ?: activeKey)
                .addObject("selectedSprint", selectedSprint)
                .addObject("hasSelectedSprint", selectedSprint != null)
                .addObject("sprintName", selectedSprint?.name ?: "Todas as Tarefas do Projeto")
                .addObject("sprintStatus", selectedSprint?.status?.displayName ?: "Projeto Geral")
                .addObject("sprintOptions", sprintSelectOptions)
                .addObject("totalPoints", totalPoints)
                .addObject("donePoints", donePoints)
                .addObject("inProgressPoints", inProgressPoints)
                .addObject("todoPoints", todoPoints)
                .addObject("completionRate", completionRate)
                .addObject("totalIssuesCount", sprintIssues.size)
                .addObject("doneCount", doneIssues.size)
                .addObject("inProgressCount", inProgressIssues.size)
                .addObject("todoCount", todoIssues.size)
                .addObject("idealLinePoints", idealLinePoints)
                .addObject("actualLinePoints", actualLinePoints)
                .addObject("actualAreaPoints", actualAreaPoints)
                .addObject("actualPointsList", actualPointsList)
                .addObject("midPointsVal", midPointsVal)
                .addObject("burndownDays", burndownDays)
                .addObject("typeDistribution", typeDistribution)
                .addObject("memberWorkload", memberWorkload)
            return appendBatteryInfo(mv)
        }
    }
}

