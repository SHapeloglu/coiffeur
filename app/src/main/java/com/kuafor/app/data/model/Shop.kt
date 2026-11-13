package com.kuafor.app.data.model

import com.google.firebase.firestore.DocumentId

/** Firestore: shops/{shopId} */
data class Shop(
    @DocumentId var id: String = "",
    var name: String = "",
    var address: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    /** Dükkan sahibinin Firebase Auth uid'i. */
    var ownerUid: String = "",
    /** TODO: Cloud Function ile güncellenecek (bkz. TASK.md). */
    var averageRating: Double = 0.0,
    var reviewCount: Int = 0,
    var imageUrl: String = "",
    var phone: String = ""
)
