package com.example.smarthealth

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var languageSettingButton: Button
    private lateinit var notificationSettingButton: Button
    private lateinit var privacySettingButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize buttons
        languageSettingButton = findViewById(R.id.language_setting)
        notificationSettingButton = findViewById(R.id.notification_setting)
        privacySettingButton = findViewById(R.id.privacy_setting)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("app_preferences", MODE_PRIVATE)

        // Set up click listeners for each setting option
        languageSettingButton.setOnClickListener {
            showLanguageDialog()
        }

        notificationSettingButton.setOnClickListener {
            // Navigate to Notification settings
            Toast.makeText(this, "Notification settings clicked", Toast.LENGTH_SHORT).show()
        }

        privacySettingButton.setOnClickListener {
            // Navigate to Privacy settings
            Toast.makeText(this, "Privacy settings clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Zulu", "Afrikaans")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Choose Language")
        builder.setItems(languages) { _, which ->
            when (which) {
                0 -> setLocale("en") // English
                1 -> setLocale("zu") // Zulu
                2 -> setLocale("af") // Afrikaans
            }
        }
        builder.show()
    }

    private fun setLocale(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Save the selected language in SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putString("language", lang)
        editor.apply()

        // Restart the activity to apply changes
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        finish()
    }
}
