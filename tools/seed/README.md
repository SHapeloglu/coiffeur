# Emülatör seed verisi

`seed.mjs`, Firestore emülatörüne demo verisi yazar: 2 dükkan, her dükkanda 2 hizmet
ve 2 berber, her berber için yarından itibaren 3 gün (Türkiye saati) 10:00–18:00 arası
30 dakikalık slotlar. Tüm slotlar `isBooked: false` ile yazılır.

## Çalıştırma

Kökten (`/home/yeliz/coiffeur`):

```bash
firebase emulators:exec --only firestore --project demo-kuafor "node tools/seed/seed.mjs"
```

Java gerektirir (emülatör için). Bu makinede JDK 17 `~/.jdks/jdk-17.0.21+9` altında.

## Güvenlik

- `FIRESTORE_EMULATOR_HOST` tanımlı değilse script hiçbir şey yazmadan çıkar. Gerçek
  Firebase projesine yazamaz.
- Proje id'si `demo-kuafor` (`demo-` öneki). Gerçek proje id'si repoda yok (`.firebaserc` yok).
- `firebase-admin` paketi `functions/node_modules` üzerinden yüklenir; bu klasörde ayrı
  bir `package.json` yok.

## Tekrar çalıştırma

Idempotent: `set()` ile yazılır, aynı komut tekrar çalıştırılabilir. Emülatör her
kapanışta sıfırlandığı için genelde her çalıştırmada temiz veri oluşur.
