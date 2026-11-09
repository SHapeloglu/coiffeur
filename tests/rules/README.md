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
