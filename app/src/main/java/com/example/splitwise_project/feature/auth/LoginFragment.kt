package com.example.splitwise_project.feature.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.splitwise_project.R
import com.google.firebase.auth.FirebaseAuth

/** Legacy login fragment kept for navigation compatibility; app now uses LoginActivity. */
class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser != null) {
            findNavController().navigate(R.id.homeFragment)
        } else {
            startActivity(
                Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            requireActivity().finish()
            Toast.makeText(requireContext(), "Please login to continue.", Toast.LENGTH_SHORT).show()
        }
    }
}
