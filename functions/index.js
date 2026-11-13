"use strict";

const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const { isPriceValid, averageRating } = require("./lib/pricing");
const { isSafeId, slotStartMs, isPastSlot } = require("./lib/validation");

initializeApp();
const db = getFirestore();

/**
 * appointments/{appointmentId} oluşturulunca çalışır.
 *
 * Not: Bu trigger, istemcinin randevu yazmasından SONRA çalışır. Firestore kuralları
 * aynı transaction içindeki slot yazımını göremediği için (bkz. BACKLOG.md "Randevu–slot bağı")
 * tam çözüm kural tabanlı değildir; burası sunucu tarafı doğrulama ve geri alma katmanıdır.
 *
 * Kontroller (hepsi tek transaction içinde; önce okumalar, sonra yazmalar):
 *  - slot shops/{shopId}/barbers/{barberId}/slots/{slotId} var ve isBooked === true
 *  - hizmet shops/{shopId}/services/{serviceId} var ve totalPriceTL beklenen fiyatla eşleşiyor
 *  - shopId/barberId/slotId/serviceId '/' içermiyor (aksi halde yol hatası; V8)
 *  - slot tarihi okunabiliyor ve başlangıcı henüz gelmemiş (V9: PAST_SLOT)
 *
 * Geçersizse: randevu status = REJECTED + rejectReason yazılır. Slot, bu randevuya
 * ait kilit ise (aynı slota bağlı başka geçerli BOOKED randevu yoksa) isBooked = false yapılır.
 */
exports.onAppointmentCreated = onDocumentCreated(
  "appointments/{appointmentId}",
  async (event) => {
    const apRef = event.data && event.data.ref;
    if (!apRef) return;

    await db.runTransaction(async (tx) => {
      // --- 1) OKUMALAR ---
      const apSnap = await tx.get(apRef);
      if (!apSnap.exists || apSnap.get("status") !== "BOOKED") return;

      const ap = apSnap.data();
      const { shopId, barberId, slotId, serviceId } = ap;

      // '/' içeren id, db.doc() yol hatasına ve trigger'ın çökmesine yol açar (V8); reddedilir.
      if (![shopId, barberId, slotId, serviceId].every(isSafeId)) {
        tx.update(apRef, rejectFields("INVALID_FIELDS"));
        return;
      }

      const slotRef = db.doc(`shops/${shopId}/barbers/${barberId}/slots/${slotId}`);
      const serviceRef = db.doc(`shops/${shopId}/services/${serviceId}`);
      // Aynı slota bağlı, hâlâ geçerli (BOOKED) randevular.
      const sameSlotQuery = db
        .collection("appointments")
        .where("shopId", "==", shopId)
        .where("barberId", "==", barberId)
        .where("slotId", "==", slotId)
        .where("status", "==", "BOOKED");

      const slotSnap = await tx.get(slotRef);
      const serviceSnap = await tx.get(serviceRef);
      const sameSlotSnap = await tx.get(sameSlotQuery);

      // --- 2) KARAR ---
      // Aynı slota daha önce oluşturulmuş geçerli bir randevu varsa bu randevu çakışmadır.
      // Sıralama createdAt ile, eşitlikte doküman kimliğiyle yapılır (deterministik).
      const reason =
        findRejectReason(ap, slotSnap, serviceSnap, Date.now()) ||
        (hasEarlierBooking(sameSlotSnap.docs, apRef.id, apSnap) ? "SLOT_ALREADY_TAKEN" : null);
      if (!reason) return;

      // --- 3) YAZMALAR ---
      tx.update(apRef, rejectFields(reason));

      const otherValidBookings = sameSlotSnap.docs.some((d) => d.id !== apRef.id);
      const slotLockIsOurs =
        slotSnap.exists && slotSnap.get("isBooked") === true && !otherValidBookings;
      if (slotLockIsOurs) {
        tx.update(slotRef, { isBooked: false });
      }

      logger.warn("Randevu reddedildi", { appointmentId: apRef.id, reason });
    });
  }
);

function findRejectReason(ap, slotSnap, serviceSnap, nowMs) {
  if (!slotSnap.exists) return "SLOT_NOT_FOUND";
  if (slotSnap.get("isBooked") !== true) return "SLOT_NOT_BOOKED";

  // Geçmiş tarihli slot rezervasyonu (V9). Saat sunucu tarafında okunur; istemci saatine güvenilmez.
  const date = slotSnap.get("date");
  const startTime = slotSnap.get("startTime");
  if (Number.isNaN(slotStartMs(date, startTime))) return "INVALID_SLOT_TIME";
  if (isPastSlot(date, startTime, nowMs)) return "PAST_SLOT";

  if (!serviceSnap.exists) return "SERVICE_NOT_FOUND";

  const service = serviceSnap.data();
  if (!isPriceValid(ap.totalPriceTL, service.priceTL, service.discountPercent)) {
    return "PRICE_MISMATCH";
  }
  return null;
}

function millisOf(ts) {
  return ts && typeof ts.toMillis === "function" ? ts.toMillis() : Number.MAX_SAFE_INTEGER;
}

function hasEarlierBooking(docs, selfId, selfSnap) {
  const selfMs = millisOf(selfSnap.get("createdAt"));
  return docs.some((d) => {
    if (d.id === selfId) return false;
    const otherMs = millisOf(d.get("createdAt"));
    if (otherMs !== selfMs) return otherMs < selfMs;
    return d.id < selfId;
  });
}

function rejectFields(reason) {
  return { status: "REJECTED", rejectReason: reason };
}

/**
 * shops/{shopId}/reviews/{reviewId} oluşturulunca dükkanın averageRating ve reviewCount
 * alanlarını yeniden hesaplar. Transaction, shop dokümanına eşzamanlı yazımları
 * (ve yorumların okunmasını) Firestore'un iyimser kilitlemesiyle sıraya koyar.
 */
exports.onReviewCreated = onDocumentCreated(
  "shops/{shopId}/reviews/{reviewId}",
  async (event) => {
    const shopId = event.params.shopId;
    const shopRef = db.collection("shops").doc(shopId);
    const reviewsRef = shopRef.collection("reviews");

    await db.runTransaction(async (tx) => {
      const shopSnap = await tx.get(shopRef);
      if (!shopSnap.exists) {
        logger.warn("Yorum için dükkan dokümanı yok, atlanıyor", { shopId });
        return;
      }

      const reviewsSnap = await tx.get(reviewsRef);
      const ratings = reviewsSnap.docs
        .map((d) => d.get("rating"))
        .filter((r) => Number.isFinite(r));

      tx.update(shopRef, {
        averageRating: averageRating(ratings),
        reviewCount: ratings.length,
      });
    });
  }
);
