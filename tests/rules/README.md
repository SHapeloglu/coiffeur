# Firestore kural testleri

`firestore.rules` dosyasını Firebase Emulator üzerinde test eder.

## Gereksinimler
- Node.js 18+
- Java 17+ (Firestore emülatörü için; `java -version` PATH üzerinde çalışmalı)

## Çalıştırma
```
cd tests/rules
npm install
npm test
```

Test dosyası iki tür sonuç içerir:
- Normal senaryolar: kuralın beklenen şekilde izin verdiği ya da reddettiği durumlar.
- `[AÇIK]` ile işaretli senaryolar: bilinen güvenlik boşlukları. Bu testler şu an *izin veriliyor* sonucunu bekler; boşluk kapatıldığında bu beklenti değiştirilmeli.

## Senaryo sayısı
Toplam **28** senaryo (25 normal, 3 `[AÇIK]`). Beklenen sonuç: `28/28 beklenen sonuç`.

Alan düzeyi kurallar (`appointments` ve `reviews` create):
- `keys().hasOnly(...)` ile yalnızca tanımlı alanlara izin verilir; fazla alan reddedilir.
- Opsiyonel alanlar: alan yazılmışsa tipi doğrulanır, yazılmamışsa geçer. Böylece `[AÇIK]` senaryoları gibi kısmi dokümanlar kabul edilmeye devam eder.
- `durationMinutes` 1..600, `totalPriceTL` sayı ve >= 0, review `comment` en fazla 1000 karakter.

Kapsam dışı: randevu–slot bağı, randevusuz slot kilidi ve fiyat doğrulaması kuralla çözülemez; bunlar `[AÇIK]` olarak kalır.
