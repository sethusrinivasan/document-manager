package com.app.traveldocs.data.drive

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides an authenticated Google Drive service using the currently signed-in Google account.
 * Returns null if no Google account is signed in.
 */
@Singleton
class GoogleDriveServiceProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : DriveServiceProvider {

    override fun getDriveService(): Drive? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_READONLY)
        )
        credential.selectedAccount = account.account ?: return null

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("TravelDocs")
            .build()
    }
}
