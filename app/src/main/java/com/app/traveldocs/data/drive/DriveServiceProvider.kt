package com.app.traveldocs.data.drive

import com.google.api.services.drive.Drive

/**
 * Provides an authenticated Google Drive service instance.
 * Returns null if the user is not authenticated or credentials are invalid.
 */
interface DriveServiceProvider {
    /**
     * Returns an authenticated Drive service, or null if authentication is unavailable.
     */
    fun getDriveService(): Drive?
}
