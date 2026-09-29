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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultCyan
import com.example.ui.theme.VaultDarkBg
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultIndigo
import com.example.ui.theme.VaultRose
import com.example.ui.theme.VaultSurfaceVariant
import com.example.ui.theme.VaultTextMuted
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@Composable
fun SetupPinScreen(
    onSetupComplete: (pin: String, question: String, answer: String, stealthMode: Boolean) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Enter PIN, 2: Confirm PIN, 3: Security recovery & disguise
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var securityQuestion by remember { mutableStateOf("What is your secret backup word?") }
    var securityAnswer by remember { mutableStateOf("") }
    var stealthCalculator by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetQuestions = listOf(
        "What is your secret backup word?",
        "What was the name of your first pet?",
        "What is your favorite movie?",
        "What city were you born in?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // App Brand Shield Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(VaultCard)
                    .border(2.dp, VaultCyan.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Vault Setup",
                    tint = VaultCyan,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to Secret Vault",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Your private, isolated media locker. Photos & videos stored here are invisible to public gallery apps.",
                fontSize = 14.sp,
                color = VaultTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Steps Indicator
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                StepCircle(number = 1, isActive = step >= 1, isCurrent = step == 1)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.width(32.dp).height(2.dp).background(if (step >= 2) VaultCyan else VaultCardBorder))
                Spacer(modifier = Modifier.width(8.dp))
                StepCircle(number = 2, isActive = step >= 2, isCurrent = step == 2)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.width(32.dp).height(2.dp).background(if (step >= 3) VaultCyan else VaultCardBorder))
                Spacer(modifier = Modifier.width(8.dp))
                StepCircle(number = 3, isActive = step >= 3, isCurrent = step == 3)
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (step) {
                1 -> {
                    Text(
                        text = "Create Master PIN",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VaultTextPrimary
                    )
                    Text(
                        text = "Enter a 4-digit PIN to lock your vault",
                        fontSize = 13.sp,
                        color = VaultTextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    PinDotsDisplay(pinLength = pin.length, total = 4)

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = VaultRose,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    NumericKeypad(
                        onDigitClick = { digit ->
                            if (pin.length < 4) {
                                pin += digit
                                errorMessage = null
                                if (pin.length == 4) {
                                    step = 2
                                }
                            }
                        },
                        onBackspaceClick = {
                            if (pin.isNotEmpty()) pin = pin.dropLast(1)
                            errorMessage = null
                        },
                        onClearClick = {
                            pin = ""
                            errorMessage = null
                        }
                    )
                }

                2 -> {
                    Text(
                        text = "Confirm Master PIN",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VaultTextPrimary
                    )
                    Text(
                        text = "Re-enter the 4-digit PIN to confirm",
                        fontSize = 13.sp,
                        color = VaultTextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    PinDotsDisplay(pinLength = confirmPin.length, total = 4)

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = VaultRose,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    NumericKeypad(
                        onDigitClick = { digit ->
                            if (confirmPin.length < 4) {
                                confirmPin += digit
                                errorMessage = null
                                if (confirmPin.length == 4) {
                                    if (confirmPin == pin) {
                                        step = 3
                                    } else {
                                        errorMessage = "PINs do not match. Try again."
                                        confirmPin = ""
                                    }
                                }
                            }
                        },
                        onBackspaceClick = {
                            if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                            errorMessage = null
                        },
                        onClearClick = {
                            confirmPin = ""
                            errorMessage = null
                        }
                    )

                    Button(
                        onClick = {
                            step = 1
                            pin = ""
                            confirmPin = ""
                            errorMessage = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text("Back to change PIN", color = VaultCyan)
                    }
                }

                3 -> {
                    Text(
                        text = "Recovery & Stealth Setup",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VaultTextPrimary
                    )
                    Text(
                        text = "Protect against forgetting your PIN and choose how the vault appears.",
                        fontSize = 13.sp,
                        color = VaultTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = VaultCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VaultCardBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = VaultCyan)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "PIN Recovery Question",
                                    fontWeight = FontWeight.SemiBold,
                                    color = VaultTextPrimary,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Question: $securityQuestion",
                                color = VaultTextSecondary,
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = securityAnswer,
                                onValueChange = {
                                    securityAnswer = it
                                    errorMessage = null
                                },
                                label = { Text("Your Secret Answer") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("security_answer_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = VaultCyan,
                                    unfocusedBorderColor = VaultCardBorder,
                                    focusedTextColor = VaultTextPrimary,
                                    unfocusedTextColor = VaultTextPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stealth Disguise Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VaultCard),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VaultCardBorder, RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (stealthCalculator) VaultCyan.copy(alpha = 0.2f) else VaultSurfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (stealthCalculator) Icons.Default.Calculate else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (stealthCalculator) VaultCyan else VaultTextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (stealthCalculator) "Calculator Disguise Mode" else "Direct PIN Screen",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = VaultTextPrimary
                                    )
                                    Text(
                                        text = if (stealthCalculator)
                                            "Opens a working calculator. Enter PIN + '=' to unlock."
                                        else
                                            "Opens direct PIN keypad.",
                                        fontSize = 12.sp,
                                        color = VaultTextSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = stealthCalculator,
                                onCheckedChange = { stealthCalculator = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = VaultCyan,
                                    checkedTrackColor = VaultCyan.copy(alpha = 0.3f),
                                    uncheckedThumbColor = VaultTextMuted,
                                    uncheckedTrackColor = VaultCardBorder
                                )
                            )
                        }
                    }

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = VaultRose,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (securityAnswer.trim().length < 2) {
                                errorMessage = "Please enter an answer to your recovery question."
                            } else {
                                onSetupComplete(
                                    pin,
                                    securityQuestion,
                                    securityAnswer.trim(),
                                    stealthCalculator
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("finish_setup_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VaultCyan)
                    ) {
                        Text(
                            text = "Finish Setup & Enter Vault",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B0F19)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepCircle(number: Int, isActive: Boolean, isCurrent: Boolean) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(
                when {
                    isCurrent -> VaultCyan
                    isActive -> VaultEmerald
                    else -> VaultSurfaceVariant
                }
            )
            .border(
                1.5.dp,
                if (isCurrent || isActive) VaultCyan else VaultCardBorder,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isActive && !isCurrent) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                text = number.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isCurrent) Color(0xFF0B0F19) else VaultTextSecondary
            )
        }
    }
}

@Composable
fun PinDotsDisplay(pinLength: Int, total: Int = 4) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until total) {
            val isFilled = i < pinLength
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isFilled) VaultCyan else Color.Transparent)
                    .border(
                        2.dp,
                        if (isFilled) VaultCyan else VaultCardBorder,
                        CircleShape
                    )
            )
        }
    }
}

@Composable
fun NumericKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("C", "0", "⌫")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(VaultCard)
                            .border(1.dp, VaultCardBorder.copy(alpha = 0.6f), CircleShape)
                            .clickable {
                                when (key) {
                                    "C" -> onClearClick()
                                    "⌫" -> onBackspaceClick()
                                    else -> onDigitClick(key)
                                }
                            }
                            .testTag("keypad_$key"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = when (key) {
                                "C" -> VaultRose
                                "⌫" -> VaultIndigo
                                else -> VaultTextPrimary
                            }
                        )
                    }
                }
            }
        }
    }
}
