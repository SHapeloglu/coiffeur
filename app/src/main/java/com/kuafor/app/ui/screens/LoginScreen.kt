package com.kuafor.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kuafor.app.ui.viewmodel.AuthStep
import com.kuafor.app.ui.viewmodel.AuthViewModel

private const val PHONE_DIGITS = 10
private const val CODE_DIGITS = 6
private const val COUNTRY_CODE = "+90"

/**
 * Telefon + SMS OTP giriş ekranı. Firebase çağrısı yapmaz; tüm işi [AuthViewModel] yapar.
 * Oturum açılınca [onLoggedIn] çağrılır (navigasyon NavGraph'ta).
 */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    // SMS doğrulaması için Activity gerekli; ContextWrapper zincirinden bulunur.
    val activity = LocalContext.current.findActivity()

    var phoneInput by rememberSaveable { mutableStateOf("") }
    var codeInput by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.loggedIn) {
        if (state.loggedIn) onLoggedIn()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Giriş yap",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (state.step == AuthStep.PHONE) {
                "Telefon numaranı gir, sana SMS ile 6 haneli bir kod gönderelim."
            } else {
                "$COUNTRY_CODE $phoneInput numarasına gelen 6 haneli kodu gir."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        when (state.step) {
            AuthStep.PHONE -> {
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { input ->
                        phoneInput = input.filter { it.isDigit() }.take(PHONE_DIGITS)
                    },
                    label = { Text("Telefon numarası") },
                    prefix = { Text(COUNTRY_CODE) },
                    placeholder = { Text("5xx xxx xx xx") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (activity != null) {
                            viewModel.sendOtp(toE164(phoneInput), activity)
                        }
                    },
                    enabled = !state.isLoading &&
                        phoneInput.length == PHONE_DIGITS &&
                        activity != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kod gönder")
                }
            }

            AuthStep.CODE -> {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { input ->
                        codeInput = input.filter { it.isDigit() }.take(CODE_DIGITS)
                    },
                    label = { Text("Doğrulama kodu") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.verifyOtp(codeInput) },
                    enabled = !state.isLoading && codeInput.length == CODE_DIGITS,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Doğrula")
                }
                Spacer(Modifier.height(8.dp))
                // TODO: "Kodu tekrar gönder" — yeniden gönderme için ForceResendingToken
                // saklanmalı (AuthRepository.sendOtp şu an token'ı saklamıyor).
                OutlinedButton(
                    onClick = {
                        codeInput = ""
                        viewModel.changePhone()
                    },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Numarayı değiştir")
                }
            }
        }

        if (state.isLoading) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }

        state.error?.let { message ->
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }

        if (activity == null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Giriş ekranı bir Activity içinde açılmalı.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** "5321234567" -> "+905321234567" (E.164). */
private fun toE164(nationalDigits: String): String = "$COUNTRY_CODE$nationalDigits"

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
