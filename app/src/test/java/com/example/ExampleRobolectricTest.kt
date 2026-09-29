package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.VaultBackupCrypto
import com.example.data.security.VaultSecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Secret Vault", appName)
  }

  @Test
  fun `inactivity timeout default and update`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val securityManager = VaultSecurityManager(context)
    
    // Default should be 60 seconds
    assertEquals(60, securityManager.getInactivityTimeoutSeconds())
    
    // Update to 30 seconds
    securityManager.setInactivityTimeoutSeconds(30)
    assertEquals(30, securityManager.getInactivityTimeoutSeconds())

    // Update to disabled (0)
    securityManager.setInactivityTimeoutSeconds(0)
    assertEquals(0, securityManager.getInactivityTimeoutSeconds())
  }

  @Test
  fun `vault backup encryption roundtrip and auth failure`() {
    val secretPayload = "SecretVaultBackupData-Confidential-2026".toByteArray(Charsets.UTF_8)
    val passphrase = "MasterPin#1234Secure"

    // Encrypt
    val encrypted = VaultBackupCrypto.encrypt(secretPayload, passphrase)
    assertTrue(encrypted.size > secretPayload.size)

    // Decrypt with correct passphrase
    val decrypted = VaultBackupCrypto.decrypt(encrypted, passphrase)
    assertEquals(String(secretPayload, Charsets.UTF_8), String(decrypted, Charsets.UTF_8))

    // Decrypt with wrong passphrase should throw exception
    assertThrows(Exception::class.java) {
      VaultBackupCrypto.decrypt(encrypted, "WrongPassword#9999")
    }
  }
}
