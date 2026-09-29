package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

enum class PinVerificationResult {
    MASTER,
    DECOY,
    INCORRECT
}

class VaultSecurityManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vault_security_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_PIN_SET = "is_pin_set"
        private const val KEY_MASTER_PIN_HASH = "master_pin_hash"
        private const val KEY_DECOY_PIN_HASH = "decoy_pin_hash"
        private const val KEY_SECURITY_QUESTION = "security_question"
        private const val KEY_SECURITY_ANSWER_HASH = "security_answer_hash"
        private const val KEY_STEALTH_CALCULATOR_MODE = "stealth_calculator_mode"
        private const val KEY_PANIC_ENABLED = "panic_enabled"
        private const val KEY_AUTO_LOCK = "auto_lock"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_APP_ICON_DISGUISE = "app_icon_disguise"
        private const val KEY_INACTIVITY_TIMEOUT_SECONDS = "inactivity_timeout_seconds"
        private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
        private const val SALT = "VaultSecureSalt#2026_Storage_Private"
    }

    fun getLastBackupTime(): Long {
        return prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    fun setLastBackupTime(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_BACKUP_TIME, timestamp).apply()
    }

    fun getAppIconDisguise(): AppIconDisguise {
        val id = prefs.getString(KEY_APP_ICON_DISGUISE, AppIconDisguise.VAULT.id)
        return AppIconDisguise.fromId(id)
    }

    fun setAppIconDisguise(disguise: AppIconDisguise) {
        prefs.edit().putString(KEY_APP_ICON_DISGUISE, disguise.id).apply()
    }

    fun isPinConfigured(): Boolean {
        return prefs.getBoolean(KEY_IS_PIN_SET, false) &&
                !prefs.getString(KEY_MASTER_PIN_HASH, null).isNullOrEmpty()
    }

    fun setMasterPin(pin: String) {
        val hash = hashString(pin)
        prefs.edit()
            .putBoolean(KEY_IS_PIN_SET, true)
            .putString(KEY_MASTER_PIN_HASH, hash)
            .apply()
    }

    fun setDecoyPin(pin: String?) {
        val editor = prefs.edit()
        if (pin.isNullOrEmpty()) {
            editor.remove(KEY_DECOY_PIN_HASH)
        } else {
            editor.putString(KEY_DECOY_PIN_HASH, hashString(pin))
        }
        editor.apply()
    }

    fun hasDecoyPin(): Boolean {
        return !prefs.getString(KEY_DECOY_PIN_HASH, null).isNullOrEmpty()
    }

    fun verifyPin(pin: String): PinVerificationResult {
        val inputHash = hashString(pin)
        val masterHash = prefs.getString(KEY_MASTER_PIN_HASH, "") ?: ""
        val decoyHash = prefs.getString(KEY_DECOY_PIN_HASH, "") ?: ""

        return when {
            masterHash.isNotEmpty() && inputHash == masterHash -> PinVerificationResult.MASTER
            decoyHash.isNotEmpty() && inputHash == decoyHash -> PinVerificationResult.DECOY
            else -> PinVerificationResult.INCORRECT
        }
    }

    fun setSecurityQuestionAndAnswer(question: String, answer: String) {
        val cleanAnswer = answer.trim().lowercase()
        prefs.edit()
            .putString(KEY_SECURITY_QUESTION, question)
            .putString(KEY_SECURITY_ANSWER_HASH, hashString(cleanAnswer))
            .apply()
    }

    fun getSecurityQuestion(): String {
        return prefs.getString(KEY_SECURITY_QUESTION, "What is your secret backup word?")
            ?: "What is your secret backup word?"
    }

    fun verifySecurityAnswer(answer: String): Boolean {
        val storedHash = prefs.getString(KEY_SECURITY_ANSWER_HASH, "") ?: return false
        val cleanAnswer = answer.trim().lowercase()
        return hashString(cleanAnswer) == storedHash
    }

    fun isStealthCalculatorMode(): Boolean {
        return prefs.getBoolean(KEY_STEALTH_CALCULATOR_MODE, true)
    }

    fun setStealthCalculatorMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STEALTH_CALCULATOR_MODE, enabled).apply()
    }

    fun isAutoLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_LOCK, true)
    }

    fun setAutoLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_LOCK, enabled).apply()
    }

    fun isPanicEnabled(): Boolean {
        return prefs.getBoolean(KEY_PANIC_ENABLED, true)
    }

    fun setPanicEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PANIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun getInactivityTimeoutSeconds(): Int {
        return prefs.getInt(KEY_INACTIVITY_TIMEOUT_SECONDS, 60)
    }

    fun setInactivityTimeoutSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_INACTIVITY_TIMEOUT_SECONDS, seconds).apply()
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest((input + SALT).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
