package io.github.matheuscruzsouza.sprintt.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import io.github.matheuscruzsouza.sprintt.databinding.ActivityMainBinding
import io.github.matheuscruzsouza.sprintt.ui.dialog.CreateProjectBottomSheet
import io.github.matheuscruzsouza.sprintt.ui.dialog.CreateUserBottomSheet
import io.github.matheuscruzsouza.sprintt.ui.fragment.AnalyticsFragment
import io.github.matheuscruzsouza.sprintt.ui.fragment.DashboardFragment
import io.github.matheuscruzsouza.sprintt.ui.fragment.ProjectListFragment
import io.github.matheuscruzsouza.sprintt.ui.fragment.UserListFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentTabPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.subtitle = "mDNS: https://sprintt.local:8443/projetos"

        setupTabs()
        setupFab()

        switchTab(0)
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Dashboard"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Projetos"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Equipe"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Analíticos"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { switchTab(it) }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                tab?.position?.let { switchTab(it) }
            }
        })
    }

    private fun switchTab(position: Int) {
        currentTabPosition = position
        val fragment = when (position) {
            1 -> ProjectListFragment.newInstance()
            2 -> UserListFragment.newInstance()
            3 -> AnalyticsFragment.newInstance()
            else -> DashboardFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            .commit()

        when (position) {
            0, 3 -> binding.fabAction.visibility = View.GONE
            1 -> {
                binding.fabAction.visibility = View.VISIBLE
                binding.fabAction.contentDescription = "Criar Projeto"
            }
            2 -> {
                binding.fabAction.visibility = View.VISIBLE
                binding.fabAction.contentDescription = "Adicionar Membro"
            }
        }
    }

    private fun setupFab() {
        binding.fabAction.setOnClickListener {
            when (currentTabPosition) {
                1 -> {
                    val dialog = CreateProjectBottomSheet {
                        val currentFragment = supportFragmentManager.findFragmentById(binding.fragmentContainer.id)
                        if (currentFragment is ProjectListFragment) {
                            currentFragment.refreshData()
                        }
                    }
                    dialog.show(supportFragmentManager, "CreateProjectBottomSheet")
                }
                2 -> {
                    val dialog = CreateUserBottomSheet {
                        val currentFragment = supportFragmentManager.findFragmentById(binding.fragmentContainer.id)
                        if (currentFragment is UserListFragment) {
                            currentFragment.refreshData()
                        }
                    }
                    dialog.show(supportFragmentManager, "CreateUserBottomSheet")
                }
            }
        }
    }
}
