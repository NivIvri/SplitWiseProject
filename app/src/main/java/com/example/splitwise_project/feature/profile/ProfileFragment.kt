package com.example.splitwise_project.feature.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.splitwise_project.R
import com.example.splitwise_project.feature.auth.LoginActivity
import com.example.splitwise_project.data.repository.AuthRepository
import com.example.splitwise_project.databinding.FragmentProfileBinding
import java.util.Locale

/** Profile summary screen (spent/received/net + monthly spent). */
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel
    private lateinit var monthlyAdapter: MonthlySpentAdapter
    private val authRepository = AuthRepository()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        monthlyAdapter = MonthlySpentAdapter()
        binding.rvMonthly.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMonthly.adapter = monthlyAdapter

        val currentUser = authRepository.getCurrentUser()
        val email = currentUser?.email.orEmpty()
        binding.tvProfileEmail.text = email.ifBlank { "No email" }
        binding.tvProfileName.text = email.substringBefore("@")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            .ifBlank { "User" }

        viewModel.totals.observe(viewLifecycleOwner) { totals ->
            binding.tvTotalSpent.text = formatMoney(totals.totalIOweCents)
            binding.tvTotalReceived.text = formatMoney(totals.totalOwedToMeCents)
            binding.tvNetBalance.text = formatMoney(totals.netBalanceCents)
        }
        viewModel.monthly.observe(viewLifecycleOwner) { monthly ->
            monthlyAdapter.submitList(monthly)
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressProfile.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        viewModel.stateText.observe(viewLifecycleOwner) { state ->
            val hasText = !state.isNullOrBlank()
            binding.tvProfileState.visibility = if (hasText) View.VISIBLE else View.GONE
            binding.tvProfileState.text = state.orEmpty()
        }
        viewModel.uiMessage.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            authRepository.logout()
            startActivity(
                Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            requireActivity().finish()
        }

        viewModel.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun formatMoney(cents: Long): String {
        return String.format(Locale.getDefault(), "%.2f ILS", cents / 100.0)
    }
}
