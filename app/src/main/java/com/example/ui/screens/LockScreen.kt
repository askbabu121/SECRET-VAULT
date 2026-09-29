package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CalculatorKeyAction
import com.example.ui.theme.CalculatorKeyEquals
import com.example.ui.theme.CalculatorKeyNum
import com.example.ui.theme.CalculatorKeyOp
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultIndigo
import com.example.ui.theme.VaultRose
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.theme.VaultTextMuted
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@Composable
fun LockScreen(
    isStealthMode: Boolean,
    onToggleStealthMode: (Boolean) -> Unit,
    onPinEntered: (String) -> Boolean,
    isBiometricEnabled: Boolean = false,
    onBiometricClick: () -> Unit = {},
    securityQuestion: String,
    onResetPinWithRecovery: (answer: String, newPin: String) -> Boolean
) {
    var showRecoveryDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
    ) {
        if (isStealthMode) {
            CalculatorDisguiseView(
                onTryUnlock = onPinEntered,
                isBiometricEnabled = isBiometricEnabled,
                onBiometricClick = onBiometricClick,
                onSwitchToKeypad = { onToggleStealthMode(false) },
                onForgotPassword = { showRecoveryDialog = true }
            )
        } else {
            DirectKeypadView(
                onTryUnlock = onPinEntered,
                isBiometricEnabled = isBiometricEnabled,
                onBiometricClick = onBiometricClick,
                onSwitchToCalculator = { onToggleStealthMode(true) },
                onForgotPassword = { showRecoveryDialog = true }
            )
        }

        if (showRecoveryDialog) {
            RecoveryDialog(
                securityQuestion = securityQuestion,
                onDismiss = { showRecoveryDialog = false },
                onResetConfirmed = { answer, newPin ->
                    val success = onResetPinWithRecovery(answer, newPin)
                    if (success) {
                        showRecoveryDialog = false
                    }
                    success
                }
            )
        }
    }
}

