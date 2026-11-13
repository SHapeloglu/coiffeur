"use strict";

// Saf fonksiyonlar: Firebase'e dokunmaz. Trigger (index.js) bunları kullanır.

// Firestore doküman yolunda '/' bir segment ayırıcıdır. Bir id'de '/' varsa
// db.doc(...) yol hatası verir ve transaction reddetme yazamadan çöker (bkz. V8).
function isSafeId(value) {
  return typeof value === "string" && value.length > 0 && !value.includes("/");
}

// Slot saatleri Türkiye saatiyle (UTC+3, yaz saati uygulaması yok) yorumlanır.
const SLOT_TIME_ZONE_OFFSET = "+03:00";

/**
 * Slot'un başlangıç anını epoch milisaniye olarak döndürür.
 * date: "yyyy-MM-dd", startTime: "HH:mm". Geçersizse NaN.
 */
function slotStartMs(date, startTime) {
  if (typeof date !== "string" || typeof startTime !== "string") return NaN;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || !/^\d{2}:\d{2}$/.test(startTime)) return NaN;
  return Date.parse(`${date}T${startTime}:00${SLOT_TIME_ZONE_OFFSET}`);
}

/**
 * Slot başlangıcı şu andan önce (ya da tam o an) ise geçmiştir.
 * Tarih okunamıyorsa geçmiş sayılmaz; bu durum ayrıca INVALID_SLOT_TIME ile raporlanır.
 */
function isPastSlot(date, startTime, nowMs) {
  const startMs = slotStartMs(date, startTime);
  return Number.isFinite(startMs) && startMs <= nowMs;
}

module.exports = { isSafeId, slotStartMs, isPastSlot };
