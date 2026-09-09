# CLAUDE.md

Bu dosya, bu repoda çalışan herhangi bir AI kod asistanının (Claude Code dahil)
projeye hızlıca bağlam kazanması için yazılmıştır. Kod üzerinde değişiklik
yapmadan önce bu dosyayı, `ARCHITECTURE.md`, `SESSION.md` ve `TASK.md`
dosyalarını oku.

## Proje nedir?

**coiffeur** — Android için native bir kuaför/berber randevu uygulaması.
Müşteri tarafında yakındaki kuaförü bulma, belirli bir berber/stilist seçme,
hizmet seçip gerçek zamanlı ve çakışmasız saat rezervasyonu yapma; kuaför
tarafında ise gelen randevuları görme akışlarını kapsar.

Proje, GitHub'daki birden fazla açık kaynak kuaför uygulaması incelenerek
("melez mimari" yaklaşımıyla) kuruldu. Kaynak projeler ve hangi özelliğin
nereden alındığı `ARCHITECTURE.md` içinde tablo halinde listelidir.

## Teknoloji yığını

- **Dil:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Mimari:** MVVM (StateFlow + ViewModel), tek yönlü veri akışı
- **DI:** Hilt
- **Backend:** Firebase (Auth — Phone OTP, Firestore, Storage, Cloud Messaging)
- **Konum:** Play Services Location + Maps Compose
- **Görsel yükleme:** Coil
- **Min SDK:** 24, **Target SDK:** 34

## Klasör yapısı

```
app/src/main/java/com/kuafor/app/
├── data/
│   ├── model/          # Shop, Barber, Service, TimeSlot, Appointment, Review
│   └── repository/     # KuaforRepository — tüm Firebase erişimi buradan geçer
├── di/                  # Hilt modülleri (FirebaseModule)
├── ui/
│   ├── navigation/      # NavGraph, Screen sealed class
│   ├── screens/         # Compose ekranları
│   ├── theme/           # Color, Theme
│   └── viewmodel/       # Ekran başına ViewModel
├── KuaforApplication.kt # @HiltAndroidApp
└── MainActivity.kt
```

## Kritik iş kuralı: çakışmasız randevu

Bu proje için en önemli teknik karar budur, değiştirilirken dikkatli
olunmalı: `KuaforRepository.bookAppointment()` fonksiyonu bir **Firestore
transaction** içinde çalışır. Slot dokümanı okunur, `isBooked == true` ise
işlem hata döndürür; değilse aynı transaction içinde hem slot
`isBooked = true` yapılır hem de randevu dokümanı oluşturulur. Bu, iki
kullanıcının aynı saati aynı anda almasını (çift rezervasyon) engeller.

Aynı kural `firestore.rules` içinde sunucu tarafında da zorunlu kılınmıştır:
slot dokümanının sadece `isBooked` alanı güncellenebilir, başka hiçbir alan
client'tan değiştirilemez.

**Bu mantığı değiştirecek her PR, transaction'ın atomikliğini bozmamalı.**

## Kod yazarken uyulacak kurallar

- Yeni Firebase erişimi eklerken repository katmanının dışına çıkma;
  ViewModel'ler doğrudan `FirebaseFirestore`/`FirebaseAuth` çağırmamalı.
- Yeni ekran eklerken `ui/navigation/NavGraph.kt` içindeki `Screen` sealed
  class'ına route ekle, string route'ları elle yazma.
- Para birimi alanları `...TL` suffix'i ile isimlendiriliyor (ör.
  `priceTL`, `totalPriceTL`) — tutarlılığı koru.
- Türkçe kullanıcı arayüzü metinleri, İngilizce kod/değişken isimleri
  standardı bu projede korunuyor.

## Henüz yapılmamış olanlar

Detaylı liste `TASK.md` içinde. Özetle: login/OTP ekranı, işletme yönetim
paneli (adisyon/kasa/prim), ödeme entegrasyonu, geohash bazlı ölçeklenebilir
konum sorgusu, push bildirim Cloud Function'ları.
