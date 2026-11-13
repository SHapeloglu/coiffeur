package com.kuafor.app.data.model

import com.google.firebase.firestore.DocumentId

/** Firestore: shops/{shopId}/services/{serviceId} */
data class Service(
    @DocumentId var id: String = "",
    var shopId: String = "",
    var name: String = "",
    var description: String = "",
    var durationMinutes: Int = 30,
    /** Liste fiyatı (TL). */
    var priceTL: Double = 0.0,
    /** 0-100 arası indirim yüzdesi. */
    var discountPercent: Int = 0
) {
    /** İndirim uygulanmış fiyat (TL). Firestore'a yazılmaması için property değil fonksiyon. */
    fun discountedPriceTL(): Double =
        priceTL * (100 - discountPercent.coerceIn(0, 100)) / 100.0
}
