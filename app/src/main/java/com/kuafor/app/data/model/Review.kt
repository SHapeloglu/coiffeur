package com.kuafor.app.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/** Firestore: shops/{shopId}/reviews/{reviewId} */
data class Review(
    @DocumentId var id: String = "",
    var shopId: String = "",
    var barberId: String = "",
    var authorUid: String = "",
    var authorName: String = "",
    /** 1-5 arası puan. */
    var rating: Int = 0,
    var comment: String = "",
    @ServerTimestamp var createdAt: Date? = null
)
