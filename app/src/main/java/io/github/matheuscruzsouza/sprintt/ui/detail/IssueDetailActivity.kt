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

        binding.tvDetailType.text = "Tipo: ${issue.type.displayName}"
        binding.tvDetailPriority.text = "Prioridade: ${issue.priority.displayName}"
        binding.tvDetailAssignee.text = "Responsável: @${issue.assignee}"
        binding.tvDetailStoryPoints.text = "Pontos de História: ${issue.storyPoints}"

        updateStatusButton(issue.status)
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
