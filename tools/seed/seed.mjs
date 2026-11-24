// Firestore emülatörüne demo verisi yazar (shop, service, barber, slot).
// Çalıştırma (kökten): firebase emulators:exec --only firestore "node tools/seed/seed.mjs"
// Yalnızca emülatöre yazar: FIRESTORE_EMULATOR_HOST yoksa hiçbir şey yapmadan çıkar.
// Idempotent: set() ile yazılır, aynı komut tekrar çalıştırılabilir.

import { createRequire } from "node:module";
import path from "node:path";
import { fileURLToPath } from "node:url";

if (!process.env.FIRESTORE_EMULATOR_HOST) {
  console.error(
    "HATA: FIRESTORE_EMULATOR_HOST tanımlı değil. Seed yalnızca Firestore emülatörüne yazar.\n" +
      "Komut: firebase emulators:exec --only firestore \"node tools/seed/seed.mjs\""
  );
  process.exit(1);
}

// firebase-admin, functions/node_modules üzerinden yüklenir (bu klasörde package.json yok).
const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "..");
const require = createRequire(path.join(repoRoot, "functions", "package.json"));
const admin = require("firebase-admin");

// Gerçek bir proje id'si değil; "demo-" öneki emülatörde gerçek projeye bağlanmayı önler.
const PROJECT_ID = "demo-kuafor";
admin.initializeApp({ projectId: PROJECT_ID });
const db = admin.firestore();

// Yarından itibaren 3 gün (Türkiye saatiyle). Sabit tarih kullanılmaz: geçmiş slotlar
// sunucuda PAST_SLOT ile reddedilir (bkz. functions/index.js).
const DATE_FORMAT = new Intl.DateTimeFormat("en-CA", { timeZone: "Europe/Istanbul" });
const DATES = [1, 2, 3].map((d) => DATE_FORMAT.format(new Date(Date.now() + d * 86400000)));
const SLOT_MINUTES = 30;
const DAY_START = 10 * 60; // 10:00
const DAY_END = 18 * 60; // 18:00 (son slot 17:30 başlar)

const shops = [
  {
    id: "shop-demo-1",
    data: {
      name: "Demo Kuaför Kadıköy",
      address: "Caferağa Mah., Kadıköy, İstanbul",
      latitude: 40.9903,
      longitude: 29.0246,
      ownerUid: "demo-owner-1",
      averageRating: 0,
      reviewCount: 0,
      imageUrl: "",
      phone: "+905550000001",
    },
    services: [
      { id: "svc-saç-kesimi", name: "Saç Kesimi", description: "Yıkama ve şekillendirme dahil", durationMinutes: 30, priceTL: 350, discountPercent: 0 },
      { id: "svc-sakal-tıraşı", name: "Sakal Tıraşı", description: "Sıcak havlu uygulamalı", durationMinutes: 30, priceTL: 200, discountPercent: 10 },
    ],
    barbers: [
      { id: "barber-1a", name: "Ahmet Yılmaz", title: "Berber" },
      { id: "barber-1b", name: "Elif Kaya", title: "Stilist" },
    ],
  },
  {
    id: "shop-demo-2",
    data: {
      name: "Demo Berber Nişantaşı",
      address: "Teşvikiye Mah., Şişli, İstanbul",
      latitude: 41.0467,
      longitude: 28.9946,
      ownerUid: "demo-owner-2",
      averageRating: 0,
      reviewCount: 0,
      imageUrl: "",
      phone: "+905550000002",
    },
    services: [
      { id: "svc-fön", name: "Fön", description: "Kuru fön ve şekillendirme", durationMinutes: 30, priceTL: 250, discountPercent: 0 },
      { id: "svc-sakal-bakımı", name: "Sakal Bakımı", description: "Sakal şekillendirme ve nemlendirme", durationMinutes: 30, priceTL: 180, discountPercent: 15 },
    ],
    barbers: [
      { id: "barber-2a", name: "Mehmet Demir", title: "Berber" },
      { id: "barber-2b", name: "Zeynep Aydın", title: "Kuaför" },
    ],
  },
];

const fmtTime = (minutes) =>
  `${String(Math.floor(minutes / 60)).padStart(2, "0")}:${String(minutes % 60).padStart(2, "0")}`;

// Tüm yazmalar tek bir batch'te (limit 500 işlem); gerekirse parçalanır.
const ops = [];
const add = (ref, data) => ops.push({ ref, data });

let slotCount = 0;
for (const shop of shops) {
  add(db.doc(`shops/${shop.id}`), shop.data);

  for (const svc of shop.services) {
    add(db.doc(`shops/${shop.id}/services/${svc.id}`), { shopId: shop.id, ...svc });
  }

  for (const barber of shop.barbers) {
    add(db.doc(`shops/${shop.id}/barbers/${barber.id}`), {
      shopId: shop.id,
      name: barber.name,
      title: barber.title,
      avatarUrl: "",
      isActive: true,
    });

    for (const date of DATES) {
      for (let m = DAY_START; m + SLOT_MINUTES <= DAY_END; m += SLOT_MINUTES) {
        const startTime = fmtTime(m);
        const slotId = `${date}_${startTime.replace(":", "")}`;
        add(db.doc(`shops/${shop.id}/barbers/${barber.id}/slots/${slotId}`), {
          barberId: barber.id,
          date,
          startTime,
          isBooked: false,
        });
        slotCount++;
      }
    }
  }
}

const BATCH_LIMIT = 500;
for (let i = 0; i < ops.length; i += BATCH_LIMIT) {
  const batch = db.batch();
  for (const { ref, data } of ops.slice(i, i + BATCH_LIMIT)) {
    batch.set(ref, data);
  }
  await batch.commit();
}

const serviceCount = shops.reduce((n, s) => n + s.services.length, 0);
const barberCount = shops.reduce((n, s) => n + s.barbers.length, 0);
console.log(
  `Seed tamam (proje: ${PROJECT_ID}, emülatör: ${process.env.FIRESTORE_EMULATOR_HOST}): ` +
    `${shops.length} dükkan, ${serviceCount} hizmet, ${barberCount} berber, ${slotCount} slot.`
);

await admin.app().delete();
