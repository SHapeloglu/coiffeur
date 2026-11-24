"use strict";

// Randevusuz kalmış slot kilitlerini serbest bırakır.
//
// İstemci bir slotu kilitlerken (isBooked false -> true) lockedAt alanını sunucu
// zamanıyla yazar. Randevu geçerli oluşturulursa trigger lockedAt'i temizler.
// Burada, lockedAt'i LOCK_TTL_MS'den eski olup hâlâ geçerli randevusu olmayan
// slotlar serbest bırakılır; geçerli randevusu olanların yalnızca işareti temizlenir.

const { FieldValue, Timestamp } = require("firebase-admin/firestore");

const LOCK_TTL_MS = 10 * 60 * 1000;

/**
 * @param {FirebaseFirestore.Firestore} db
 * @param {number} nowMs
 * @returns {Promise<{scanned: number, released: number, confirmed: number}>}
 */
async function releaseExpiredLocks(db, nowMs = Date.now()) {
  const cutoff = Timestamp.fromMillis(nowMs - LOCK_TTL_MS);
  const candidates = await db.collectionGroup("slots").where("lockedAt", "<", cutoff).get();

  let released = 0;
  let confirmed = 0;
  for (const candidate of candidates.docs) {
    const outcome = await db.runTransaction(async (tx) => {
      const slotSnap = await tx.get(candidate.ref);
      const lockedAt = slotSnap.get("lockedAt");
      // Okuma ile yazma arasında kilit yenilendiyse ya da zaten temizlendiyse dokunma.
      if (!slotSnap.exists || !lockedAt || lockedAt.toMillis() >= cutoff.toMillis()) {
        return "skipped";
      }

      const barberRef = slotSnap.ref.parent.parent;
      const shopId = barberRef.parent.parent.id;
      const barberId = barberRef.id;
      const bookingsQuery = db
        .collection("appointments")
        .where("shopId", "==", shopId)
        .where("barberId", "==", barberId)
        .where("slotId", "==", slotSnap.id)
        .where("status", "==", "BOOKED")
        .limit(1);
      const bookings = await tx.get(bookingsQuery);

      if (slotSnap.get("isBooked") === true && bookings.empty) {
        tx.update(slotSnap.ref, { isBooked: false, lockedAt: FieldValue.delete() });
        return "released";
      }
      // Randevusu var (trigger lockedAt'i temizlemeyi atlamış olabilir) ya da zaten boş.
      tx.update(slotSnap.ref, { lockedAt: FieldValue.delete() });
      return bookings.empty ? "cleared" : "confirmed";
    });

    if (outcome === "released") released++;
    if (outcome === "confirmed") confirmed++;
  }

  return { scanned: candidates.size, released, confirmed };
}

module.exports = { releaseExpiredLocks, LOCK_TTL_MS };
