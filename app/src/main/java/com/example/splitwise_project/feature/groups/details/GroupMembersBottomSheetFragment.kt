package com.example.splitwise_project.feature.groups.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.splitwise_project.databinding.BottomSheetGroupMembersBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Bottom sheet that shows current group members in a compact list. */
class GroupMembersBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetGroupMembersBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: GroupDetailsViewModel
    private lateinit var membersAdapter: MemberListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetGroupMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Reuse parent fragment ViewModel so this sheet reflects live member changes.
        viewModel = ViewModelProvider(requireParentFragment())[GroupDetailsViewModel::class.java]

        membersAdapter = MemberListAdapter()
        binding.rvMembers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMembers.adapter = membersAdapter

        viewModel.memberUsers.observe(viewLifecycleOwner) { users ->
            membersAdapter.submitList(users)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): GroupMembersBottomSheetFragment {
            return GroupMembersBottomSheetFragment()
        }
    }
}
