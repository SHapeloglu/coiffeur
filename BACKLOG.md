# BACKLOG.md — coiffeur Fikir Havuzu

Önceliklendirilmiş işler `TASK.md`'de (kritik → yüksek → orta → düşük). Bu dosya henüz sıraya girmemiş fikirler içindir; bir fikir somutlaşınca `TASK.md`'ye taşınır.

> Not: Kotlin kaynak kodu yerelde mevcut (`app/`, `firestore.rules`, `functions/`, Gradle dosyaları) ancak henüz commit edilmedi. Login/OTP kodu geçici kopyada derlendi (cihazda test edilmedi); diğer kodların çoğu derlenmedi. Cloud Function ve kural testleri yalnızca saf fonksiyon ve `tests/rules` düzeyinde çalıştırıldı, deploy edilmedi. Aşağıdaki "Kodda eksik kalan TODO'lar" bölümü, kaynak koddaki gerçek TODO'lardan derlenmiştir.

## Kodda eksik kalan TODO'lar

Kod tabanında hiç karşılığı olmayan veya yalnızca yorum olarak duran öğeler. Sıraya girdiğinde `TASK.md`'ye taşınır.

- **Login / Phone OTP (kapatıldı, kalan küçük işler):** `AuthRepository`, `AuthViewModel`, `LoginScreen` ve `NavGraph` Splash oturum kontrolü yazıldı; derlendi, cihazda test edilmedi. Kalanlar: `MainActivity` içindeki eski TODO yorumu kaldırılmalı; `LoginScreen` "Kodu tekrar gönder" için `ForceResendingToken` saklanmıyor. (Bkz. `TASK.md` Kritik.)
- **Geohash / yakın dükkan sorgusu:** `getNearbyShops(lat, lng, radiusKm)` henüz yok; `KuaforRepository` tüm `shops` koleksiyonunu çekiyor. (`TASK.md` Yüksek.)
- **Yorum sistemi:** `getReviews(shopId)` ve `addReview(...)` yazılmadı. `Shop.averageRating` ve `reviewCount` için `onReviewCreated` Cloud Function'ı yazıldı (`functions/index.js`, deploy edilmedi). İstemci tarafı yorum ekleme/okuma hâlâ açık. `firestore.rules` `reviews` için yalnızca create izni veriyor.
- **Randevu iptali:** `cancelAppointment(appointmentId)` yok. `Appointment.status` için `CANCELLED` durumu tanımlı değil. İptal, slotu `isBooked = false` yapmalı; `firestore.rules` slot update kuralı bunu şu an yalnızca `false -> true` yönünde izin veriyor, kural genişletilmeli. Appointment `update` kuralı da kapalı.
- **Cloud Function'lar:** `onReviewCreated` (averageRating) ve `onAppointmentCreated` (randevu doğrulama) `functions/` içinde yazıldı, deploy edilmedi. Push bildirim/randevu hatırlatma için Function yok; Cloud Messaging bağımlılığı var ama `FirebaseMessagingService` yazılmadı.
- **Fiyat doğrulaması (kısmen çözüldü):** `bookAppointment()` `totalPriceTL` değerini client tarafında hesaplıyor (`Service.discountedPriceTL()`). `onAppointmentCreated` Cloud Function'ı bu fiyatı `services/{serviceId}` dokümanına karşı doğrular ve uyuşmazsa randevuyu `REJECTED` (`PRICE_MISMATCH`) yapar; trigger tabanlı olduğu için istemci yazdıktan sonra çalışır. Kural tarafında `get()` ile fiyat kontrolü yok; `tests/rules` içinde `[AÇIK]` olarak duruyor.
- **Konum izni ve harita:** Manifest'te konum izinleri var; izin isteği ve `FusedLocationProviderClient` kullanımı yok. Google Maps API anahtarı manifest'te yorum satırı olarak duruyor, `local.properties` / secrets üzerinden eklenmedi.
- **Launcher icon:** `AndroidManifest.xml`'de `android:icon` referansı kaldırıldı; gerçek ikon eklenmedi.
- **Firestore seed verisi:** Repoda başlangıç verisi (shop/service/barber/slot) veya seed script yok.
- **Randevu–slot bağı (çift rezervasyon Cloud Function ile engellendi, emülatörde doğrulandı):** Aynı slota ikinci geçerli randevu `SLOT_ALREADY_TAKEN` ile reddediliyor (`functions/test/triggers.emulator.mjs`, 7/7). Eski açıklama: `onAppointmentCreated` Cloud Function'ı, randevunun `slotId`'sinin slotta `isBooked = true` olup olmadığını ve slota bağlı başka geçerli randevu olup olmadığını kontrol eder; geçersiz randevuyu `REJECTED` yapar. Trigger istemci yazdıktan sonra çalıştığı için kural tabanlı tam çözüm değildir; değiştirilmiş bir istemci slot güncellemesini atlayıp randevu yazabilir ve bu, kuralda hâlâ `[AÇIK]`. Appointment create kuralında alan/tip kısıtları (`keys().hasOnly`, `slotId` ve diğer alan tipleri) eklendi.
- **Randevusuz slot kilidi (açık):** Müşteri slotu `isBooked = true` yapıp randevu oluşturmadan bırakabilir ve slot kalıcı olarak dolu görünür. `onAppointmentCreated` yalnızca reddedilen randevunun slotunu serbest bırakır; randevu hiç yazılmayan kilitler için zamanlanmış temizlik işi (ör. Cloud Scheduler) henüz yazılmadı. İptal akışıyla birlikte bu da ele alınmalı.
- **`isBooked` alanı eksik slotlar:** Firestore eşitlik sorgusu (`whereEqualTo("isBooked", false)`) ve kural `resource.data.isBooked == false` kontrolü, alanı olmayan slot dokümanlarında eşleşmez/hata verir. Slot oluşturma akışında `isBooked: false` zorunlu olmalı.
- **Transaction hata mesajı:** `toUserMessage()` yalnızca doğrudan `KuaforException` alt sınıflarını tanıyor. Firestore SDK'nın transaction lambda'sından fırlatılan kullanıcı istisnasını sarıp sarmadığı doğrulanmadı; sarıyorsa "Seçilen saat az önce doldu" mesajı kaybolur. Bir test veya SDK kaynağıyla doğrulanmalı.

