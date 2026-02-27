package com.example.splitwise_project.feature.groups.details

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.splitwise_project.R
import com.example.splitwise_project.data.model.Expense
import com.example.splitwise_project.data.model.Settlement
import com.example.splitwise_project.data.repository.AuthRepository
import com.example.splitwise_project.databinding.BottomSheetExpenseCategoriesBinding
import com.example.splitwise_project.databinding.FragmentGroupDetailsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

/** Group details screen: members, expenses list, add-member/add-expense dialogs. */
class GroupDetailsFragment : Fragment(R.layout.fragment_group_details) {

    private var _binding: FragmentGroupDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: GroupDetailsViewModel
    private lateinit var expensesAdapter: ExpensesAdapter
    private lateinit var summaryAdapter: SummaryBreakdownAdapter
    private var currentGroupId: String? = null
    private val currentUid: String? get() = AuthRepository().getCurrentUser()?.uid
    private var latestSettlements: List<Settlement> = emptyList()
    private var latestUidToName: Map<String, String> = emptyMap()
    private var showAllSummaryRows: Boolean = false
    private var balanceLoading: Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGroupDetailsBinding.bind(view)

        viewModel = ViewModelProvider(this)[GroupDetailsViewModel::class.java]

        expensesAdapter = ExpensesAdapter { expense -> onExpenseClick(expense) }
        expensesAdapter.setCurrentUid(currentUid)
        binding.rvExpenses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvExpenses.adapter = expensesAdapter
        summaryAdapter = SummaryBreakdownAdapter()
        binding.rvSummaryBreakdown.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSummaryBreakdown.adapter = summaryAdapter

        viewModel.uiMessage.observe(viewLifecycleOwner) { msg ->
            showToast(msg)
        }

        viewModel.group.observe(viewLifecycleOwner) { group ->
            // Keep the top area compact: show group name in the toolbar title.
            binding.toolbarGroupDetails.title = group?.name.orEmpty()
        }

        viewModel.memberUids.observe(viewLifecycleOwner) { uids ->
            binding.chipMembersCount.text = "${uids.size} member${if (uids.size != 1) "s" else ""}"
        }
        viewModel.uidToName.observe(viewLifecycleOwner) { map ->
            latestUidToName = map
            expensesAdapter.setUidToName(map)
            renderSummaryBreakdown()
        }
        viewModel.isBalanceLoading.observe(viewLifecycleOwner) { loading ->
            balanceLoading = loading
            renderSummaryBreakdown()
        }
        viewModel.settlements.observe(viewLifecycleOwner) { settlements ->
            latestSettlements = settlements
            renderSummaryBreakdown()
        }

        viewModel.expenses.observe(viewLifecycleOwner) { expenses ->
            expensesAdapter.submitList(expenses)
        }

        binding.btnAddMember.setOnClickListener {
            val groupId = currentGroupId.orEmpty()
            if (groupId.isBlank()) {
                showToast("Group not loaded")
                return@setOnClickListener
            }
            AddMembersBottomSheetFragment.newInstance(groupId)
                .show(childFragmentManager, "AddMembersBottomSheet")
        }
        binding.chipMembersCount.setOnClickListener {
            val groupId = currentGroupId.orEmpty()
            if (groupId.isBlank()) {
                showToast("Group not loaded")
                return@setOnClickListener
            }
            GroupMembersBottomSheetFragment.newInstance()
                .show(childFragmentManager, "GroupMembersBottomSheet")
        }
        binding.fabAddExpense.setOnClickListener { showAddExpenseDialog() }
        binding.toolbarGroupDetails.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.tvViewAllSummary.setOnClickListener {
            showAllSummaryRows = !showAllSummaryRows
            renderSummaryBreakdown()
        }

