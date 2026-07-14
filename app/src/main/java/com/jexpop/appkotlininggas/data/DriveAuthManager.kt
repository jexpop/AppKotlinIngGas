package com.jexpop.appkotlininggas.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.services.drive.DriveScopes

object DriveAuthManager {

    const val REQUEST_CODE_SIGN_IN = 9001

    fun getSignInIntent(context: Context): Intent {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE))
            .build()
        return GoogleSignIn.getClient(context, options).signInIntent
    }

    fun isSignedIn(context: Context): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && GoogleSignIn.hasPermissions(
            account,
            com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE)
        )
    }

    fun getSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun getSignedInEmail(context: Context): String? {
        return getSignedInAccount(context)?.email
    }

    fun isAuthorizedAccount(context: Context, allowedEmail: String): Boolean {
        val signedInEmail = getSignedInEmail(context)?.trim()?.lowercase() ?: return false
        return signedInEmail == allowedEmail.trim().lowercase()
    }

    fun signOut(context: Context) {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE))
            .build()
        GoogleSignIn.getClient(context, options).signOut()
    }
}
