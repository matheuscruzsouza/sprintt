package io.github.matheuscruzsouza.sprintt.ui.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.matheuscruzsouza.sprintt.R
import io.github.matheuscruzsouza.sprintt.data.model.Issue
import io.github.matheuscruzsouza.sprintt.data.model.IssueStatus
import io.github.matheuscruzsouza.sprintt.data.model.IssueType
import io.github.matheuscruzsouza.sprintt.databinding.ItemIssueCardBinding

class IssueAdapter(
    private val onIssueClick: (Issue) -> Unit,
    private val onIssueLongClick: (Issue) -> Unit
) : ListAdapter<Issue, IssueAdapter.IssueViewHolder>(IssueDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueViewHolder {
        val binding = ItemIssueCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IssueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IssueViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class IssueViewHolder(private val binding: ItemIssueCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(issue: Issue) {
            binding.tvIssueKey.text = issue.key
            binding.tvIssueTitle.text = issue.title
            binding.tvAssignee.text = "@${issue.assignee}"
            binding.tvStoryPoints.text = issue.storyPoints.toString()
            binding.tvPriority.text = issue.priority.displayName

            // Configuração do Tipo
            binding.tvIssueType.text = issue.type.displayName
            val typeColor = when (issue.type) {
                IssueType.BUG -> ContextCompat.getColor(itemView.context, R.color.issue_type_bug)
                IssueType.STORY -> ContextCompat.getColor(itemView.context, R.color.issue_type_story)
                IssueType.TASK -> ContextCompat.getColor(itemView.context, R.color.issue_type_task)
                IssueType.EPIC -> ContextCompat.getColor(itemView.context, R.color.issue_type_epic)
                IssueType.SUBTASK -> ContextCompat.getColor(itemView.context, R.color.jira_accent)
            }
            (binding.tvIssueType.background as? GradientDrawable)?.setColor(typeColor)

            // Configuração do Status
            binding.tvStatusBadge.text = issue.status.displayName.uppercase()
            val (statusBg, statusText) = when (issue.status) {
                IssueStatus.TODO -> Pair(
                    ContextCompat.getColor(itemView.context, R.color.status_todo_bg),
                    ContextCompat.getColor(itemView.context, R.color.status_todo_text)
                )
                IssueStatus.IN_PROGRESS -> Pair(
                    ContextCompat.getColor(itemView.context, R.color.status_in_progress_bg),
                    ContextCompat.getColor(itemView.context, R.color.status_in_progress_text)
                )
                IssueStatus.DONE -> Pair(
                    ContextCompat.getColor(itemView.context, R.color.status_done_bg),
                    ContextCompat.getColor(itemView.context, R.color.status_done_text)
                )
            }
            (binding.tvStatusBadge.background as? GradientDrawable)?.setColor(statusBg)
            binding.tvStatusBadge.setTextColor(statusText)

            binding.root.setOnClickListener { onIssueClick(issue) }
            binding.root.setOnLongClickListener {
                onIssueLongClick(issue)
                true
            }
        }
    }

    class IssueDiffCallback : DiffUtil.ItemCallback<Issue>() {
        override fun areItemsTheSame(oldItem: Issue, newItem: Issue): Boolean =
            oldItem.key == newItem.key

        override fun areContentsTheSame(oldItem: Issue, newItem: Issue): Boolean =
            oldItem == newItem
    }
}
