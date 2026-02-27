package com.example.splitwise_project.feature.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.splitwise_project.R
import com.example.splitwise_project.databinding.FragmentRegisterBinding

/** Registration fragment for creating new accounts. */
class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AuthViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRegisterBinding.bind(view)

        viewModel = ViewModelProvider(this)[AuthViewModel::class.java]

        viewModel.onMessage = { msg -> showToast(msg) }
        // After successful registration/login, return to the previous screen (e.g., Home).
        viewModel.onAuthSuccess = {
            findNavController().navigateUp()
        }

        binding.btnRegister.setOnClickListener {
            val displayName = binding.etDisplayName.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            viewModel.register(email, password, displayName)
        }

        binding.tvLoginLink.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.onMessage = null
        viewModel.onAuthSuccess = null
        _binding = null
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
