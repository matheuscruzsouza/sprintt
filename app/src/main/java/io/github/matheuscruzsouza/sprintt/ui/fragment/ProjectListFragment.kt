package io.github.matheuscruzsouza.sprintt.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.matheuscruzsouza.sprintt.SprinttApp
import io.github.matheuscruzsouza.sprintt.databinding.FragmentProjectListBinding
import io.github.matheuscruzsouza.sprintt.ui.adapter.ProjectAdapter

class ProjectListFragment : Fragment() {

    private var _binding: FragmentProjectListBinding? = null
    private val binding get() = _binding!!
    private lateinit var projectAdapter: ProjectAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        projectAdapter = ProjectAdapter { project ->
            Toast.makeText(requireContext(), "Projeto ${project.key}: ${project.name}", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerViewProjects.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewProjects.adapter = projectAdapter

        refreshData()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    fun refreshData() {
        val projects = SprinttApp.instance.ticketRepository.getAllProjects()
        projectAdapter.submitList(projects)
        binding.tvEmptyProjects.visibility = if (projects.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = ProjectListFragment()
    }
}
