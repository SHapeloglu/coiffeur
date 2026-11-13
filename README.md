# coiffeur

Android için native bir kuaför/berber randevu uygulaması. Müşteri
tarafında yakındaki kuaförü bulma, belirli bir berber/stilist seçme,
hizmet seçip gerçek zamanlı ve çakışmasız saat rezervasyonu yapma;
kuaför tarafında ise gelen randevuları görüp yönetme akışlarını kapsar.

Proje, GitHub'daki birden fazla açık kaynak kuaför uygulaması incelenerek
"melez mimari" yaklaşımıyla kuruldu — her birinin en güçlü özelliği
birleştirildi. Detaylar için `ARCHITECTURE.md`.

## Özellikler

- 📍 Konum bazlı yakındaki kuaförleri bulma
- 💈 Belirli bir berber/stilist seçerek randevu alma
- 🕐 Gerçek zamanlı, çakışmasız saat/slot sistemi (Firestore transaction)
- 💰 İndirimli hizmet fiyatlandırma
- ⭐ Puanlama / yorum sistemi
- 👥 İki rollü yapı: Müşteri ve Kuaför (işletme) paneli

## Teknoloji yığını

| Katman | Teknoloji |
|---|---|
| Dil | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Mimari | MVVM (StateFlow + ViewModel) |
| Bağımlılık enjeksiyonu | Hilt |
| Backend | Firebase (Auth, Firestore, Storage, Cloud Messaging) |
| Konum | Play Services Location, Maps Compose |
| Görsel yükleme | Coil |

Min SDK 24, Target SDK 34.

## Proje yapısı

```
app/src/main/java/com/kuafor/app/
├── data/
│   ├── model/          # Shop, Barber, Service, TimeSlot, Appointment, Review
│   └── repository/     # KuaforRepository — tüm Firebase erişimi buradan geçer
├── di/                  # Hilt modülleri
├── ui/
│   ├── navigation/      # NavGraph
│   ├── screens/         # Compose ekranları
│   ├── theme/           # Color, Theme
│   └── viewmodel/       # Ekran başına ViewModel
├── KuaforApplication.kt
└── MainActivity.kt
```

## Kurulum

1. Bu repoyu klonla ve Android Studio'da aç.
2. [Firebase Console](https://console.firebase.google.com)'da yeni bir
   proje oluştur, Android uygulaması ekle (paket adı: `com.kuafor.app`).
3. İndirdiğin `google-services.json` dosyasını `app/` klasörünün içine
   (`app/build.gradle.kts` ile aynı seviyeye) koy.
4. Gradle sync'i çalıştır (Android Studio otomatik önerecektir).
5. Firestore Database'i etkinleştir ve `firestore.rules` dosyasındaki
   kuralları yayınla.
6. Firestore'a en az bir test `shop`, `service`, `barber` ve `slot`
   dokümanı ekle (aksi halde ekranlar boş görünür).
7. Bir emülatör veya cihaz seçip ▶ Run'a bas.

Detaylı adım adım rehber ve olası hatalar için proje geçmişini
`SESSION.md` dosyasında bulabilirsin.

## Testler

- Firestore kuralları (Java 17+ ve Node.js 18+ gerekli): `cd tests/rules && npm install && npm test`
- Cloud Function saf fonksiyonları (Node.js 20): `cd functions && npm test`
- Cloud Function trigger'ları (emülatör; önce `cd functions && npm install`): `firebase emulators:exec --only firestore,functions --config firebase.json "node functions/emulator/triggers.emulator.mjs"` (firebase CLI için `tests/rules` içinde `npm install` ile gelen binary kullanılabilir)

Ayrıntılar için `tests/rules/README.md` dosyasına bak.

## Dokümantasyon

Bu repo, bir AI kod asistanıyla (Claude Code vb.) sürdürülebilir şekilde
çalışmak için aşağıdaki dosyaları içerir:

| Dosya | İçerik |
|---|---|
| `CLAUDE.md` | AI asistanı için proje bağlamı, kod kuralları, kritik iş mantığı |
| `ARCHITECTURE.md` | Sistem mimarisi, kaynak repo kıyaslaması, pazar konumlandırması |
| `SESSION.md` | Oturum geçmişi / devir notları |
| `TASK.md` | Öncelik sıralı görev listesi |

Yeni bir katkıda bulunan veya AI asistan oturumu başlamadan önce bu dört
dosyayı okumalı.

## Bilinen eksikler

Uygulama şu an derlenebilir durumda ama production için henüz hazır
değil. Login/OTP ekranı yazıldı ancak cihazda test edilmedi. Kritik
eksikler: gerçek Firebase bağlantısı ve Console ayarları, Cloud Function
ve kuralların deploy'u, işletme onboarding akışı, ödeme entegrasyonu.
Tam liste için `TASK.md`.

## Lisans

Henüz belirlenmedi.
