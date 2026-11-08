package com.kuafor.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Firestore: shops/{shopId}/barbers/{barberId}/slots/{slotId}
 *
 * Çakışmasız randevu kuralının kalbi: `isBooked` alanı yalnızca
 * KuaforRepository.bookAppointment() transaction'ı içinde false -> true yapılır.
 */
data class TimeSlot(
    @DocumentId var id: String = "",
    var barberId: String = "",
    /** Tarih, "yyyy-MM-dd" formatında. */
    var date: String = "",
    /** Başlangıç saati, "HH:mm" formatında. */
    var startTime: String = "",
    @get:PropertyName("isBooked") @set:PropertyName("isBooked")
    var isBooked: Boolean = false
)
