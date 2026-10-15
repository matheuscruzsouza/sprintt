package io.github.matheuscruzsouza.sprintt.web

import com.github.matheuscruzsouza.nanospring.annotation.GetMethod
import com.github.matheuscruzsouza.nanospring.annotation.PathVariable
import com.github.matheuscruzsouza.nanospring.annotation.RequestParam
import com.github.matheuscruzsouza.nanospring.annotation.RestController
import com.github.matheuscruzsouza.nanospring.ui.ModelAndView
import io.github.matheuscruzsouza.sprintt.SprinttApp

@RestController("/jira")
class LegacyJiraWebController {

    @GetMethod("")
    fun legacyIndex(@RequestParam("projectKey") projectKey: String?): ModelAndView {
        val activeKey = projectKey ?: SprinttApp.instance.ticketRepository.getAllProjects().firstOrNull()?.key ?: "TM"
        return JiraWebController.buildBoardModelAndView("jira_board", activeKey.uppercase())
    }

    @GetMethod("/")
    fun legacyIndexSlash(@RequestParam("projectKey") projectKey: String?): ModelAndView {
        return legacyIndex(projectKey)
    }

    @GetMethod("/projects")
    fun legacyProjects(): ModelAndView {
        val repo = SprinttApp.instance.ticketRepository
        val projects = repo.getAllProjects()
        val allIssues = repo.getAllIssues()

        val projectCards = projects.map { p ->
            val pIssues = allIssues.filter { it.projectKey.equals(p.key, ignoreCase = true) }
            val todo = pIssues.count { it.status == io.github.matheuscruzsouza.sprintt.data.model.IssueStatus.TODO }
            val inProgress = pIssues.count { it.status == io.github.matheuscruzsouza.sprintt.data.model.IssueStatus.IN_PROGRESS }
            val done = pIssues.count { it.status == io.github.matheuscruzsouza.sprintt.data.model.IssueStatus.DONE }
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

        return ModelAndView("jira_projects")
            .addObject("projects", projectCards)
            .addObject("totalProjects", projects.size)
            .addObject("totalIssues", allIssues.size)
    }

    @GetMethod("/backlog")
    fun legacyBacklog(@RequestParam("projectKey") projectKey: String?): ModelAndView {
        val activeKey = projectKey ?: SprinttApp.instance.ticketRepository.getAllProjects().firstOrNull()?.key ?: "TM"
        return JiraWebController.buildBacklogModelAndView("jira_backlog", activeKey.uppercase())
    }

    @GetMethod("/backlog-fragment")
    fun legacyBacklogFragment(@RequestParam("projectKey") projectKey: String?): ModelAndView {
        val activeKey = projectKey ?: SprinttApp.instance.ticketRepository.getAllProjects().firstOrNull()?.key ?: "TM"
        return JiraWebController.buildBacklogModelAndView("jira_backlog_fragment", activeKey.uppercase())
    }

    @GetMethod("/board-grid")
    fun legacyBoardGrid(@RequestParam("projectKey") projectKey: String?): ModelAndView {
        val activeKey = projectKey ?: SprinttApp.instance.ticketRepository.getAllProjects().firstOrNull()?.key ?: "TM"
        return JiraWebController.buildBoardModelAndView("jira_kanban_grid", activeKey.uppercase())
    }

    @GetMethod("/reports")
    fun legacyReports(
        @RequestParam("projectKey") projectKey: String?,
        @RequestParam("sprintId") sprintId: String?
    ): ModelAndView {
        val activeKey = projectKey ?: SprinttApp.instance.ticketRepository.getAllProjects().firstOrNull()?.key ?: "TM"
        return JiraWebController.buildReportsModelAndView("jira_reports", activeKey.uppercase(), sprintId)
    }

    @GetMethod("/issues/:key/detail")
    fun legacyIssueDetail(@PathVariable("key") key: String): ModelAndView {
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
}
