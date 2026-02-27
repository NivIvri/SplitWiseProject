package com.example.splitwise_project.feature.groups.expense_details

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.splitwise_project.R
import com.example.splitwise_project.databinding.FragmentExpenseDetailsBinding
import java.util.Locale

/** Shows complete details for one expense. */
class ExpenseDetailsFragment : Fragment(R.layout.fragment_expense_details) {

    private var _binding: FragmentExpenseDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ExpenseDetailsViewModel
    private lateinit var splitAdapter: SplitDetailsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentExpenseDetailsBinding.bind(view)
        viewModel = ViewModelProvider(this)[ExpenseDetailsViewModel::class.java]

        splitAdapter = SplitDetailsAdapter()
        binding.rvSplitDetails.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSplitDetails.adapter = splitAdapter

        viewModel.expense.observe(viewLifecycleOwner) { expense ->
            binding.tvTitle.text = expense?.description.orEmpty()
            val cents = expense?.amountCents ?: 0L
            val currency = expense?.currency ?: "ILS"
            binding.tvAmount.text = String.format(Locale.getDefault(), "%.2f %s", cents / 100.0, currency)
        }
        viewModel.paidByLabel.observe(viewLifecycleOwner) { paidBy ->
            binding.tvPaidBy.text = if (paidBy.isBlank()) "" else "Paid by: $paidBy"
        }
        viewModel.splitRows.observe(viewLifecycleOwner) { rows ->
            splitAdapter.submitList(rows)
        }
        viewModel.uiMessage.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        val groupId = arguments?.getString("groupId").orEmpty()
        val expenseId = arguments?.getString("expenseId").orEmpty()
        if (groupId.isBlank() || expenseId.isBlank()) {
            Toast.makeText(requireContext(), "Missing expense details args", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.start(groupId, expenseId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
