# ARCHITECTURE.md

## Genel bakış

coiffeur, katmanlı MVVM mimarisi kullanan native bir Android uygulamasıdır.
Veri akışı tek yönlüdür: `Repository → ViewModel (StateFlow) → Compose UI`.

```
┌─────────────────┐
│   ui/screens     │  Compose UI, sadece state okur / event fırlatır
└────────┬─────────┘
         │ collectAsState()
┌────────▼─────────┐
│  ui/viewmodel     │  StateFlow ile UI state tutar, iş mantığını tetikler
└────────┬─────────┘
         │ suspend fun çağrıları
┌────────▼─────────┐
│ data/repository   │  Tüm Firebase erişimi burada toplanır
└────────┬─────────┘
         │
┌────────▼─────────┐
│  Firebase         │  Auth, Firestore, Storage, Cloud Messaging
└───────────────────┘
```

## Firebase kullanım kapsamı ve durum

| Servis | Kapsam | Durum |
|---|---|---|
| Auth | Phone OTP ile giriş | Kod yazıldı (`AuthRepository`, `LoginScreen`), derlendi, cihazda test edilmedi; Phone Auth ve SHA-256 Firebase Console'da doğrulanmalı. `bookAppointment` oturum yoksa `NotSignedInException` fırlatır |
| Firestore | Tüm veri erişimi `KuaforRepository` üzerinden; slot transaction'ı; `firestore.rules` | Kod yazıldı; kurallar `tests/rules` ile test ediliyor (27 senaryo, 3'ü `[AÇIK]`); deploy edilmedi; gerçek Firebase projesine bağlı değil |
| Cloud Functions | `functions/`: randevu doğrulama (`onAppointmentCreated`), dükkan puanı (`onReviewCreated`) | Kod yazıldı, saf fonksiyon testleri geçti (16/16); deploy edilmedi, emülatörde çalıştırılmadı |
| Storage | `FirebaseModule` içinde sağlayıcı olarak tanımlı | Hiçbir yerde kullanılmıyor |
| Cloud Messaging | Push bildirim / randevu hatırlatma | Bağımlılık eklendi; `FirebaseMessagingService` veya bildirim gönderen Function yok |

`google-services.json` henüz `app/` klasörüne eklenmedi (`TASK.md`'de
`[~]`). Auth ve Firestore bu dosya olmadan çalışmaz; kurulum adımları için
bkz. `README.md` (Kurulum bölümü).

## Kaynak proje kıyaslaması ve melez mimari kararı

Proje, GitHub'da incelenen açık kaynak kuaför/berber uygulamalarının en
güçlü özellikleri birleştirilerek tasarlandı:

| Özellik | Kaynak proje | Neden alındı |
|---|---|---|
| Kotlin + Jetpack Compose | danielvilha/kotlin-barber-shop | En modern, sürdürülebilir native yaklaşım |
| Firebase Phone OTP girişi | zeryab-tahir/Barber-App | Türkiye'de SMS OTP kullanıcı için tanıdık akış |
| Konum bazlı yakın kuaför bulma | zeryab-tahir/Barber-App | Fresha/Booksy gibi global oyuncularda standart, Türkiye pazarında eksik |
| Gerçek zamanlı, çakışmasız slot sistemi | zeryab-tahir/Barber-App | Çift rezervasyon en kritik güven sorunu |
| Belirli berber/stilist seçimi | zeryab-tahir/Barber-App | Müşteri sadakati bu seçime bağlı |
| İki rollü yapı (Müşteri / Kuaför paneli) | moloke/Kotlin-Barber-App | Tek codebase'de hem müşteri hem işletme tarafı |
| İndirimli hizmet fiyatlandırma | dalpat98/Barber-Booking-App | Kampanya/indirim desteği |
| Puanlama / yorum sistemi | dalpat98 & antoni-codes (Flutter projeleri) | Marketplace güveni için gerekli |
| İş galerisi | antoni-codes/Barber_Booking-UI-Flutter_App | Berber seçiminde görsel kanıt |

## Pazar konumlandırması

Türkiye'deki mevcut oyuncular (Salon Randevu, AtlasPlan, RandevuNet,
Kuaförüm Yanımda) ağırlıklı olarak **B2B işletme yönetim yazılımı**
(adisyon, kasa, prim, çoklu şube) olarak konumlanmış; müşteri tarafı
keşif deneyimi zayıf. Global oyuncular (Fresha, Booksy, Vagaro) ise bunun
tersini iyi yapıyor ama Türkiye'ye özgü değil ve komisyon modelleri
(%20 yeni müşteri komisyonu vb.) yerel pazarda kabul görmemiş bir model.

**coiffeur'ün konumu:** Türkiye pazarında eksik olan tüketici-merkezli
keşif deneyimini (konum bazlı arama, belirli berber seçimi, puanlama)
sağlarken, gelir modelinde RandevuNet'in yerel pazarda kabul gören
"düşük/sıfır abonelik + bildirim kredisi" yaklaşımına yakın durmayı
hedefler. İşletme yönetim özellikleri (adisyon, kasa, bordro) kapsam
dışı bırakılmıştır — bkz. `TASK.md`.

## Veri modeli (Firestore koleksiyon şeması)

```
users/{uid}
shops/{shopId}
  services/{serviceId}
  barbers/{barberId}
    slots/{slotId}
  reviews/{reviewId}
appointments/{appointmentId}
```

## Güvenlik

`firestore.rules`, slot dokümanlarının sadece `isBooked` alanının
güncellenebileceğini zorunlu kılar; bu, çakışmasız randevu mantığının
sunucu tarafında da bypass edilememesini garanti eder. Randevu
dokümanları sadece kendi `customerUid`'i ile eşleşen kullanıcı
tarafından okunabilir/yazılabilir.

Kural, aynı transaction içindeki slot yazımını göremediği için randevu–slot
bağını ve fiyatı tek başına doğrulayamaz. Bu kontroller
`onAppointmentCreated` Cloud Function'ı (`functions/index.js`) tarafından
istemci yazdıktan sonra yapılır; geçersiz randevu `REJECTED` olarak işaretlenir.
Bu katman henüz deploy edilmedi.

## Bilinen ölçeklenme sınırları

- `getNearbyShops()` şu an tüm `shops` koleksiyonunu çekip client'ta
  mesafe hesabı yapıyor. Dükkan sayısı arttıkça bu maliyetli hale gelir;
  geohash tabanlı sorguya (ör. GeoFirestore) geçilmeli.
- `averageRating` client'tan güncellenmiyor (race condition riski);
  `onReviewCreated` Cloud Function'ı ile hesaplanıyor (deploy edilmedi).
