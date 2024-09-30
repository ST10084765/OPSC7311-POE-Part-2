package com.example.smarthealth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.biometric.BiometricManager
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Base64
import java.security.MessageDigest
import com.facebook.AccessToken
import com.facebook.GraphRequest



class LoginActivity : AppCompatActivity() {

    private lateinit var callbackManager: CallbackManager
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        printKeyHash()
        printSHA1Fingerprint()

        // Initialize Facebook SDK
        callbackManager = CallbackManager.Factory.create()

        // Set up Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Set up Google Sign-In button
        findViewById<View>(R.id.google_sign_in_button).setOnClickListener {
            signInWithGoogle()
        }

        // Set up Facebook Login button
        val loginButton = findViewById<LoginButton>(R.id.facebook_login_button)
        loginButton.setPermissions("email", "public_profile")
        loginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(loginResult: LoginResult) {
                // Handle successful Facebook login
                Log.d("LoginActivity", "Facebook token: ${loginResult.accessToken.token}")

                // Call fetchUserData with the AccessToken
                fetchUserData(loginResult.accessToken)

                // Call your onLoginSuccess method to handle further navigation
                onLoginSuccess()
            }

            override fun onCancel() {
                // Handle Facebook login cancellation
                Log.d("LoginActivity", "Facebook login cancelled")
                onLoginFailure()
            }

            override fun onError(error: FacebookException) {
                // Handle Facebook login error
                Log.d("LoginActivity", "Facebook login failed", error)
                onLoginFailure()
            }
        })
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleGoogleSignInResult(task)
        }
    }

    private fun handleGoogleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            onLoginSuccess(account)
        } catch (e: ApiException) {
            Log.w("LoginActivity", "Google sign-in failed: ${e.statusCode}")
            onLoginFailure()
        }
    }

    private fun onLoginSuccess(account: GoogleSignInAccount? = null) {
        if (isBiometricEnrolled()) {
            showBiometricPrompt()
        } else {
            Log.d("LoginActivity", "No biometric enrollment found, proceeding to main activity")
            navigateToHomeActivity()
        }
    }

    private fun onLoginFailure() {
        // Handle login failure
        Log.d("LoginActivity", "Login failed or cancelled")
        // Provide feedback to the user or allow retry
    }

    private fun isBiometricEnrolled(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return when (biometricManager.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // Biometric hardware is available and enrolled
                true
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                // No biometric hardware available
                Log.d("LoginActivity", "Biometric hardware not available")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                // Biometric hardware is unavailable
                Log.d("LoginActivity", "Biometric hardware unavailable")
                false
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // No biometric credentials are enrolled
                Log.d("LoginActivity", "No biometric credentials enrolled")
                false
            }
            else -> {
                // Unknown error
                Log.d("LoginActivity", "Unknown biometric error")
                false
            }
        }
    }

    private fun showBiometricPrompt() {
        val biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.d("LoginActivity", "Biometric authentication succeeded")
                    navigateToHomeActivity()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.d("LoginActivity", "Biometric authentication failed")
                    // Handle authentication failure
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.d("LoginActivity", "Authentication error: $errString")
                    // Handle errors
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Login")
            .setSubtitle("Log in using your biometric credentials")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun navigateToHomeActivity() {
        //to transition to home page
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun printKeyHash() {
        try {
            val info: PackageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hash: String = Base64.encodeToString(md.digest(), Base64.NO_WRAP) // Changed to NO_WRAP to avoid new lines
                Log.d("KeyHash", "KeyHash: $hash")
            }
        } catch (e: Exception) {
            Log.e("KeyHash", "Failed to get key hash", e)
        }
    }

    private fun printSHA1Fingerprint() {
        try {
            val info: PackageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA-1")
                md.update(signature.toByteArray())
                val sha1: String = md.digest().joinToString(":") { "%02x".format(it) }
                Log.d("SHA1Fingerprint", "SHA1 Fingerprint: $sha1")
            }
        } catch (e: Exception) {
            Log.e("SHA1Fingerprint", "Failed to get SHA1 fingerprint", e)
        }
    }

    private fun fetchUserData(accessToken: AccessToken) {
        val request = GraphRequest.newMeRequest(
            accessToken
        ) { jsonObject, response ->
            // Handle the response
            try {
                val userId = jsonObject?.getString("id") // This is the user ID
                val userName = jsonObject?.getString("name") // Optional: User's name
                val userEmail = jsonObject?.getString("email") // Optional: User's email

                // Log the user ID, access token, and any other data
                Log.d("FacebookLogin", "User ID: $userId")
                Log.d("FacebookLogin", "Access Token: ${accessToken.token}")
                Log.d("FacebookLogin", "User Name: $userName")
                Log.d("FacebookLogin", "User Email: $userEmail")
            } catch (e: Exception) {
                Log.e("FacebookLogin", "Failed to retrieve user data", e)
            }
        }

        val parameters = Bundle()
        parameters.putString("fields", "id,name,email") // Specify fields you need
        request.parameters = parameters
        request.executeAsync()
    }

}