## Ürün / pazar (ARCHITECTURE.md "Pazar konumlandırması"ndan)

- **Gelir modeli denemeleri:** düşük/sıfır abonelik + SMS/bildirim kredisi (RandevuNet yaklaşımı) — Fresha tarzı yeni müşteri komisyonu yerel pazarda kabul görmüyor.
- **Bekleme listesi:** dolu slot için "yer açılırsa haber ver".
- **Son dakika indirimleri:** boş kalan slotlara otomatik indirim + bildirim.
- **Sadakat:** N. randevuda indirim / puan.
- **Paylaşılabilir işletme profili linki** (WhatsApp/Instagram'dan doğrudan randevu).
- **iOS sürümü** (Compose Multiplatform veya ayrı SwiftUI) — Android doğrulandıktan sonra.

## Teknik

- `getNearbyShops()` için geohash sorgusu (TASK'ta yüksek öncelik) sonrası: bölge bazlı önbellek ve sayfalama.
- Firestore güvenlik kurallarının emülatörle otomatik testi — `tests/rules` ile yapıldı (27 senaryo, 3'ü `[AÇIK]`); CI'a bağlanması açık.
- Çakışmasız slot transaction'ı için yük testi (aynı slota eşzamanlı istek).
- Çevrimdışı mod: son görüntülenen dükkanlar ve kullanıcının randevuları önbellekte.

## Kapsam dışı (bilinçli karar)

Adisyon, kasa, prim/bordro gibi işletme yönetim (B2B ERP) özellikleri — rakiplerin alanı, coiffeur tüketici tarafı keşif deneyimine odaklı.

## Ekleme Şablonu

```markdown
### Başlık
- **Kategori:** yeni özellik / iyileştirme / teknik borç / araştırma
- **Neden:** kısa gerekçe
- **Notlar:** büyüklük, bağımlılıklar, riskler
```
