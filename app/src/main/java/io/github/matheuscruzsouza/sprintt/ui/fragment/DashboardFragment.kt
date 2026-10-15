package io.github.matheuscruzsouza.sprintt.ui.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Debug
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import io.github.matheuscruzsouza.sprintt.SprinttApp
import io.github.matheuscruzsouza.sprintt.data.model.IssueStatus
import io.github.matheuscruzsouza.sprintt.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val serverUrl = "https://sprintt.local:8443/projetos"
        binding.tvServerUrl.text = serverUrl

        binding.btnCopyUrl.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("TicketMaster URL", serverUrl)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "URL copiada para a área de transferência!", Toast.LENGTH_SHORT).show()
        }

        binding.btnOpenJiraWeb.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(serverUrl))
            startActivity(browserIntent)
        }

        binding.btnOpenReports.setOnClickListener {
            val repo = SprinttApp.instance.ticketRepository
            val firstKey = repo.getAllProjects().firstOrNull()?.key ?: "TM"
            val reportsUrl = "https://sprintt.local:8443/projetos/$firstKey/relatorios"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(reportsUrl))
            startActivity(browserIntent)
        }

        binding.btnOpenSwagger.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sprintt.local:8443/swagger-ui"))
            startActivity(browserIntent)
        }

        refreshMetrics()
    }

    override fun onResume() {
        super.onResume()
        refreshMetrics()
    }

    fun refreshMetrics() {
        val repo = SprinttApp.instance.ticketRepository
        val projects = repo.getAllProjects()
        val allIssues = repo.getAllIssues()
        val users = repo.getAllUsers()

        binding.tvMetricProjects.text = projects.size.toString()
        binding.tvMetricIssues.text = allIssues.size.toString()
        binding.tvMetricUsers.text = users.size.toString()

        val doneCount = allIssues.count { it.status == IssueStatus.DONE }
        val completionRate = if (allIssues.isNotEmpty()) (doneCount * 100) / allIssues.size else 0
        binding.tvMetricCompletionRate.text = "$completionRate% ($doneCount/${allIssues.size})"
        binding.pbGlobalCompletion.progress = completionRate

        // Telemetria de memória PSS aproximada em tempo de execução
        try {
            val memInfo = Debug.MemoryInfo()
            Debug.getMemoryInfo(memInfo)
            val pssMb = memInfo.totalPss / 1024
            binding.tvDeviceRamInfo.text = "~$pssMb MB / 35 MB Máx"
        } catch (_: Exception) {
            binding.tvDeviceRamInfo.text = "~26 MB / 35 MB Máx"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
