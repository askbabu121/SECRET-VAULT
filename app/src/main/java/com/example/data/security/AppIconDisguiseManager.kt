package com.example.data.security

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

enum class AppIconDisguise(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val componentClass: String
) {
    VAULT(
        id = "VAULT",
        displayName = "Secret Vault",
        subtitle = "Standard secure vault icon",
        componentClass = "com.example.MainActivity"
    ),
    NOTES(
        id = "NOTES",
        displayName = "Notes",
        subtitle = "Generic yellow notepad icon",
        componentClass = "com.example.MainActivityAliasNotes"
    ),
    CALCULATOR(
        id = "CALCULATOR",
        displayName = "Calculator",
        subtitle = "Standard utility calculator icon",
        componentClass = "com.example.MainActivityAliasCalculator"
    ),
    CLOCK(
        id = "CLOCK",
        displayName = "Clock",
        subtitle = "Minimalist clock icon",
        componentClass = "com.example.MainActivityAliasClock"
    );

    companion object {
        fun fromId(id: String?): AppIconDisguise {
            return entries.find { it.id == id } ?: VAULT
        }
    }
}

object AppIconDisguiseManager {

    fun setAppIconDisguise(context: Context, disguise: AppIconDisguise) {
        val pm = context.packageManager
        val pkg = context.packageName

        // Enable the target component first
        val targetComponent = ComponentName(pkg, disguise.componentClass)
        pm.setComponentEnabledSetting(
            targetComponent,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // Disable other components
        AppIconDisguise.entries.forEach { option ->
            if (option != disguise) {
                val comp = ComponentName(pkg, option.componentClass)
                pm.setComponentEnabledSetting(
                    comp,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }
}
