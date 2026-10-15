package io.github.matheuscruzsouza.sprintt.ui.fragment

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import io.github.matheuscruzsouza.sprintt.R
import io.github.matheuscruzsouza.sprintt.SprinttApp
import io.github.matheuscruzsouza.sprintt.data.model.Issue
import io.github.matheuscruzsouza.sprintt.data.model.IssueStatus
import io.github.matheuscruzsouza.sprintt.data.model.IssueType
import io.github.matheuscruzsouza.sprintt.data.model.Project
import io.github.matheuscruzsouza.sprintt.databinding.FragmentAnalyticsBinding

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private var projectList: List<Project> = emptyList()
    private var selectedProjectKey: String = "TM"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupProjectSpinner()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        refreshAnalytics()
    }

    private fun setupProjectSpinner() {
        val repo = SprinttApp.instance.ticketRepository
        projectList = repo.getAllProjects()

        val spinnerItems = mutableListOf<String>()
        projectList.forEach { p ->
            spinnerItems.add("${p.name} (${p.key})")
        }
        spinnerItems.add("Todos os Projetos")

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            spinnerItems
        )
        binding.spinnerProjects.adapter = spinnerAdapter

        binding.spinnerProjects.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedProjectKey = if (position < projectList.size) {
                    projectList[position].key
                } else {
                    "ALL"
                }
                refreshAnalytics()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupListeners() {
        binding.btnOpenWebReports.setOnClickListener {
            val key = if (selectedProjectKey == "ALL") {
                projectList.firstOrNull()?.key ?: "TM"
            } else {
                selectedProjectKey
            }
            val reportsUrl = "https://sprintt.local:8443/projetos/$key/relatorios"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(reportsUrl))
            startActivity(browserIntent)
        }
    }

    fun refreshAnalytics() {
        if (_binding == null) return
        val repo = SprinttApp.instance.ticketRepository

        val allIssues: List<Issue> = if (selectedProjectKey == "ALL") {
            repo.getAllIssues()
        } else {
            repo.getIssuesByProject(selectedProjectKey)
        }

        // 1. Dados da Sprint Ativa
        val activeSprint = if (selectedProjectKey != "ALL") {
            repo.getActiveSprint(selectedProjectKey)
        } else {
            null
        }

        val sprintIssues = if (activeSprint != null) {
            repo.getIssuesBySprint(activeSprint.id)
        } else {
            allIssues
        }

        if (activeSprint != null) {
            binding.tvActiveSprintBadge.text = activeSprint.name
            binding.tvSprintGoal.text = if (activeSprint.goal.isNotBlank()) "Meta: ${activeSprint.goal}" else "Meta: Sem meta definida"
            binding.tvSprintGoal.visibility = View.VISIBLE
        } else if (selectedProjectKey == "ALL") {
            binding.tvActiveSprintBadge.text = "Visão Global"
            binding.tvSprintGoal.text = "Consolidação de todos os projetos e tarefas ativas"
            binding.tvSprintGoal.visibility = View.VISIBLE
        } else {
            binding.tvActiveSprintBadge.text = "Sem Sprint Ativa"
            binding.tvSprintGoal.text = "Mostrando todas as tarefas do backlog do projeto"
            binding.tvSprintGoal.visibility = View.VISIBLE
        }

        val totalPoints = sprintIssues.sumOf { it.storyPoints }
        val doneIssues = sprintIssues.filter { it.status == IssueStatus.DONE }
        val inProgressIssues = sprintIssues.filter { it.status == IssueStatus.IN_PROGRESS }
        val todoIssues = sprintIssues.filter { it.status == IssueStatus.TODO }

        val donePoints = doneIssues.sumOf { it.storyPoints }
        val inProgressPoints = inProgressIssues.sumOf { it.storyPoints }
        val todoPoints = todoIssues.sumOf { it.storyPoints }

        val completionRate = if (totalPoints > 0) (donePoints * 100) / totalPoints else 0
        binding.tvCompletionPercent.text = "$completionRate%"
        binding.pbSprintProgress.progress = completionRate
        binding.tvStoryPointsSummary.text = "$donePoints de $totalPoints Story Points Entregues"

        binding.tvPointsTodo.text = "$todoPoints SP"
        binding.tvPointsInProgress.text = "$inProgressPoints SP"
        binding.tvPointsDone.text = "$donePoints SP"

        // 2. Distribuição por Status
        val totalCount = sprintIssues.size
        val todoPct = if (totalCount > 0) (todoIssues.size * 100) / totalCount else 0
        val inProgressPct = if (totalCount > 0) (inProgressIssues.size * 100) / totalCount else 0
        val donePct = if (totalCount > 0) (doneIssues.size * 100) / totalCount else 0

        binding.pbStatusTodo.progress = todoPct
        binding.tvCountTodo.text = "${todoIssues.size} ($todoPct%)"

        binding.pbStatusInProgress.progress = inProgressPct
        binding.tvCountInProgress.text = "${inProgressIssues.size} ($inProgressPct%)"

        binding.pbStatusDone.progress = donePct
        binding.tvCountDone.text = "${doneIssues.size} ($donePct%)"

        // 3. Distribuição por Tipo
        val storiesCount = sprintIssues.count { it.type == IssueType.STORY }
        val tasksCount = sprintIssues.count { it.type == IssueType.TASK }
        val bugsCount = sprintIssues.count { it.type == IssueType.BUG }
        val epicsCount = sprintIssues.count { it.type == IssueType.EPIC }

        binding.tvCountStories.text = storiesCount.toString()
        binding.tvCountTasks.text = tasksCount.toString()
        binding.tvCountBugs.text = bugsCount.toString()
        binding.tvCountEpics.text = epicsCount.toString()

        // 4. Carga e Produtividade da Equipe
        populateTeamWorkload(sprintIssues)
    }

    private fun populateTeamWorkload(issues: List<Issue>) {
        val container = binding.containerTeamWorkload
        container.removeAllViews()

        val repo = SprinttApp.instance.ticketRepository
        val users = repo.getAllUsers()

        if (users.isEmpty()) {
            val emptyTv = TextView(requireContext()).apply {
                text = "Nenhum membro registrado na equipe."
                setTextColor(Color.GRAY)
                textSize = 12f
            }
            container.addView(emptyTv)
            return
        }

        users.forEach { user ->
            val userIssues = issues.filter {
                it.assignee.equals(user.username, ignoreCase = true) ||
                        it.assignee.equals(user.fullName, ignoreCase = true)
            }
            val uTotalPts = userIssues.sumOf { it.storyPoints }
            val uDonePts = userIssues.filter { it.status == IssueStatus.DONE }.sumOf { it.storyPoints }
            val uRate = if (uTotalPts > 0) (uDonePts * 100) / uTotalPts else 0

            val itemLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 8, 0, 12)
            }

            val headerLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val tvName = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = "${user.fullName} (${user.role})"
                setTextColor(Color.parseColor("#172B4D"))
                textSize = 13f
                paint.isFakeBoldText = true
            }

            val tvMetrics = TextView(requireContext()).apply {
                text = "${userIssues.size} tickets • $uDonePts/$uTotalPts SP ($uRate%)"
                setTextColor(Color.parseColor("#6B778C"))
                textSize = 12f
                paint.isFakeBoldText = true
            }

            headerLayout.addView(tvName)
            headerLayout.addView(tvMetrics)

            val pb = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 14).apply {
                    topMargin = 8
                }
                progress = uRate
                progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#10B981"))
            }

            itemLayout.addView(headerLayout)
            itemLayout.addView(pb)

            container.addView(itemLayout)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): AnalyticsFragment {
            return AnalyticsFragment()
        }
    }
}
