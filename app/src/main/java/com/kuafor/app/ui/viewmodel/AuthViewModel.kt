package com.kuafor.app.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuafor.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_ERROR_MESSAGE = "Bir hata oluştu, lütfen tekrar deneyin."
private const val OTP_LENGTH = 6
private const val RESEND_COOLDOWN_SECONDS = 30

enum class AuthStep { PHONE, CODE }

data class AuthUiState(
    /** PHONE: telefon girişi; CODE: SMS kodu girişi. */
    val step: AuthStep = AuthStep.PHONE,
    /** Firebase'in SMS gönderimi sırasında döndürdüğü doğrulama ID'si (CODE adımında dolu). */
    val verificationId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false,
    /** Yeniden gönderme için kalan saniye; 0 ise "Kodu tekrar gönder" aktif. */
    val resendCooldown: Int = 0
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // Başlangıç değeri repository'den senkron okunur; Splash yönlendirmesi bunu kullanır.
    private val _uiState = MutableStateFlow(
        AuthUiState(loggedIn = authRepository.currentUid.value != null)
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.currentUid.collect { uid ->
                _uiState.update { it.copy(loggedIn = uid != null) }
            }
        }
    }

    /**
     * [phoneE164] "+90..." biçiminde olmalı (E.164 dönüşümü ekran tarafında yapılır).
     * [activity] yalnızca çağrı süresince kullanılır, ViewModel'de saklanmaz.
     */
    fun sendOtp(phoneE164: String, activity: Activity) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            authRepository.sendOtp(phoneE164, activity)
                .onSuccess { verificationId ->
                    if (verificationId == AuthRepository.AUTO_VERIFIED_ID) {
                        // Oturum zaten açıldı; loggedIn, repository akışından güncellenir.
                        _uiState.update { it.copy(isLoading = false) }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                step = AuthStep.CODE,
                                verificationId = verificationId
                            )
                        }
                        startResendCooldown()
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: DEFAULT_ERROR_MESSAGE)
                    }
                }
        }
    }

    /**
     * Kodu son numaraya yeniden gönderir. Yeni doğrulama ID'si CODE adımında kullanılır.
     * Bekleme süresi dolmadan ya da başka bir işlem sürerken çağrılırsa hiçbir şey yapmaz.
     */
    fun resendOtp(activity: Activity) {
        val current = _uiState.value
        if (current.isLoading || current.resendCooldown > 0) return
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            authRepository.resendOtp(activity)
                .onSuccess { verificationId ->
                    if (verificationId == AuthRepository.AUTO_VERIFIED_ID) {
                        _uiState.update { it.copy(isLoading = false) }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, verificationId = verificationId)
                        }
                        startResendCooldown()
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: DEFAULT_ERROR_MESSAGE)
                    }
                }
        }
    }

    private fun startResendCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (remaining in RESEND_COOLDOWN_SECONDS downTo 1) {
                _uiState.update { it.copy(resendCooldown = remaining) }
                delay(1_000L)
            }
            _uiState.update { it.copy(resendCooldown = 0) }
        }
    }

    fun verifyOtp(code: String) {
        val verificationId = _uiState.value.verificationId ?: return
        if (_uiState.value.isLoading) return

        if (code.length != OTP_LENGTH) {
            _uiState.update { it.copy(error = "Doğrulama kodu 6 haneli olmalıdır.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            authRepository.verifyOtp(verificationId, code)
                .onSuccess {
                    // loggedIn, currentUid akışından true olur.
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: DEFAULT_ERROR_MESSAGE)
                    }
                }
        }
    }

    /** Kod adımından telefon adımına döner. */
    fun changePhone() {
        cooldownJob?.cancel()
        _uiState.update {
            it.copy(
                step = AuthStep.PHONE,
                verificationId = null,
                error = null,
                resendCooldown = 0
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
