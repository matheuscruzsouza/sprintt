package io.github.matheuscruzsouza.sprintt.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.matheuscruzsouza.sprintt.SprinttApp
import io.github.matheuscruzsouza.sprintt.databinding.FragmentUserListBinding
import io.github.matheuscruzsouza.sprintt.ui.adapter.UserAdapter

class UserListFragment : Fragment() {

    private var _binding: FragmentUserListBinding? = null
    private val binding get() = _binding!!
    private lateinit var userAdapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userAdapter = UserAdapter(
            onUserClick = { user ->
                val statusText = if (user.status == "PENDING_SETUP") {
                    "Status: Aguardando Primeiro Acesso\nCódigo: ${user.setupCode ?: "Não gerado"}"
                } else {
                    "Status: Ativo\nSenha já configurada."
                }
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("${user.fullName} (@${user.username})")
                    .setMessage("Cargo: ${user.role}\nE-mail: ${user.email}\n\n$statusText")
                    .setPositiveButton("OK", null)
                    .show()
            },
            onResetCodeClick = { user ->
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Forçar Novo Código?")
                    .setMessage("Deseja gerar um novo código de ativação de 6 dígitos para o usuário @${user.username}? A senha atual será invalidada até que ele defina uma nova com este código.")
                    .setPositiveButton("Gerar Código") { _, _ ->
                        val code = SprinttApp.instance.ticketRepository.generateSetupCode(user.id) ?: ""
                        refreshData()
                        androidx.appcompat.app.AlertDialog.Builder(requireContext())
                            .setTitle("Novo Código Gerado!")
                            .setMessage("🔑 CÓDIGO DE 6 DÍGITOS: $code\n\nEntregue este código ao usuário @${user.username} para que ele redefina sua senha.")
                            .setPositiveButton("Copiar") { _, _ ->
                                val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Código de 6 dígitos", code)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(requireContext(), "Código copiado: $code", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Fechar", null)
                            .show()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        )

        binding.recyclerViewUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewUsers.adapter = userAdapter

        refreshData()
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    fun refreshData() {
        val users = SprinttApp.instance.ticketRepository.getAllUsers()
        userAdapter.submitList(users)
        binding.tvEmptyUsers.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = UserListFragment()
    }
}
