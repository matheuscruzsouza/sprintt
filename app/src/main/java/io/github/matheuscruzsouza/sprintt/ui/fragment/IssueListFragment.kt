package io.github.matheuscruzsouza.sprintt.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.matheuscruzsouza.sprintt.SprinttApp
import io.github.matheuscruzsouza.sprintt.data.model.Issue
import io.github.matheuscruzsouza.sprintt.data.model.IssueStatus
import io.github.matheuscruzsouza.sprintt.databinding.FragmentIssueListBinding
import io.github.matheuscruzsouza.sprintt.ui.adapter.IssueAdapter
import io.github.matheuscruzsouza.sprintt.ui.detail.IssueDetailActivity

class IssueListFragment : Fragment() {

    private var _binding: FragmentIssueListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: IssueAdapter
    private var filterStatus: IssueStatus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val statusName = arguments?.getString(ARG_STATUS)
        filterStatus = statusName?.let { IssueStatus.valueOf(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIssueListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = IssueAdapter(
            onIssueClick = { issue ->
                val intent = Intent(requireContext(), IssueDetailActivity::class.java).apply {
                    putExtra(IssueDetailActivity.EXTRA_ISSUE_KEY, issue.key)
                }
                startActivity(intent)
            },
            onIssueLongClick = { issue ->
                showQuickTransitionDialog(issue)
            }
        )

        binding.recyclerViewIssues.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewIssues.adapter = adapter

        refreshData()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    fun refreshData() {
        val repo = SprinttApp.instance.ticketRepository
        val allMatching = if (filterStatus != null) {
            repo.getIssuesByStatus(filterStatus!!)
        } else {
            repo.getAllIssues()
        }
        val issues = allMatching.filter { it.parentKey.isNullOrBlank() }

        binding.tvHeaderCount.text = if (filterStatus != null) {
            "${filterStatus!!.displayName} (${issues.size})"
        } else {
            "Total de Tickets no Backlog (${issues.size})"
        }

        adapter.submitList(issues)
    }

    private fun showQuickTransitionDialog(issue: Issue) {
        val transitions = issue.status.getAvailableTransitions()
        if (transitions.isEmpty()) return

        val items = transitions.map { "Mover para: ${it.displayName}" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("${issue.key} - Transição de Status")
            .setItems(items) { _, which ->
                val targetStatus = transitions[which]
                SprinttApp.instance.ticketRepository.updateIssueStatus(issue.key, targetStatus)
                refreshData()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_STATUS = "arg_status"

        fun newInstance(status: IssueStatus? = null): IssueListFragment {
            return IssueListFragment().apply {
                arguments = Bundle().apply {
                    if (status != null) {
                        putString(ARG_STATUS, status.name)
                    }
                }
            }
        }
    }
}
