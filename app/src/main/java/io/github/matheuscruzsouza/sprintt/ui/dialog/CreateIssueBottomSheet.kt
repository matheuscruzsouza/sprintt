package io.github.matheuscruzsouza.sprintt.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.matheuscruzsouza.sprintt.SprinttApp
import io.github.matheuscruzsouza.sprintt.data.model.IssuePriority
import io.github.matheuscruzsouza.sprintt.data.model.IssueType
import io.github.matheuscruzsouza.sprintt.databinding.DialogCreateIssueBinding

class CreateIssueBottomSheet(
    private val onIssueCreated: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogCreateIssueBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateIssueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val availableTypes = IssueType.values().filter { it != IssueType.SUBTASK }
        val typeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            availableTypes.map { it.displayName }
        )
        binding.spinnerType.adapter = typeAdapter

        val priorityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            IssuePriority.values().map { it.displayName }
        )
        binding.spinnerPriority.adapter = priorityAdapter
        binding.spinnerPriority.setSelection(IssuePriority.MEDIUM.ordinal)

        binding.btnSubmit.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            if (title.isEmpty()) {
                binding.etTitle.error = "Informe um título"
                return@setOnClickListener
            }

            val desc = binding.etDescription.text.toString().trim()
            val type = availableTypes[binding.spinnerType.selectedItemPosition]
            val priority = IssuePriority.values()[binding.spinnerPriority.selectedItemPosition]
            val sp = binding.etStoryPoints.text.toString().toIntOrNull() ?: 1

            SprinttApp.instance.ticketRepository.createIssue(
                title = title,
                description = desc,
                type = type,
                priority = priority,
                storyPoints = sp
            )

            Toast.makeText(requireContext(), "Ticket criado com sucesso!", Toast.LENGTH_SHORT).show()
            onIssueCreated()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
