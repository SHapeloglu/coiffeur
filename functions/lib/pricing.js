"use strict";

// Saf fonksiyonlar: bağımlılık yok, Firebase'e dokunmaz. Kotlin tarafındaki
// Service.discountedPriceTL() ile aynı formülü (indirim 0-100 arasına sıkıştırılır) kullanır.

const TOLERANCE_TL = 0.01;

function round2(value) {
  return Math.round(value * 100) / 100;
}

/**
 * Beklenen toplam fiyat (TL): priceTL * (100 - discountPercent) / 100, 2 ondalığa yuvarlanır.
 */
function expectedTotalTL(priceTL, discountPercent) {
  const discount = Math.min(100, Math.max(0, discountPercent));
  return round2((priceTL * (100 - discount)) / 100);
}

/**
 * İstemcinin gönderdiği totalPriceTL, sunucunun hesapladığı beklenen fiyatla
 * tolerans (0.01 TL) içinde eşleşiyor mu?
 */
function isPriceValid(totalPriceTL, priceTL, discountPercent) {
  if (typeof totalPriceTL !== "number" || !Number.isFinite(totalPriceTL)) return false;
  const expected = expectedTotalTL(priceTL, discountPercent);
  if (!Number.isFinite(expected)) return false;
  // Kayan nokta hatasından kaçınmak için kuruş (tam sayı) üzerinden karşılaştır:
  // 0.01 TL = 1 kuruş, sınır değeri tam olarak kabul edilir.
  const diffCents = Math.abs(Math.round(totalPriceTL * 100) - Math.round(expected * 100));
  return diffCents <= Math.round(TOLERANCE_TL * 100);
}

/**
 * Puan listesinin ortalaması, 2 ondalığa yuvarlanır. Boş liste → 0.
 */
function averageRating(ratings) {
  if (ratings.length === 0) return 0;
  const sum = ratings.reduce((acc, r) => acc + r, 0);
  return round2(sum / ratings.length);
}

module.exports = { expectedTotalTL, isPriceValid, averageRating, TOLERANCE_TL };
