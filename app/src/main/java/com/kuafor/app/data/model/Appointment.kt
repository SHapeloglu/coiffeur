package com.kuafor.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/** Firestore: appointments/{appointmentId} */
data class Appointment(
    @DocumentId var id: String = "",
    var customerUid: String = "",
    var shopId: String = "",
    var barberId: String = "",
    var serviceId: String = "",
    var slotId: String = "",
    /** Rezervasyon anındaki hizmet adı (sonradan değişse de randevuda sabit kalsın diye kopyalanır). */
    var serviceName: String = "",
    /** "yyyy-MM-dd" */
    var date: String = "",
    /** "HH:mm" */
    var startTime: String = "",
    var durationMinutes: Int = 30,
    /** İndirim uygulanmış toplam fiyat (TL). */
    var totalPriceTL: Double = 0.0,
    /** "BOOKED", "CANCELLED", "COMPLETED" ... İptal akışı henüz yok (TODO). */
    var status: String = STATUS_BOOKED,
    @ServerTimestamp var createdAt: Date? = null
) {
    companion object {
        const val STATUS_BOOKED = "BOOKED"
    }
}
