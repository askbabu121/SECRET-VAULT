package com.example

import android.app.Application
import com.example.data.backup.GoogleDriveBackupService
import com.example.data.backup.VaultBackupPackageService
import com.example.data.database.VaultDatabase
import com.example.data.repository.VaultRepository
import com.example.data.security.VaultSecurityManager

class VaultApplication : Application() {

    lateinit var database: VaultDatabase
        private set

    lateinit var securityManager: VaultSecurityManager
        private set

    lateinit var repository: VaultRepository
        private set

    lateinit var backupPackageService: VaultBackupPackageService
        private set

    lateinit var googleDriveBackupService: GoogleDriveBackupService
        private set

    override fun onCreate() {
        super.onCreate()
        database = VaultDatabase.getDatabase(this)
        securityManager = VaultSecurityManager(this)
        repository = VaultRepository(this, database.vaultDao())
        backupPackageService = VaultBackupPackageService(this, database.vaultDao())
        googleDriveBackupService = GoogleDriveBackupService(this)
    }
}
