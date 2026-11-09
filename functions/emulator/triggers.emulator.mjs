// Emülatör üzerinde trigger testi. Çalıştırma: tests/rules/npm test değil,
// kökten: firebase emulators:exec --only firestore,functions --config firebase.json "node functions/emulator/triggers.emulator.mjs"
import { initializeApp } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";

initializeApp({ projectId: "demo-kuafor-triggers" });
const db = getFirestore();

const results = [];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
async function waitFor(fn, timeoutMs = 20000) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    const v = await fn();
    if (v) return v;
    await sleep(500);
  }
  return null;
}
function check(name, ok, detail = "") {
  results.push({ name, ok, detail });
}

// Tohum veriler
await db.doc("shops/s1").set({ ownerUid: "owner1", name: "Test", averageRating: 0, reviewCount: 0 });
await db.doc("shops/s1/services/svc1").set({ priceTL: 100, discountPercent: 10, name: "Saç" });
await db.doc("shops/s1/barbers/b1/slots/slot1").set({ isBooked: true, date: "2026-12-01", startTime: "10:00" });
await db.doc("shops/s1/barbers/b1/slots/slot2").set({ isBooked: true, date: "2026-12-01", startTime: "11:00" });
await db.doc("shops/s1/barbers/b1/slots/slot3").set({ isBooked: true, date: "2026-12-01", startTime: "12:00" });

const base = { customerUid: "c1", shopId: "s1", barberId: "b1", serviceId: "svc1", serviceName: "Saç", date: "2026-12-01", durationMinutes: 30, status: "BOOKED", createdAt: Timestamp.now() };

// 1) Geçerli randevu: fiyat 90 (100 - %10), slot kilitli -> BOOKED kalmalı
await db.doc("appointments/a1").set({ ...base, slotId: "slot1", startTime: "10:00", totalPriceTL: 90 });
await sleep(4000);
const a1 = (await db.doc("appointments/a1").get()).data();
check("geçerli randevu BOOKED kalır", a1.status === "BOOKED", `status=${a1.status} ${a1.rejectReason ?? ""}`);

// 2) Yanlış fiyat -> REJECTED PRICE_MISMATCH, slot2 kilidi açılır
await db.doc("appointments/a2").set({ ...base, slotId: "slot2", startTime: "11:00", totalPriceTL: 1 });
const a2 = await waitFor(async () => { const d = (await db.doc("appointments/a2").get()).data(); return d.status !== "BOOKED" ? d : null; });
check("yanlış fiyat REJECTED", a2 && a2.rejectReason === "PRICE_MISMATCH", JSON.stringify(a2));
const s2 = await waitFor(async () => { const d = (await db.doc("shops/s1/barbers/b1/slots/slot2").get()).data(); return d.isBooked === false ? d : null; });
check("reddedilen randevunun slot kilidi açılır", !!s2);

// 3) Var olmayan slot -> REJECTED SLOT_NOT_FOUND
await db.doc("appointments/a3").set({ ...base, slotId: "yok", startTime: "13:00", totalPriceTL: 90 });
const a3 = await waitFor(async () => { const d = (await db.doc("appointments/a3").get()).data(); return d.status !== "BOOKED" ? d : null; });
check("var olmayan slot REJECTED", a3 && a3.rejectReason === "SLOT_NOT_FOUND", JSON.stringify(a3));

// 4) Aynı slota ikinci geçerli randevu (slot1 zaten a1 ile dolu) -> REJECTED beklenir (ÇİFT REZERVASYON)
await db.doc("appointments/a4").set({ ...base, slotId: "slot1", startTime: "10:00", totalPriceTL: 90, createdAt: Timestamp.fromMillis(Date.now() + 1000) });
await sleep(5000);
const a4 = (await db.doc("appointments/a4").get()).data();
check("aynı slota ikinci randevu REJECTED (çift rezervasyon)", a4.status === "REJECTED", `status=${a4.status} ${a4.rejectReason ?? ""}`);
const a1again = (await db.doc("appointments/a1").get()).data();
check("ilk randevu (a1) çakışmada etkilenmez", a1again.status === "BOOKED");

// 5) Yorum -> averageRating ve reviewCount
await db.doc("shops/s1/reviews/r1").set({ shopId: "s1", barberId: "b1", authorUid: "c1", authorName: "A", rating: 4, comment: "", createdAt: Timestamp.now() });
await db.doc("shops/s1/reviews/r2").set({ shopId: "s1", barberId: "b1", authorUid: "c2", authorName: "B", rating: 2, comment: "", createdAt: Timestamp.now() });
const shop = await waitFor(async () => { const d = (await db.doc("shops/s1").get()).data(); return d.reviewCount === 2 ? d : null; });
check("yorum sonrası averageRating=3, reviewCount=2", shop && shop.averageRating === 3 && shop.reviewCount === 2, JSON.stringify(shop));

let fail = 0;
for (const r of results) {
  if (!r.ok) fail++;
  console.log(`${r.ok ? "PASS" : "FAIL"}  ${r.name}${r.ok ? "" : "  -> " + r.detail}`);
}
console.log(`\n${results.length - fail}/${results.length} beklenen sonuç`);
process.exit(fail ? 1 : 0);
