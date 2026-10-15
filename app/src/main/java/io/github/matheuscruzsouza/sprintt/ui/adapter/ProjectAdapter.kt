package io.github.matheuscruzsouza.sprintt.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.github.matheuscruzsouza.sprintt.SprinttApp
import io.github.matheuscruzsouza.sprintt.data.model.Project
import io.github.matheuscruzsouza.sprintt.databinding.ItemProjectCardBinding

class ProjectAdapter(
    private val onProjectClick: (Project) -> Unit
) : ListAdapter<Project, ProjectAdapter.ProjectViewHolder>(ProjectDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProjectViewHolder(private val binding: ItemProjectCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(project: Project) {
            binding.tvProjectKey.text = project.key
            binding.tvProjectName.text = project.name
            binding.tvProjectDesc.text = if (project.description.isBlank()) "Sem descrição" else project.description
            binding.tvProjectLead.text = "Líder: @${project.leadUserName}"

            val ticketCount = SprinttApp.instance.ticketRepository.getIssuesByProject(project.key).size
            binding.tvProjectTicketCount.text = "$ticketCount tickets"

            binding.root.setOnClickListener { onProjectClick(project) }
        }
    }

    class ProjectDiffCallback : DiffUtil.ItemCallback<Project>() {
        override fun areItemsTheSame(oldItem: Project, newItem: Project): Boolean =
            oldItem.key == newItem.key

        override fun areContentsTheSame(oldItem: Project, newItem: Project): Boolean =
            oldItem == newItem
    }
}
