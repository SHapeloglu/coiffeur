import { initializeTestEnvironment, assertSucceeds, assertFails } from "@firebase/rules-unit-testing";
import { readFileSync } from "node:fs";
import { doc, setDoc, updateDoc, getDoc } from "firebase/firestore";

const env = await initializeTestEnvironment({
  projectId: "demo-kuafor-rules",
  firestore: { rules: readFileSync(new URL("../../firestore.rules", import.meta.url), "utf8"), host: "127.0.0.1", port: 8080 },
});

const results = [];
async function check(name, expectAllowed, fn) {
  try {
    if (expectAllowed) await assertSucceeds(fn()); else await assertFails(fn());
    results.push({ name, ok: true, expectAllowed });
  } catch (e) {
    results.push({ name, ok: false, expectAllowed, err: e.message });
  }
}

await env.withSecurityRulesDisabled(async (ctx) => {
  const db = ctx.firestore();
  await setDoc(doc(db, "shops/s1"), { ownerUid: "owner1", name: "Test" });
  await setDoc(doc(db, "shops/s1/barbers/b1"), { name: "Ali" });
  await setDoc(doc(db, "shops/s1/barbers/b1/slots/slot1"), { isBooked: false, startTime: "10:00", date: "2026-12-01" });
  await setDoc(doc(db, "shops/s1/barbers/b1/slots/slot2"), { isBooked: false, startTime: "11:00", date: "2026-12-01" });
  await setDoc(doc(db, "shops/s1/barbers/b1/slots/slot3"), { isBooked: true, startTime: "12:00", date: "2026-12-01" });
  await setDoc(doc(db, "shops/s1/barbers/b1/slots/slot4"), { isBooked: false, startTime: "13:00", date: "2026-12-01" });
});

const customer = env.authenticatedContext("cust1").firestore();
const anon = env.unauthenticatedContext().firestore();
const slotPath = (id) => doc(customer, `shops/s1/barbers/b1/slots/${id}`);

// Slot kuralları (bookAppointment transaction'ının dayandığı kural)
await check("slot false->true (sadece isBooked) izinli", true,
  () => updateDoc(slotPath("slot1"), { isBooked: true }));
await check("slot güncellemesi başka alanı da değiştirirse reddedilir", false,
  () => updateDoc(slotPath("slot2"), { isBooked: true, startTime: "00:00" }));
await check("dolu slot (isBooked=true) tekrar alınamaz", false,
  () => updateDoc(slotPath("slot3"), { isBooked: true }));
await check("slot true->false (iptal) reddedilir", false,
  () => updateDoc(slotPath("slot3"), { isBooked: false }));

// Randevu kuralları
await check("kendi randevusunu oluşturabilir (BOOKED)", true,
  () => setDoc(doc(customer, "appointments/a1"), { customerUid: "cust1", status: "BOOKED", slotId: "slot1", shopId: "s1", barberId: "b1", date: "2026-12-01", startTime: "10:00", totalPriceTL: 100 }));
await check("başkası adına randevu oluşturamaz", false,
  () => setDoc(doc(customer, "appointments/a2"), { customerUid: "other", status: "BOOKED", slotId: "slot4" }));
await check("status BOOKED değilse randevu oluşturulamaz", false,
  () => setDoc(doc(customer, "appointments/a3"), { customerUid: "cust1", status: "CANCELLED", slotId: "slot4" }));

// BİLİNEN AÇIKLAR: bu testler mevcut kuralların zafiyetini kanıtlar (true = açık var)
await check("[AÇIK] slot almadan randevu yazılabiliyor (slot bağı yok)", true,
  () => setDoc(doc(customer, "appointments/a4"), { customerUid: "cust1", status: "BOOKED", slotId: "SAHTE_SLOT", totalPriceTL: 1 }));
await check("[AÇIK] randevu yazmadan slot kilitlenebiliyor", true,
  () => updateDoc(slotPath("slot4"), { isBooked: true }));
await check("[AÇIK] fiyat doğrulaması yok: totalPriceTL=1 yazılabiliyor", true,
  () => setDoc(doc(customer, "appointments/a5"), { customerUid: "cust1", status: "BOOKED", slotId: "x", totalPriceTL: 1 }));

// Diğer kurallar
await check("anonim kullanıcı dükkanları okuyamaz", false,
  () => getDoc(doc(anon, "shops/s1")));
await check("oturumlu kullanıcı dükkanları okuyabilir", true,
  () => getDoc(doc(customer, "shops/s1")));
await check("review puanı 3 izinli", true,
  () => setDoc(doc(customer, "shops/s1/reviews/r1"), { authorUid: "cust1", rating: 3 }));
await check("review puanı 6 reddedilir", false,
  () => setDoc(doc(customer, "shops/s1/reviews/r2"), { authorUid: "cust1", rating: 6 }));
await check("review puanı 0 reddedilir", false,
  () => setDoc(doc(customer, "shops/s1/reviews/r3"), { authorUid: "cust1", rating: 0 }));
await check("review puanı string ise reddedilir", false,
  () => setDoc(doc(customer, "shops/s1/reviews/r4"), { authorUid: "cust1", rating: "5" }));
await check("başkasının randevusunu okuyamaz", false,
  () => getDoc(doc(env.authenticatedContext("other").firestore(), "appointments/a1")));

await env.cleanup();
let fail = 0;
for (const r of results) {
  const mark = r.ok ? "PASS" : "FAIL";
  if (!r.ok) fail++;
  console.log(`${mark}  ${r.name}${r.ok ? "" : "  -> " + (r.err || "").split("\n")[0]}`);
}
console.log(`\n${results.length - fail}/${results.length} beklenen sonuç`);
process.exit(fail ? 1 : 0);
