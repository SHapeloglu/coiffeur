package com.kuafor.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kuafor.app.data.model.Service
import com.kuafor.app.data.model.TimeSlot
import com.kuafor.app.data.repository.KuaforException
import com.kuafor.app.data.repository.KuaforRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_ERROR_MESSAGE = "Bir hata oluştu, lütfen tekrar deneyin."

@HiltViewModel
class BookingViewModel @Inject constructor(
    private val repository: KuaforRepository
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val services: List<Service> = emptyList(),
        val slots: List<TimeSlot> = emptyList(),
        /** Başarılı rezervasyonun randevu ID'si; UI gösterdikten sonra [consumeBookingResult] ile temizlenir. */
        val bookedAppointmentId: String? = null,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Dükkanın hizmetlerini ve berberin seçilen gündeki boş slotlarını yükler. */
    fun load(shopId: String, barberId: String, date: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val services = repository.getServices(shopId)
                val slots = repository.getAvailableSlots(shopId, barberId, date)
                _uiState.update { it.copy(isLoading = false, services = services, slots = slots) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.toUserMessage()) }
            }
        }
    }

    /**
     * Slot rezervasyonu. Çakışma kontrolü repository'deki Firestore transaction'da yapılır;
     * başarılı olursa slot listeden düşürülür.
     */
    fun bookSlot(shopId: String, barberId: String, slotId: String, serviceId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val appointmentId = repository.bookAppointment(shopId, barberId, slotId, serviceId)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        slots = state.slots.filterNot { it.id == slotId },
                        bookedAppointmentId = appointmentId
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.toUserMessage()) }
            }
        }
    }

    fun consumeBookingResult() {
        _uiState.update { it.copy(bookedAppointmentId = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun Exception.toUserMessage(): String =
        if (this is KuaforException) message ?: DEFAULT_ERROR_MESSAGE else DEFAULT_ERROR_MESSAGE
}
