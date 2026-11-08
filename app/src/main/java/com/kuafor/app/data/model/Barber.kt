package com.kuafor.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/** Firestore: shops/{shopId}/barbers/{barberId} */
data class Barber(
    @DocumentId var id: String = "",
    var shopId: String = "",
    var name: String = "",
    /** Örn. "Berber", "Kuaför", "Stilist". */
    var title: String = "",
    var avatarUrl: String = "",
    @get:PropertyName("isActive") @set:PropertyName("isActive")
    var isActive: Boolean = true
)
