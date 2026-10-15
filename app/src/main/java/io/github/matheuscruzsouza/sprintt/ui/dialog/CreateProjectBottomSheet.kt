package io.github.matheuscruzsouza.sprintt.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.matheuscruzsouza.sprintt.SprinttApp
import io.github.matheuscruzsouza.sprintt.databinding.DialogCreateProjectBinding

class CreateProjectBottomSheet(
    private val onProjectCreated: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogCreateProjectBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateProjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCreateProject.setOnClickListener {
            val key = binding.etProjectKey.text?.toString()?.trim() ?: ""
            val name = binding.etProjectName.text?.toString()?.trim() ?: ""
            val desc = binding.etProjectDesc.text?.toString()?.trim() ?: ""
            val lead = binding.etProjectLead.text?.toString()?.trim() ?: "Matheus"

            if (key.isBlank() || name.isBlank()) {
                Toast.makeText(requireContext(), "Informe a Chave e o Nome do Projeto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val repo = SprinttApp.instance.ticketRepository
            if (repo.getProjectByKey(key) != null) {
                Toast.makeText(requireContext(), "Já existe um projeto com a chave $key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            repo.createProject(key, name, desc, lead)
            Toast.makeText(requireContext(), "Projeto $key criado com sucesso!", Toast.LENGTH_SHORT).show()
            onProjectCreated()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
