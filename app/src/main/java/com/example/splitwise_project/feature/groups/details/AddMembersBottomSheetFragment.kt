package com.example.splitwise_project.feature.groups.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.splitwise_project.databinding.BottomSheetAddMembersBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Bottom sheet for selecting multiple existing users and adding them to a group. */
class AddMembersBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddMembersBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: GroupDetailsViewModel
    private lateinit var adapter: SelectableUsersAdapter
    private val selected = mutableSetOf<String>()
    private val existingMembers = mutableSetOf<String>()
    private var allUsersCache = emptyList<com.example.splitwise_project.data.model.User>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireParentFragment())[GroupDetailsViewModel::class.java]

        adapter = SelectableUsersAdapter { uid, isSelected ->
            if (isSelected) selected.add(uid) else selected.remove(uid)
        }
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter

        viewModel.allUsers.observe(viewLifecycleOwner) { users ->
            allUsersCache = users
            submitFiltered(users, binding.etSearch.text?.toString().orEmpty())
        }
        viewModel.memberUids.observe(viewLifecycleOwner) { uids ->
            existingMembers.clear()
            existingMembers.addAll(uids)
            // Existing members must be pre-selected when opening this sheet.
            selected.addAll(uids)
            adapter.setExistingUids(existingMembers)
            adapter.setSelectedUids(selected)
        }
        viewModel.uiMessage.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        binding.etSearch.doAfterTextChanged { editable ->
            submitFiltered(allUsersCache, editable?.toString().orEmpty())
        }

        binding.btnSave.setOnClickListener {
            val groupId = arguments?.getString(ARG_GROUP_ID).orEmpty()
            val toAdd = selected.filterNot { existingMembers.contains(it) }
            if (toAdd.isEmpty()) {
                Toast.makeText(requireContext(), "Selected users are already members.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.addMembersToGroup(groupId, toAdd)
            dismiss()
        }
    }

    private fun submitFiltered(users: List<com.example.splitwise_project.data.model.User>, query: String) {
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) {
            users
        } else {
            users.filter { user ->
                user.username.lowercase().contains(q) ||
                    user.displayName.lowercase().contains(q) ||
                    user.email.lowercase().contains(q)
            }
        }
        adapter.submitList(filtered)
        adapter.setExistingUids(existingMembers)
        adapter.setSelectedUids(selected)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_GROUP_ID = "groupId"

        fun newInstance(groupId: String): AddMembersBottomSheetFragment {
            val fragment = AddMembersBottomSheetFragment()
            fragment.arguments = Bundle().apply { putString(ARG_GROUP_ID, groupId) }
            return fragment
        }
    }
}
