package io.github.matheuscruzsouza.sprintt.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.matheuscruzsouza.sprintt.SprinttApp
import io.github.matheuscruzsouza.sprintt.databinding.DialogCreateUserBinding

class CreateUserBottomSheet(
    private val onUserCreated: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogCreateUserBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCreateUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnCreateUser.setOnClickListener {
            val username = binding.etUsername.text?.toString()?.trim() ?: ""
            val fullName = binding.etFullName.text?.toString()?.trim() ?: ""
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val role = binding.etRole.text?.toString()?.trim() ?: "Developer"

            if (username.isBlank() || fullName.isBlank() || email.isBlank()) {
                Toast.makeText(requireContext(), "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val repo = SprinttApp.instance.ticketRepository
            if (repo.getAllUsers().any { it.username.equals(username, ignoreCase = true) }) {
                Toast.makeText(requireContext(), "Usuário @$username já existe", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            repo.createUser(username, fullName, email, role)
            Toast.makeText(requireContext(), "Membro @$username cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
            onUserCreated()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