@Composable
fun CalculatorDisguiseView(
    onTryUnlock: (String) -> Boolean,
    isBiometricEnabled: Boolean = false,
    onBiometricClick: () -> Unit = {},
    onSwitchToKeypad: () -> Unit,
    onForgotPassword: () -> Unit
) {
    var displayExpr by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("0") }
    var currentInputDigits by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Calculator Header disguised as normal tool
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onForgotPassword() }
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Calculator",
                    tint = VaultTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Calculator",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = VaultTextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isBiometricEnabled) {
                    IconButton(
                        onClick = onBiometricClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Unlock",
                            tint = VaultCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                IconButton(
                    onClick = onSwitchToKeypad,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Switch to Keypad",
                        tint = VaultTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Calculator Display Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            if (displayExpr.isNotEmpty()) {
                Text(
                    text = displayExpr,
                    fontSize = 20.sp,
                    color = VaultTextSecondary,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (currentInputDigits.isNotEmpty()) currentInputDigits else resultText,
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                color = VaultTextPrimary,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }

        // Calculator Buttons Grid
        val calcRows = listOf(
            listOf("AC", "±", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "⌫", "=")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            calcRows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { key ->
                        val isOp = key in listOf("÷", "×", "-", "+")
                        val isEquals = key == "="
                        val isAction = key in listOf("AC", "±", "%", "⌫")

                        val btnColor = when {
                            isEquals -> CalculatorKeyEquals
                            isOp -> CalculatorKeyOp
                            isAction -> CalculatorKeyAction
                            else -> CalculatorKeyNum
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(btnColor)
                                .clickable {
                                    when (key) {
                                        "AC" -> {
                                            displayExpr = ""
                                            currentInputDigits = ""
                                            resultText = "0"
                                        }
                                        "⌫" -> {
                                            if (currentInputDigits.isNotEmpty()) {
                                                currentInputDigits = currentInputDigits.dropLast(1)
                                            }
                                        }
                                        "±" -> {
                                            if (currentInputDigits.isNotEmpty()) {
                                                currentInputDigits = if (currentInputDigits.startsWith("-")) {
                                                    currentInputDigits.substring(1)
                                                } else {
                                                    "-$currentInputDigits"
                                                }
                                            }
                                        }
                                        "%" -> {
                                            val v = currentInputDigits.toDoubleOrNull()
                                            if (v != null) {
                                                currentInputDigits = (v / 100).toString()
                                            }
                                        }
                                        in listOf("÷", "×", "-", "+") -> {
                                            if (currentInputDigits.isNotEmpty()) {
                                                displayExpr = "$currentInputDigits $key "
                                                currentInputDigits = ""
                                            }
                                        }
                                        "=" -> {
                                            // FIRST: Check if current input is the secret Vault PIN!
                                            val cleanPin = currentInputDigits.replace(".", "").replace("-", "")
                                            val unlocked = onTryUnlock(cleanPin)
                                            if (!unlocked) {
                                                // Perform normal standard calculation fallback so it stays disguised
                                                if (displayExpr.isNotEmpty() && currentInputDigits.isNotEmpty()) {
                                                    val calculated = evaluateMath(displayExpr + currentInputDigits)
                                                    resultText = calculated
                                                    displayExpr = ""
                                                    currentInputDigits = calculated
                                                } else if (currentInputDigits.isNotEmpty()) {
                                                    resultText = currentInputDigits
                                                }
                                            }
                                        }
                                        "." -> {
                                            if (!currentInputDigits.contains(".")) {
                                                currentInputDigits = if (currentInputDigits.isEmpty()) "0." else "$currentInputDigits."
                                            }
                                        }
                                        else -> {
                                            // Digit tapped
                                            currentInputDigits += key
                                        }
                                    }
                                }
                                .testTag("calc_key_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                fontSize = if (key.length > 1) 20.sp else 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    isEquals -> Color.White
                                    isOp -> Color.White
                                    isAction -> VaultCyan
                                    else -> VaultTextPrimary
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun evaluateMath(expr: String): String {
    return try {
        val parts = expr.split(" ")
        if (parts.size >= 3) {
            val a = parts[0].toDoubleOrNull() ?: 0.0
            val op = parts[1]
            val b = parts[2].toDoubleOrNull() ?: 0.0
            val res = when (op) {
                "+" -> a + b
                "-" -> a - b
                "×" -> a * b
                "÷" -> if (b != 0.0) a / b else Double.NaN
                else -> b
            }
            if (res.isNaN()) "Error"
            else if (res == res.toLong().toDouble()) res.toLong().toString()
            else String.format("%.2f", res)
        } else {
            expr
        }
    } catch (e: Exception) {
        "0"
    }
}

@Composable
fun DirectKeypadView(
    onTryUnlock: (String) -> Boolean,
    isBiometricEnabled: Boolean = false,
    onBiometricClick: () -> Unit = {},
    onSwitchToCalculator: () -> Unit,
    onForgotPassword: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorShake by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onSwitchToCalculator,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = "Calculator Mode",
                    tint = VaultTextSecondary
                )
            }

            TextButton(onClick = onForgotPassword) {
                Text("Forgot PIN?", color = VaultCyan, fontSize = 13.sp)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(VaultCard)
                    .border(2.dp, VaultCyan.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = VaultCyan,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Secret Vault Locked",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary
            )
            Text(
                text = "Enter your secret PIN or use biometric scan",
                fontSize = 13.sp,
                color = VaultTextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            PinDotsDisplay(pinLength = pin.length, total = 4)

            if (isBiometricEnabled) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(VaultCard)
                        .border(1.dp, VaultCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .clickable { onBiometricClick() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("biometric_unlock_pill")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Biometric Scan",
                        tint = VaultCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Touch Fingerprint",
                        color = VaultCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            errorShake?.let {
                Text(
                    text = it,
                    color = VaultRose,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        NumericKeypad(
            onDigitClick = { digit ->
                if (pin.length < 4) {
                    val newPin = pin + digit
                    pin = newPin
                    errorShake = null
                    if (newPin.length == 4) {
                        val unlocked = onTryUnlock(newPin)
                        if (!unlocked) {
                            errorShake = "Incorrect PIN"
                            pin = ""
                        }
                    }
                }
            },
            onBackspaceClick = {
                if (pin.isNotEmpty()) pin = pin.dropLast(1)
                errorShake = null
            },
            onClearClick = {
                pin = ""
                errorShake = null
            }
        )
    }
}

@Composable
fun RecoveryDialog(
    securityQuestion: String,
    onDismiss: () -> Unit,
    onResetConfirmed: (answer: String, newPin: String) -> Boolean
) {
    var answer by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurface,
        title = {
            Text(
                text = "PIN Recovery",
                color = VaultTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Security Question:\n$securityQuestion",
                    color = VaultTextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("Your Answer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VaultCyan,
                        unfocusedBorderColor = VaultCardBorder,
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4) newPin = it },
                    label = { Text("New 4-digit PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VaultCyan,
                        unfocusedBorderColor = VaultCardBorder,
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary
                    )
                )

                error?.let {
                    Text(
                        text = it,
                        color = VaultRose,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (answer.isBlank()) {
                        error = "Please answer the security question."
                    } else if (newPin.length < 4) {
                        error = "New PIN must be 4 digits."
                    } else {
                        val ok = onResetConfirmed(answer, newPin)
                        if (!ok) {
                            error = "Answer is incorrect. Try again."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VaultCyan)
            ) {
                Text("Reset & Unlock", color = Color(0xFF0B0F19), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultTextSecondary)
            }
        }
    )
}
