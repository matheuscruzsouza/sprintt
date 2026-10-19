package io.github.matheuscruzsouza.sprintt.web

import com.github.matheuscruzsouza.nanospring.annotation.GetMethod
import com.github.matheuscruzsouza.nanospring.annotation.PathVariable
import com.github.matheuscruzsouza.nanospring.annotation.RequestParam
import com.github.matheuscruzsouza.nanospring.annotation.RestController
import com.github.matheuscruzsouza.nanospring.ui.ModelAndView
import io.github.matheuscruzsouza.sprintt.SprinttApp
import io.github.matheuscruzsouza.sprintt.data.model.BoardColumn
import io.github.matheuscruzsouza.sprintt.data.model.ColumnCategory
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
        return renderIssueDetail(key)
    }

    companion object {
        fun enrichIssue(issue: io.github.matheuscruzsouza.sprintt.data.model.Issue, repo: io.github.matheuscruzsouza.sprintt.data.repository.TicketRepository): Map<String, Any?> {
            val subtasks = repo.getSubtasks(issue.key)
            val hasSubtasks = subtasks.isNotEmpty()
            val doneCount = subtasks.count { it.status == IssueStatus.DONE }
            val totalCount = subtasks.size
            val isSubtask = !issue.parentKey.isNullOrBlank()
            val parentIssue = if (isSubtask) repo.getParent(issue.key) else null
            return mapOf(
                "id" to issue.id,
                "key" to issue.key,
                "title" to issue.title,
                "description" to issue.description,
                "type" to issue.type.name,
                "priority" to issue.priority.displayName,
                "status" to issue.status.name,
                "storyPoints" to issue.storyPoints,
                "assignee" to issue.assignee,
                "sprintId" to issue.sprintId,
                "parentKey" to issue.parentKey,
                "parentTitle" to parentIssue?.title,
                "isSubtask" to isSubtask,
                "hasSubtasks" to hasSubtasks,
                "subtaskDoneCount" to doneCount,
                "subtaskTotalCount" to totalCount,
                "subtaskProgressText" to "$doneCount/$totalCount"
            )
        }

        fun renderIssueDetail(key: String): ModelAndView {
            val repo = SprinttApp.instance.ticketRepository
            val issue = repo.getIssueByKey(key)
            val mv = ModelAndView("jira_issue_detail_modal")
            if (issue != null) {
                val subtasks = repo.getSubtasks(issue.key).map { sub ->
                    mapOf(
                        "id" to sub.id,
                        "key" to sub.key,
                        "title" to sub.title,
                        "status" to sub.status.name,
                        "storyPoints" to sub.storyPoints,
                        "assignee" to sub.assignee,
                        "isTodo" to (sub.status == IssueStatus.TODO),
                        "isInProgress" to (sub.status == IssueStatus.IN_PROGRESS),
                        "isDone" to (sub.status == IssueStatus.DONE)
                    )
                }
                val progress = repo.getSubtaskProgress(issue.key)
                val users = repo.getAllUsers()
                val parentIssue = if (!issue.parentKey.isNullOrBlank()) repo.getIssueByKey(issue.parentKey!!) else null
                val isSubtask = issue.type == io.github.matheuscruzsouza.sprintt.data.model.IssueType.SUBTASK || !issue.parentKey.isNullOrBlank()

                mv.addObject("issue", issue)
                mv.addObject("hasIssue", true)
                mv.addObject("subtasks", subtasks)
                mv.addObject("hasSubtasks", subtasks.isNotEmpty())
                mv.addObject("subtaskTotal", progress.total)
                mv.addObject("subtaskDone", progress.done)
                mv.addObject("subtaskInProgress", progress.inProgress)
                mv.addObject("subtaskTodo", progress.todo)
                mv.addObject("subtaskPercent", progress.percent)
                mv.addObject("users", users)
                mv.addObject("isSubtask", isSubtask)
                mv.addObject("parentIssue", parentIssue)
                mv.addObject("hasParentIssue", parentIssue != null)
            } else {
                mv.addObject("hasIssue", false)
                mv.addObject("errorMessage", "Ticket não encontrado")
            }
            return mv
        }

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
            
            // Todos os tickets da sprint ativa (tarefas principais e sub-tarefas) são exibidos no quadro Kanban
            val baseIssues = if (activeSprint != null) {
                repo.getIssuesBySprint(activeSprint.id)
            } else {
                emptyList()
            }

            val projectColumns = repo.getColumnsForProject(activeKey)
            
            // Garante que cada issue tenha seu columnId resolvido
            baseIssues.forEach { iss ->
                if (iss.columnId == null || projectColumns.none { it.id == iss.columnId }) {
                    val fallbackCol = projectColumns.firstOrNull { 
                        when (it.category) {
                            ColumnCategory.TO_DO -> iss.status == IssueStatus.TODO
                            ColumnCategory.IN_PROGRESS -> iss.status == IssueStatus.IN_PROGRESS
                            ColumnCategory.DONE -> iss.status == IssueStatus.DONE
                        }
                    } ?: projectColumns.firstOrNull()
                    iss.columnId = fallbackCol?.id
                }
            }

            val columnsViewModel = projectColumns.map { col ->
                val colIssues = baseIssues.filter { it.columnId == col.id }.map { enrichIssue(it, repo) }
                val colPoints = colIssues.sumOf { (it["storyPoints"] as? Int) ?: 0 }
                val isWipExceeded = col.wipLimit > 0 && colIssues.size > col.wipLimit
                mapOf(
                    "id" to col.id,
                    "title" to col.title,
                    "category" to col.category.name,
                    "categoryDisplayName" to col.category.displayName,
                    "dotClass" to col.category.dotClass,
                    "orderIndex" to col.orderIndex,
                    "wipLimit" to col.wipLimit,
                    "hasWipLimit" to (col.wipLimit > 0),
                    "isWipExceeded" to isWipExceeded,
                    "count" to colIssues.size,
                    "points" to colPoints,
                    "issues" to colIssues
                )
            }

            val todoIssues = baseIssues.filter { it.status == IssueStatus.TODO }.map { enrichIssue(it, repo) }
            val inProgressIssues = baseIssues.filter { it.status == IssueStatus.IN_PROGRESS }.map { enrichIssue(it, repo) }
            val doneIssues = baseIssues.filter { it.status == IssueStatus.DONE }.map { enrichIssue(it, repo) }

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
                .addObject("columns", columnsViewModel)
                .addObject("projectColumnsJson", com.google.gson.Gson().toJson(projectColumns))
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
            val backlogIssuesRaw = repo.getTopLevelProductBacklogIssues(activeKey)

            // Tickets de nível superior da sprint ativa enriquecidos
            val activeSprintIssuesRaw = if (activeSprint != null) repo.getTopLevelIssuesBySprint(activeSprint.id) else emptyList()
            val activeSprintIssues = activeSprintIssuesRaw.map { enrichIssue(it, repo) }

            // Map das sprints futuras com seus tickets de nível superior
            val futureSprintsData = futureSprints.map { s ->
                val sIssuesRaw = repo.getTopLevelIssuesBySprint(s.id)
                val points = sIssuesRaw.sumOf { it.storyPoints }
                val sIssues = sIssuesRaw.map { enrichIssue(it, repo) }
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

            val activeSprintPoints = activeSprintIssuesRaw.sumOf { it.storyPoints }
            val backlogPoints = backlogIssuesRaw.sumOf { it.storyPoints }
            val backlogIssues = backlogIssuesRaw.map { enrichIssue(it, repo) }

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

            // 1. Escopo de demandas de alto nível vs todas as demandas
            val allSprintIssues = if (selectedSprint != null) {
                repo.getIssuesBySprint(selectedSprint.id)
            } else {
                repo.getIssuesByProject(activeKey)
            }

            val topLevelIssues = if (selectedSprint != null) {
                repo.getTopLevelIssuesBySprint(selectedSprint.id)
            } else {
                repo.getAllIssues().filter { it.projectKey.equals(activeKey, ignoreCase = true) && it.parentKey.isNullOrBlank() }
            }

            val totalPoints = topLevelIssues.sumOf { it.storyPoints }
            val doneIssues = topLevelIssues.filter { it.status == IssueStatus.DONE }
            val inProgressIssues = topLevelIssues.filter { it.status == IssueStatus.IN_PROGRESS }
            val todoIssues = topLevelIssues.filter { it.status == IssueStatus.TODO }

            val donePoints = doneIssues.sumOf { it.storyPoints }
            val inProgressPoints = inProgressIssues.sumOf { it.storyPoints }
            val todoPoints = todoIssues.sumOf { it.storyPoints }
            val completionRate = if (totalPoints > 0) (donePoints * 100) / totalPoints else 0

            // 2. Burndown Chart baseado no ciclo real e tempo decorrido
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

            val oneDayMillis = 86_400_000L
            val sprintStart = selectedSprint?.startDate ?: selectedSprint?.createdAt ?: System.currentTimeMillis()

            val currentDayReal = when {
                selectedSprint == null -> 0
                selectedSprint.status == io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.COMPLETED -> totalDays
                selectedSprint.status == io.github.matheuscruzsouza.sprintt.data.model.SprintStatus.PLANNING -> 0
                else -> {
                    val elapsedDays = ((System.currentTimeMillis() - sprintStart).coerceAtLeast(0) / oneDayMillis).toInt()
                    elapsedDays.coerceIn(0, totalDays)
                }
            }

            for (day in 0..totalDays) {
                val cx = ox + (day.toDouble() / totalDays.toDouble()) * plotW
                val idealRemaining = maxPointsVal * (1.0 - (day.toDouble() / totalDays.toDouble()))
                val cyIdeal = oy + (1.0 - (idealRemaining / maxPointsVal)) * plotH
                idealCoords.add(String.format(java.util.Locale.US, "%.1f,%.1f", cx, cyIdeal))

                if (day <= currentDayReal) {
                    val dayTimestamp = sprintStart + (day.toLong() * oneDayMillis) + (oneDayMillis - 1)
                    val burnedPointsAtDay = if (day == 0) {
                        0
                    } else {
                        doneIssues.filter { (it.completedAt ?: it.createdAt) <= dayTimestamp }.sumOf { it.storyPoints }
                    }
                    val remainingPoints = (totalPoints - burnedPointsAtDay).coerceAtLeast(0).toDouble()
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
                        "x" to cx.toInt(),
                        "isCurrentDay" to (day == currentDayReal)
                    )
                )
            }

            val idealLinePoints = idealCoords.joinToString(" ")
            val actualLinePoints = actualCoords.joinToString(" ")

            val lastActualX = ox + (currentDayReal.toDouble() / totalDays.toDouble()) * plotW
            val actualAreaPoints = if (actualCoords.isNotEmpty()) {
                val baseLeft = String.format(java.util.Locale.US, "%.1f,%.1f", ox, oy + plotH)
                val baseRight = String.format(java.util.Locale.US, "%.1f,%.1f", lastActualX, oy + plotH)
                "$baseLeft $actualLinePoints $baseRight"
            } else ""

            // 3. Distribuição por Tipo de Demanda (incluindo SUBTASK)
            val typeCountMap = allSprintIssues.groupBy { it.type }
            val totalAllIssuesCount = allSprintIssues.size
            val typeDistribution = listOf(
                io.github.matheuscruzsouza.sprintt.data.model.IssueType.STORY,
                io.github.matheuscruzsouza.sprintt.data.model.IssueType.TASK,
                io.github.matheuscruzsouza.sprintt.data.model.IssueType.BUG,
                io.github.matheuscruzsouza.sprintt.data.model.IssueType.EPIC,
                io.github.matheuscruzsouza.sprintt.data.model.IssueType.SUBTASK
            ).map { t ->
                val count = typeCountMap[t]?.size ?: 0
                val pts = typeCountMap[t]?.sumOf { it.storyPoints } ?: 0
                val pct = if (totalAllIssuesCount > 0) (count * 100) / totalAllIssuesCount else 0
                mapOf(
                    "typeName" to t.displayName,
                    "typeCode" to t.name,
                    "count" to count,
                    "points" to pts,
                    "percent" to pct
                )
            }

            // 4. Distribuição e Carga por Membro da Equipe
            val users = repo.getAllUsers()
            val memberWorkload = users.map { u ->
                val userIssues = topLevelIssues.filter { it.assignee.equals(u.username, ignoreCase = true) || it.assignee.equals(u.fullName, ignoreCase = true) }
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
                .addObject("totalIssuesCount", topLevelIssues.size)
                .addObject("allIssuesCount", allSprintIssues.size)
                .addObject("doneCount", doneIssues.size)
                .addObject("inProgressCount", inProgressIssues.size)
                .addObject("todoCount", todoIssues.size)
                .addObject("idealLinePoints", idealLinePoints)
                .addObject("actualLinePoints", actualLinePoints)
                .addObject("actualAreaPoints", actualAreaPoints)
                .addObject("actualPointsList", actualPointsList)
                .addObject("midPointsVal", midPointsVal)
                .addObject("burndownDays", burndownDays)
                .addObject("currentSprintDay", currentDayReal)
                .addObject("totalSprintDays", totalDays)
                .addObject("typeDistribution", typeDistribution)
                .addObject("memberWorkload", memberWorkload)
            return appendBatteryInfo(mv)
        }
    }
}

