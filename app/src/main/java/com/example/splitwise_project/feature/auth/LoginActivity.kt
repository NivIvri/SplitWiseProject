package com.example.splitwise_project.feature.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.splitwise_project.core.navigation.MainActivity
import com.example.splitwise_project.data.model.User
import com.example.splitwise_project.data.repository.UserRepository
import com.example.splitwise_project.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

/** Custom login/register screen using FirebaseAuth directly. */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (auth.currentUser != null) {
            openMainAndFinish()
            return
        }

        binding.btnLogin.setOnClickListener { signIn() }
        binding.btnRegister.setOnClickListener { register() }
    }

    private fun signIn() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        if (email.isBlank() || password.isBlank()) {
            showToast("Email and password are required.")
            return
        }
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                auth.currentUser?.let { saveUserProfile(it.uid, email) }
                openMainAndFinish()
            }
            .addOnFailureListener { e ->
                showToast(e.message ?: "Login failed.")
            }
    }

    private fun register() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        if (email.isBlank() || password.isBlank()) {
            showToast("Email and password are required.")
            return
        }
        if (password.length < 6) {
            showToast("Password must be at least 6 characters.")
            return
        }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid.orEmpty()
                if (uid.isBlank()) {
                    showToast("Registration failed.")
                    return@addOnSuccessListener
                }
                saveUserProfile(uid, email)
                openMainAndFinish()
            }
            .addOnFailureListener { e ->
                showToast(e.message ?: "Registration failed.")
            }
    }

    private fun saveUserProfile(uid: String, email: String) {
        val username = email.substringBefore("@").ifBlank { "user_${uid.take(6)}" }
        val user = User(
            uid = uid,
            email = email.lowercase(),
            username = username,
            displayName = username,
            createdAt = System.currentTimeMillis()
        )
        userRepository.saveUser(user) { /* best-effort profile sync */ }
    }

    private fun openMainAndFinish() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
