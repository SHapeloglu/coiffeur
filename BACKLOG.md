# BACKLOG.md — coiffeur Fikir Havuzu

Önceliklendirilmiş işler `TASK.md`'de (kritik → yüksek → orta → düşük). Bu dosya henüz sıraya girmemiş fikirler içindir; bir fikir somutlaşınca `TASK.md`'ye taşınır.

> Not: Kotlin kaynak kodu yerelde mevcut (`app/`, `firestore.rules`, Gradle dosyaları) ancak henüz commit edilmedi; derlenmedi. Aşağıdaki "Kodda eksik kalan TODO'lar" bölümü, kaynak koddaki gerçek TODO'lardan derlenmiştir.

## Kodda eksik kalan TODO'lar

Kod tabanında hiç karşılığı olmayan veya yalnızca yorum olarak duran öğeler. Sıraya girdiğinde `TASK.md`'ye taşınır.

- **Login / Phone OTP:** `MainActivity` ve `NavGraph` Splash'ında oturum kontrolü yok (TODO). Oturum yoksa giriş ekranına yönlendirme ve FirebaseAuth Phone OTP akışı yazılmadı. (Bkz. `TASK.md` Kritik.)
- **Geohash / yakın dükkan sorgusu:** `getNearbyShops(lat, lng, radiusKm)` henüz yok; `KuaforRepository` tüm `shops` koleksiyonunu çekiyor. (`TASK.md` Yüksek.)
- **Yorum sistemi:** `getReviews(shopId)` ve `addReview(...)` yazılmadı. `Shop.averageRating` alanı client'tan güncellenmemeli; Cloud Function gerekli. `firestore.rules` `reviews` için yalnızca create izni veriyor.
- **Randevu iptali:** `cancelAppointment(appointmentId)` yok. `Appointment.status` için `CANCELLED` durumu tanımlı değil. İptal, slotu `isBooked = false` yapmalı; `firestore.rules` slot update kuralı bunu şu an yalnızca `false -> true` yönünde izin veriyor, kural genişletilmeli. Appointment `update` kuralı da kapalı.
- **Cloud Function'lar:** `averageRating` hesabı ve push bildirim/randevu hatırlatma için Function yok. Cloud Messaging bağımlılığı var ama `FirebaseMessagingService` yazılmadı.
- **Fiyat doğrulaması:** `bookAppointment()` `totalPriceTL` değerini client tarafında hesaplıyor (`Service.discountedPriceTL()`). `firestore.rules` appointment `create` kuralı bu değeri `services/{serviceId}` dokümanına karşı doğrulamıyor; istemci keyfi fiyat yazabilir. Kuralda `get()` ile fiyat/indirim kontrolü veya Cloud Function ile hesaplama gerekli.
- **Konum izni ve harita:** Manifest'te konum izinleri var; izin isteği ve `FusedLocationProviderClient` kullanımı yok. Google Maps API anahtarı manifest'te yorum satırı olarak duruyor, `local.properties` / secrets üzerinden eklenmedi.
- **Launcher icon:** `AndroidManifest.xml`'de `android:icon` referansı kaldırıldı; gerçek ikon eklenmedi.
- **Firestore seed verisi:** Repoda başlangıç verisi (shop/service/barber/slot) veya seed script yok.
- **Randevu–slot bağı (kural boşluğu):** `appointments` create kuralı `slotId`'yi slot'un gerçekten `isBooked = true` yapıldığına bağlamıyor; değiştirilmiş bir istemci slot güncellemesini atlayıp randevu yazabilir. Firestore kuralları aynı transaction'daki yazmaları `get()` ile göremediği için tam çözüm Cloud Function veya sunucu doğrulaması gerektirir. Ayrıca create kuralında alan/tip kısıtları (`keys().hasOnly`, `slotId` tipi) eksik.
- **Randevusuz slot kilidi:** Müşteri slotu `isBooked = true` yapıp randevu oluşturmadan bırakabilir ve slot kalıcı olarak dolu görünür. İptal akışıyla birlikte, randevu ile slot eşleşmesi sunucu tarafında zorunlu kılınmalı.
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
- Firestore güvenlik kurallarının emülatörle otomatik testi.
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
