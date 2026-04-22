package com.vishnu.trim

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthManager(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(context)

    // Using the Web Client ID from the latest google-services.json
    private val webClientId = "752034207667-6fhg1lris7pdvif72gsrsg3s7e1sgmud.apps.googleusercontent.com"

    private fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }

    suspend fun signInWithGoogle(): Boolean {
        val activity = findActivity(context) ?: return false
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            return handleSignInResult(result)
        } catch (e: GetCredentialException) {
            Log.e("AuthManager", "Credential Error: ${e.message}")
            return false
        } catch (e: Exception) {
            Log.e("AuthManager", "Auth Error: ${e.message}")
            return false
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Boolean {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            true
        } catch (e: Exception) {
            Log.e("AuthManager", "Email Sign-In Error: ${e.message}")
            false
        }
    }

    private suspend fun handleSignInResult(result: GetCredentialResponse): Boolean {
        val credential = result.credential
        if (credential is GoogleIdTokenCredential) {
            val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
            return try {
                auth.signInWithCredential(firebaseCredential).await()
                true
            } catch (e: Exception) {
                Log.e("AuthManager", "Firebase Sign-In Error: ${e.message}")
                false
            }
        }
        return false
    }

    fun isUserSignedIn(): Boolean = auth.currentUser != null
}
