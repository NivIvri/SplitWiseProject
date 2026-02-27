package com.example.splitwise_project.core.navigation

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.splitwise_project.R
import com.example.splitwise_project.databinding.ActivityMainBinding
import com.example.splitwise_project.feature.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(
                Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Wire bottom navigation with top-level destinations.
        binding.bottomNav.setupWithNavController(navController)

        // Hide bottom nav on auth/details screens, show on Groups/Activity/Profile.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showBottomNav = destination.id == R.id.homeFragment ||
                destination.id == R.id.activityFragment ||
                destination.id == R.id.profileFragment
            binding.bottomNav.visibility = if (showBottomNav) View.VISIBLE else View.GONE
        }
    }

    // Optional: only needed if you show an "Up" button in a Toolbar/ActionBar.
    // If you do not use an ActionBar/Toolbar, you can remove this override entirely.
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
