package io.github.matheuscruzsouza.sprintt.ui.detail

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.github.matheuscruzsouza.sprintt.R
import io.github.matheuscruzsouza.sprintt.SprinttApp
import io.github.matheuscruzsouza.sprintt.data.model.Issue
import io.github.matheuscruzsouza.sprintt.data.model.IssueStatus
import io.github.matheuscruzsouza.sprintt.databinding.ActivityIssueDetailBinding

class IssueDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIssueDetailBinding
    private var issueKey: String? = null
    private var currentIssue: Issue? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIssueDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarDetail)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarDetail.setNavigationOnClickListener { finish() }

        issueKey = intent.getStringExtra(EXTRA_ISSUE_KEY)
        loadIssueData()

        binding.btnStatusTransition.setOnClickListener {
            showTransitionDialog()
        }

        binding.btnDeleteIssue.setOnClickListener {
            confirmDelete()
        }

        binding.btnAddSubtask.setOnClickListener {
            showAddSubtaskDialog()
        }
    }

    private fun loadIssueData() {
        val key = issueKey ?: return
        currentIssue = SprinttApp.instance.ticketRepository.getIssueByKey(key)
        val issue = currentIssue ?: run {
            Toast.makeText(this, "Ticket não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        supportActionBar?.title = issue.key
        binding.tvDetailKey.text = issue.key
        binding.tvDetailTitle.text = issue.title
        binding.tvDetailDescription.text = if (issue.description.isBlank()) {
            "Sem descrição fornecida."
        } else {
            issue.description
        }

        // Vínculo com a tarefa-pai
        if (!issue.parentKey.isNullOrBlank()) {
            binding.tvParentLink.visibility = android.view.View.VISIBLE
            binding.tvParentLink.text = "↳ Sub-tarefa de: ${issue.parentKey}"
            binding.tvParentLink.setOnClickListener {
                val intent = android.content.Intent(this, IssueDetailActivity::class.java).apply {
                    putExtra(EXTRA_ISSUE_KEY, issue.parentKey)
                }
                startActivity(intent)
            }
        } else {
            binding.tvParentLink.visibility = android.view.View.GONE
        }

        binding.tvDetailType.text = "Tipo: ${issue.type.displayName}"
        binding.tvDetailPriority.text = "Prioridade: ${issue.priority.displayName}"
        binding.tvDetailAssignee.text = "Responsável: @${issue.assignee}"
        binding.tvDetailStoryPoints.text = "Pontos de História: ${issue.storyPoints}"

        updateStatusButton(issue.status)
        loadSubtasks(issue)
    }

    private fun loadSubtasks(issue: Issue) {
        val isSubtask = issue.type == io.github.matheuscruzsouza.sprintt.data.model.IssueType.SUBTASK || !issue.parentKey.isNullOrBlank()
        if (isSubtask) {
            binding.cardSubtasks.visibility = android.view.View.GONE
            return
        }

        binding.cardSubtasks.visibility = android.view.View.VISIBLE
        val repo = SprinttApp.instance.ticketRepository
        val subtasks = repo.getSubtasks(issue.key)
        val progress = repo.getSubtaskProgress(issue.key)

        binding.tvSubtaskHeader.text = "SUB-TAREFAS (${progress.done}/${progress.total})"
        binding.pbSubtasks.progress = progress.percent

        binding.llSubtasksContainer.removeAllViews()
        if (subtasks.isEmpty()) {
            binding.tvNoSubtasks.visibility = android.view.View.VISIBLE
        } else {
            binding.tvNoSubtasks.visibility = android.view.View.GONE
            val inflater = layoutInflater
            for (sub in subtasks) {
                val subBinding = io.github.matheuscruzsouza.sprintt.databinding.ItemSubtaskBinding.inflate(inflater, binding.llSubtasksContainer, false)
                subBinding.tvSubtaskKey.text = sub.key
                subBinding.tvSubtaskTitle.text = sub.title
                subBinding.tvSubtaskAssignee.text = "@${sub.assignee}"
                subBinding.cbSubtaskStatus.isChecked = (sub.status == IssueStatus.DONE)

                if (sub.status == IssueStatus.DONE) {
                    subBinding.tvSubtaskTitle.paintFlags = subBinding.tvSubtaskTitle.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                }

                subBinding.cbSubtaskStatus.setOnCheckedChangeListener { _, isChecked ->
                    val newStatus = if (isChecked) IssueStatus.DONE else IssueStatus.TODO
                    repo.updateIssueStatus(sub.key, newStatus)
                    loadSubtasks(issue)
                }

                subBinding.tvSubtaskTitle.setOnClickListener {
                    val intent = android.content.Intent(this, IssueDetailActivity::class.java).apply {
                        putExtra(EXTRA_ISSUE_KEY, sub.key)
                    }
                    startActivity(intent)
                }

                subBinding.btnDeleteSubtask.setOnClickListener {
                    AlertDialog.Builder(this)
                        .setTitle("Excluir sub-tarefa")
                        .setMessage("Deseja excluir a sub-tarefa ${sub.key}?")
                        .setPositiveButton("Excluir") { _, _ ->
                            repo.deleteIssue(sub.key)
                            loadSubtasks(issue)
                            Toast.makeText(this, "Sub-tarefa excluída", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }

                binding.llSubtasksContainer.addView(subBinding.root)
            }
        }
    }

    private fun showAddSubtaskDialog() {
        val issue = currentIssue ?: return
        val input = android.widget.EditText(this).apply {
            hint = "Título da sub-tarefa"
            setPadding(40, 30, 40, 30)
        }

        AlertDialog.Builder(this)
            .setTitle("Adicionar Sub-tarefa")
            .setView(input)
            .setPositiveButton("Adicionar") { _, _ ->
                val title = input.text.toString().trim()
                if (title.isNotBlank()) {
                    SprinttApp.instance.ticketRepository.createSubtask(
                        parentKey = issue.key,
                        title = title,
                        assignee = issue.assignee
                    )
                    loadSubtasks(issue)
                    Toast.makeText(this, "Sub-tarefa adicionada!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateStatusButton(status: IssueStatus) {
        binding.btnStatusTransition.text = "${status.displayName.uppercase()} ▾"
        val textColor = when (status) {
            IssueStatus.TODO -> ContextCompat.getColor(this, R.color.status_todo_text)
            IssueStatus.IN_PROGRESS -> ContextCompat.getColor(this, R.color.status_in_progress_text)
            IssueStatus.DONE -> ContextCompat.getColor(this, R.color.status_done_text)
        }
        binding.btnStatusTransition.backgroundTintList =
            ContextCompat.getColorStateList(this, when (status) {
                IssueStatus.TODO -> R.color.status_todo_bg
                IssueStatus.IN_PROGRESS -> R.color.status_in_progress_bg
                IssueStatus.DONE -> R.color.status_done_bg
            })
        binding.btnStatusTransition.setTextColor(textColor)
    }

    private fun showTransitionDialog() {
        val issue = currentIssue ?: return
        val transitions = issue.status.getAvailableTransitions()

        if (transitions.isEmpty()) {
            Toast.makeText(this, "Nenhuma transição permitida para este status", Toast.LENGTH_SHORT).show()
            return
        }

        val items = transitions.map { "Mudar para: ${it.displayName}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Transição de Fluxo de Trabalho")
            .setItems(items) { _, which ->
                val newStatus = transitions[which]
                SprinttApp.instance.ticketRepository.updateIssueStatus(issue.key, newStatus)
                loadIssueData()
                Toast.makeText(this, "Status alterado para ${newStatus.displayName}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmDelete() {
        val key = issueKey ?: return
        AlertDialog.Builder(this)
            .setTitle("Excluir Ticket")
            .setMessage("Tem certeza de que deseja excluir o ticket $key?")
            .setPositiveButton("Excluir") { _, _ ->
                SprinttApp.instance.ticketRepository.deleteIssue(key)
                Toast.makeText(this, "Ticket excluído com sucesso", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    companion object {
        const val EXTRA_ISSUE_KEY = "extra_issue_key"
    }
}
