package com.example.splitwise_project.feature.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.splitwise_project.R
import com.example.splitwise_project.databinding.FragmentActivityBinding
import com.example.splitwise_project.feature.groups.details.ActivityAdapter

/** Separate screen for timeline across all groups. */
class ActivityFragment : Fragment(R.layout.fragment_activity) {

    private var _binding: FragmentActivityBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ActivityViewModel
    private lateinit var adapter: ActivityAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentActivityBinding.bind(view)
        viewModel = ViewModelProvider(this)[ActivityViewModel::class.java]

        adapter = ActivityAdapter()
        binding.rvActivity.layoutManager = LinearLayoutManager(requireContext())
        binding.rvActivity.adapter = adapter

        viewModel.activities.observe(viewLifecycleOwner) { adapter.submitList(it) }
        viewModel.uidToUsername.observe(viewLifecycleOwner) { map ->
            adapter.setUidToUsername(map)
        }
        viewModel.uiMessage.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
