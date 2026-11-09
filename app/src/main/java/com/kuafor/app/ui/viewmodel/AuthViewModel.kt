package com.kuafor.app.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuafor.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_ERROR_MESSAGE = "Bir hata oluştu, lütfen tekrar deneyin."
private const val OTP_LENGTH = 6

enum class AuthStep { PHONE, CODE }

data class AuthUiState(
    /** PHONE: telefon girişi; CODE: SMS kodu girişi. */
    val step: AuthStep = AuthStep.PHONE,
    /** Firebase'in SMS gönderimi sırasında döndürdüğü doğrulama ID'si (CODE adımında dolu). */
    val verificationId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val loggedIn: Boolean = false
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
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: DEFAULT_ERROR_MESSAGE)
                    }
                }
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
        _uiState.update { it.copy(step = AuthStep.PHONE, verificationId = null, error = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