        val groupId = arguments?.getString("groupId")
        if (groupId.isNullOrBlank()) {
            showToast("Missing groupId")
            return
        }
        currentGroupId = groupId
        viewModel.start(groupId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showAddExpenseDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_expense, null)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDescription)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val rowCategory = dialogView.findViewById<LinearLayout>(R.id.rowCategory)
        val ivCategoryIcon = dialogView.findViewById<ImageView>(R.id.ivCategoryIcon)
        val tvCategoryValue = dialogView.findViewById<TextView>(R.id.tvCategoryValue)
        var selectedCategory = ExpenseCategory.OTHER

        fun renderCategory() {
            ivCategoryIcon.setImageResource(ExpenseCategory.iconFor(selectedCategory))
            tvCategoryValue.text = ExpenseCategory.labelFor(selectedCategory)
        }
        renderCategory()

        rowCategory.setOnClickListener {
            showCategoryPickerBottomSheet(
                selectedCategory = selectedCategory,
                onSelected = { newCategory ->
                    selectedCategory = newCategory
                    renderCategory()
                }
            )
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Expense")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val desc = etDesc.text.toString()
                val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
                showSplitMembersDialog(desc, amount, selectedCategory)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSplitMembersDialog(description: String, amount: Double, category: String) {
        val members = viewModel.memberUsers.value.orEmpty()
        if (members.isEmpty()) {
            showToast("No members available for split.")
            return
        }

        val labels = members.map { it.username.ifBlank { it.displayName.ifBlank { it.email } } }.toTypedArray()
        val checked = BooleanArray(members.size) { true }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Split between members")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("Save") { _, _ ->
                val selectedUids = members
                    .mapIndexedNotNull { index, user -> if (checked[index]) user.uid else null }
                viewModel.addExpense(description, amount, selectedUids, category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    private fun renderSummaryBreakdown() {
        if (balanceLoading) {
            summaryAdapter.submitList(emptyList())
            binding.tvSummaryBreakdown.text = "Loading balances..."
            binding.tvViewAllSummary.visibility = View.GONE
            return
        }
        val me = currentUid ?: return
        val perUserNet = mutableMapOf<String, Long>()

        latestSettlements.forEach { s ->
            when {
                s.fromUid == me -> {
                    perUserNet[s.toUid] = (perUserNet[s.toUid] ?: 0L) - s.amountCents
                }
                s.toUid == me -> {
                    perUserNet[s.fromUid] = (perUserNet[s.fromUid] ?: 0L) + s.amountCents
                }
            }
        }

        val rows = perUserNet
            .filterValues { it != 0L }
            .map { (otherUid, net) ->
                SummaryBreakdownAdapter.Row(
                    otherUid = otherUid,
                    otherName = latestUidToName[otherUid] ?: "Unknown user",
                    netCentsFromMe = net
                )
            }
            .sortedByDescending { kotlin.math.abs(it.netCentsFromMe) }

        val limitedRows = if (!showAllSummaryRows) rows.take(3) else rows
        summaryAdapter.submitList(limitedRows)

        binding.tvSummaryBreakdown.text = if (rows.isEmpty()) {
            "No balances yet"
        } else {
            "${rows.size} open balance relation${if (rows.size == 1) "" else "s"}"
        }
        binding.tvViewAllSummary.visibility = if (rows.size > 3) View.VISIBLE else View.GONE
        binding.tvViewAllSummary.text = if (showAllSummaryRows) "Show less" else "View all"
    }

    private fun onExpenseClick(expense: Expense) {
        val uidToName = viewModel.uidToName.value.orEmpty()
        val paidBy = uidToName[expense.paidByUid] ?: "Unknown user"
        val splitLines = expense.splits.entries
            .sortedByDescending { it.value }
            .joinToString(separator = "\n") { (uid, cents) ->
                val name = uidToName[uid] ?: "Unknown user"
                val amount = String.format(Locale.getDefault(), "%.2f %s", cents / 100.0, expense.currency)
                "• $name: $amount"
            }
        val total = String.format(Locale.getDefault(), "%.2f %s", expense.amountCents / 100.0, expense.currency)
        val message = buildString {
            appendLine("Amount: $total")
            appendLine("Category: ${ExpenseCategory.labelFor(expense.category)}")
            appendLine("Paid by: $paidBy")
            appendLine()
            appendLine("Participants:")
            append(splitLines.ifBlank { "No participants" })
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(expense.description.ifBlank { "Expense details" })
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showCategoryPickerBottomSheet(
        selectedCategory: String,
        onSelected: (String) -> Unit
    ) {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetExpenseCategoriesBinding.inflate(layoutInflater)
        val adapter = ExpenseCategoryGridAdapter { option ->
            onSelected(option.key)
            dialog.dismiss()
        }
        sheetBinding.rvCategoryOptions.layoutManager = GridLayoutManager(requireContext(), 3)
        sheetBinding.rvCategoryOptions.adapter = adapter
        adapter.submitList(
            ExpenseCategory.options,
            ExpenseCategory.normalize(selectedCategory)
        )
        dialog.setContentView(sheetBinding.root)
        dialog.show()
    }
}
