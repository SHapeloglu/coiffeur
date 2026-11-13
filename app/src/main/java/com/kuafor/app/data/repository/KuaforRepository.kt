package com.kuafor.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.kuafor.app.data.model.Appointment
import com.kuafor.app.data.model.Barber
import com.kuafor.app.data.model.Service
import com.kuafor.app.data.model.Shop
import com.kuafor.app.data.model.TimeSlot
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tüm Firebase erişiminin tek geçtiği katman. ViewModel'ler Firebase'i doğrudan
 * çağırmaz; yalnızca bu sınıfın suspend fonksiyonlarını kullanır.
 *
 * Firestore yolları:
 *  shops/{shopId}
 *    services/{serviceId}
 *    barbers/{barberId}/slots/{slotId}
 *    reviews/{reviewId}
 *  appointments/{appointmentId}
 */
@Singleton
class KuaforRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private val shopsRef: CollectionReference
        get() = firestore.collection(COLLECTION_SHOPS)

    private val appointmentsRef: CollectionReference
        get() = firestore.collection(COLLECTION_APPOINTMENTS)

    private fun barbersRef(shopId: String) =
        shopsRef.document(shopId).collection(SUBCOLLECTION_BARBERS)

    private fun servicesRef(shopId: String) =
        shopsRef.document(shopId).collection(SUBCOLLECTION_SERVICES)

    private fun slotsRef(shopId: String, barberId: String) =
        barbersRef(shopId).document(barberId).collection(SUBCOLLECTION_SLOTS)

    private fun currentUid(): String =
        auth.currentUser?.uid ?: throw NotSignedInException()

    // ---------------------------------------------------------------- okuma

    suspend fun getShops(): List<Shop> =
        shopsRef.get().await().toObjects(Shop::class.java)

    /* TODO: getNearbyShops(lat, lng, radiusKm) — şu an tüm koleksiyonu çekmek
     * yerine geohash tabanlı sorgu (bkz. TASK.md). */

    suspend fun getShop(shopId: String): Shop? =
        shopsRef.document(shopId).get().await().toObject(Shop::class.java)

    suspend fun getBarbers(shopId: String): List<Barber> =
        barbersRef(shopId).get().await().toObjects(Barber::class.java)

    suspend fun getServices(shopId: String): List<Service> =
        servicesRef(shopId).get().await().toObjects(Service::class.java)

    /**
     * Belirli bir berber için, verilen güne ait boş (isBooked == false) slotları
     * saate göre sıralı döner. Equality filtreleri olduğu için composite index
     * gerekmez; sıralama istemci tarafında yapılır.
     *
     * @param date "yyyy-MM-dd" formatında tarih.
     */
    suspend fun getAvailableSlots(shopId: String, barberId: String, date: String): List<TimeSlot> =
        slotsRef(shopId, barberId)
            .whereEqualTo("date", date)
            .whereEqualTo("isBooked", false)
            .get()
            .await()
            .toObjects(TimeSlot::class.java)
            .filter { isUpcoming(it.date, it.startTime) }
            .sortedBy { it.startTime }

    /** Giriş yapmış müşterinin randevuları (en yakın tarih önce). */
    suspend fun getMyAppointments(): List<Appointment> =
        appointmentsRef
            .whereEqualTo("customerUid", currentUid())
            .get()
            .await()
            .toObjects(Appointment::class.java)
            .sortedWith(compareBy<Appointment>({ it.date }, { it.startTime }))

    /* TODO: getReviews(shopId) ve addReview(...) — addReview sonrası averageRating
     * bir Cloud Function ile güncellenecek (bkz. TASK.md). */

    /* TODO: cancelAppointment(appointmentId) — slot isBooked'ı true -> false
     * yapmak için firestore.rules'taki slot update kuralının genişletilmesi gerekir. */

    // ---------------------------------------------------------- randevu

    /**
     * Çakışmasız randevu oluşturma. KRİTİK: bu fonksiyon tek bir Firestore
     * transaction içinde çalışır; slot okunur, doluysa hata fırlatılır, değilse
     * aynı transaction içinde slot isBooked = true yapılır ve randevu dokümanı
     * oluşturulur. Bu atomikliği bozacak değişiklik yapılmamalı.
     *
     * Transaction kuralı: tüm okumalar (tx.get) yazmalardan (tx.update / tx.set)
     * önce yapılır.
     *
     * Randevu dokümanının ID'si transaction dışında, istemci tarafında üretilir;
     * böylece ID transaction'ın retry'lerinde değişmez.
     *
     * @return oluşturulan randevunun ID'si
     * @throws SlotNotFoundException slot dokümanı yoksa
     * @throws SlotAlreadyBookedException slot az önce başka biri tarafından alındıysa
     */
    suspend fun bookAppointment(
        shopId: String,
        barberId: String,
        slotId: String,
        serviceId: String
    ): String {
        val customerUid = currentUid()

        val slotRef = slotsRef(shopId, barberId).document(slotId)
        val serviceRef = servicesRef(shopId).document(serviceId)
        val appointmentRef = appointmentsRef.document()

        firestore.runTransaction { tx ->
            // --- 1) OKUMALAR ---
            val slotSnapshot = tx.get(slotRef)
            if (!slotSnapshot.exists()) throw SlotNotFoundException()
            val slot = slotSnapshot.toObject(TimeSlot::class.java)
                ?: throw SlotNotFoundException()

            if (slot.isBooked) throw SlotAlreadyBookedException()

            val serviceSnapshot = tx.get(serviceRef)
            if (!serviceSnapshot.exists()) throw ServiceNotFoundException()
            val service = serviceSnapshot.toObject(Service::class.java)
                ?: throw ServiceNotFoundException()

            // --- 2) YAZMALAR (aynı transaction) ---
            tx.update(slotRef, "isBooked", true)

            val appointment = Appointment(
                id = appointmentRef.id,
                customerUid = customerUid,
                shopId = shopId,
                barberId = barberId,
                serviceId = serviceId,
                slotId = slotId,
                serviceName = service.name,
                date = slot.date,
                startTime = slot.startTime,
                durationMinutes = service.durationMinutes,
                totalPriceTL = service.discountedPriceTL(),
                status = Appointment.STATUS_BOOKED
            )
            tx.set(appointmentRef, appointment)

            Unit
        }.await()

        awaitServerVerdict(appointmentRef)
        return appointmentRef.id
    }

    /**
     * Sunucu trigger'ı (onAppointmentCreated) randevuyu sonradan reddedebilir (V8/V9).
     * Kısa süre randevu durumunu izler; REJECTED ise kullanıcıya hata verilir. Süre dolunca
     * hâlâ BOOKED ise başarılı kabul edilir (trigger henüz çalışmamış olabilir).
     */
    private suspend fun awaitServerVerdict(appointmentRef: DocumentReference) {
        repeat(VERDICT_POLL_ATTEMPTS) {
            val snapshot = appointmentRef.get().await()
            when (snapshot.getString("status")) {
                Appointment.STATUS_BOOKED, null -> delay(VERDICT_POLL_DELAY_MS)
                "REJECTED" -> throw rejectionFor(snapshot.getString("rejectReason"))
                else -> return
            }
        }
    }

    private fun rejectionFor(reason: String?): KuaforException = when (reason) {
        "SLOT_ALREADY_TAKEN" -> SlotAlreadyBookedException()
        "PAST_SLOT" -> SlotInPastException()
        "SLOT_NOT_FOUND" -> SlotNotFoundException()
        "SERVICE_NOT_FOUND" -> ServiceNotFoundException()
        else -> AppointmentRejectedException()
    }

    private companion object {
        const val VERDICT_POLL_ATTEMPTS = 6
        const val VERDICT_POLL_DELAY_MS = 500L

        /** Slot saatleri Türkiye saatiyle (UTC+3) yorumlanır; istemci saat dilimine güvenilmez. */
        val SLOT_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Europe/Istanbul")

        /** Slot başlangıcı şu andan sonra mı? Okunamayan tarih/saat listeye alınmaz. */
        fun isUpcoming(date: String, startTime: String): Boolean {
            val startMs = try {
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
                    isLenient = false
                    timeZone = SLOT_TIME_ZONE
                }.parse("$date $startTime")?.time
            } catch (e: ParseException) {
                null
            } ?: return false
            return startMs > System.currentTimeMillis()
        }

        const val COLLECTION_SHOPS = "shops"
        const val COLLECTION_APPOINTMENTS = "appointments"
        const val SUBCOLLECTION_SERVICES = "services"
        const val SUBCOLLECTION_BARBERS = "barbers"
        const val SUBCOLLECTION_SLOTS = "slots"
    }
}
