package com.example.splitwise_project.feature.groups.home

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.splitwise_project.R
import com.example.splitwise_project.feature.auth.LoginActivity
import com.example.splitwise_project.data.model.Group
import com.example.splitwise_project.databinding.FragmentHomeBinding
import com.example.splitwise_project.data.repository.AuthRepository
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** XML-based home screen fragment; inflates fragment_home layout. */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: GroupAdapter
    private var latestGroups: List<Group> = emptyList()
    private var latestIsLoading: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable options menu for login/logout actions.
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHomeBinding.bind(view)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        adapter = GroupAdapter { group -> onGroupClick(group) }
        binding.rvGroups.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGroups.adapter = adapter

        viewModel.onMessage = { msg -> showToast(msg) }

        viewModel.groups.observe(viewLifecycleOwner) { groups ->
            latestGroups = groups
            adapter.submitList(groups)
            renderState()
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            latestIsLoading = isLoading
            renderState()
        }

        binding.fabCreateGroup.setOnClickListener { showCreateGroupDialog() }
        binding.btnCreateFirstGroup.setOnClickListener { showCreateGroupDialog() }

        viewModel.start()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.home_menu, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val item = menu.findItem(R.id.menuLoginLogout)
        val isLoggedIn = AuthRepository().getCurrentUser() != null
        item.title = if (isLoggedIn) getString(R.string.logout) else getString(R.string.login)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menuProfile -> {
                findNavController().navigate(R.id.profileFragment)
                true
            }
            R.id.menuLoginLogout -> {
                val authRepository = AuthRepository()
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    startActivity(
                        Intent(requireContext(), LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    requireActivity().finish()
                } else {
                    authRepository.logout()
                    viewModel.stop()
                    showToast(getString(R.string.logged_out))
                    startActivity(
                        Intent(requireContext(), LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                    requireActivity().finish()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.onMessage = null
        _binding = null
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun renderState() {
        val showLoading = latestIsLoading
        val showContent = !latestIsLoading && latestGroups.isNotEmpty()
        val showEmpty = !latestIsLoading && latestGroups.isEmpty()

        binding.progressGroups.visibility = if (showLoading) View.VISIBLE else View.GONE
        binding.rvGroups.visibility = if (showContent) View.VISIBLE else View.GONE
        binding.emptyStateContainer.visibility = if (showEmpty) View.VISIBLE else View.GONE
    }

    /** Handle group item click; navigate to group details with a simple Bundle argument. */
    private fun onGroupClick(group: Group) {
        val bundle = android.os.Bundle().apply { putString("groupId", group.id) }
        findNavController().navigate(R.id.groupDetailsFragment, bundle)
    }

    /** Show AlertDialog with EditText for group name; delegate validation to ViewModel. */
    private fun showCreateGroupDialog() {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Group name"
        }

        // Important: keep the builder type stable, so Kotlin can resolve setTitle / setPositiveButton.
        val builder: AlertDialog.Builder = try {
            MaterialAlertDialogBuilder(requireContext())
        } catch (e: Exception) {
            AlertDialog.Builder(requireContext())
        }

        builder
            .setTitle("Create Group")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString()
                viewModel.createGroup(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
