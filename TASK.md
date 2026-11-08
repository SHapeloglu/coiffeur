# TASK.md

Görev durumları: `[ ]` yapılmadı · `[~]` kısmen yapıldı · `[x]` tamamlandı

## Mevcut kod durumu — kod yazıldı, derlenmedi

Aşağıdaki parçalar kodda mevcut ama hiçbiri derlenmedi ve gerçek Firebase projesine karşı test edilmedi.

- [~] **Veri katmanı (okuma)** — `KuaforRepository`: `getShops`, `getShop`,
      `getBarbers`, `getServices`, `getAvailableSlots`, `getMyAppointments`.
      Kod yazıldı, derlenmedi; bunları çağıran ekran henüz yok.
- [~] **Çakışmasız randevu** — `KuaforRepository.bookAppointment()` transaction'ı
      ve `firestore.rules` içindeki slot/appointment kuralları. Kod yazıldı,
      derlenmedi; eşzamanlı rezervasyon testi yapılmadı.
- [~] **BookingViewModel** — `load()` ve `bookSlot()`. Kod yazıldı, derlenmedi;
      bağlı `BookingScreen` henüz yok.
- [~] **Veri modelleri** — `Shop`, `Barber`, `Service`, `TimeSlot`,
      `Appointment`, `Review`. Kod yazıldı, derlenmedi. (`User` modeli yok.)
- [~] **Proje iskeleti** — Gradle (`build.gradle.kts`, `gradle/libs.versions.toml`),
      `AndroidManifest.xml`, `MainActivity`, `KuaforApplication`, `FirebaseModule`,
      tema dosyaları, `Screen` ve placeholder'lı `NavGraph`. Kod yazıldı,
      derlenmedi; `gradle/wrapper/` ve `values/themes.xml` repoda yok.

## Kritik — uygulama çalışmadan önce şart

- [~] **Firebase projesi bağlama** — Firebase Console'da proje oluşturuldu
      mu belirsiz; `google-services.json` henüz `app/` klasörüne
      eklenmedi (doğrulandı: dosya repoda yok). Bu olmadan Auth/Firestore
      hiç çalışmaz.
- [ ] **Login / Phone OTP ekranı** — `MainActivity` içinde oturum kontrolü
      yok (TODO). `NavGraph`'taki Splash de `FirebaseAuth` kontrolü yapmıyor.
      Gerçek `FirebaseAuth` Phone OTP akışı (kaynak: zeryab-tahir/Barber-App)
      yazılmalı, giriş yapmamış kullanıcı `NavGraph`'a girmeden login
      ekranına yönlendirilmeli. (`bookAppointment` oturum yoksa
      `NotSignedInException` fırlatıyor.)
- [ ] **Firestore'a başlangıç verisi** — En az 1 `shop`, birkaç
      `service`, `barber` ve `slot` dokümanı elle veya bir seed script
      ile eklenmeli, aksi halde tüm ekranlar boş görünür. (Repoda seed
      script yok.)
- [ ] **Gerçek konum izni akışı** — `ACCESS_FINE_LOCATION` ve
      `ACCESS_COARSE_LOCATION` izinleri `AndroidManifest.xml`'de tanımlı,
      ama kodda konum alan bir yer yok. İzin isteği ve
      `FusedLocationProviderClient` ile gerçek konum alma yazılmalı.

## Yüksek öncelik

- [ ] **Kuaför/işletme onboarding akışı** — Yeni bir dükkan sahibinin
      `shop`, `service`, `barber` ve `slot` dokümanlarını uygulama
      içinden oluşturabilmesi gerekiyor; şu an bunlar sadece Firestore
      Console'dan elle eklenebiliyor.
- [ ] **`averageRating` güncelleme Cloud Function'ı** — `addReview()`
      çağrıldığında dükkanın ortalama puanını client değil, bir Cloud
      Function güncellemeli (race condition riski, bkz.
      `ARCHITECTURE.md`). (`addReview` ve `getReviews` henüz yazılmadı;
      `reviews` için yalnızca create kuralı var.)
- [ ] **Push bildirim / randevu hatırlatma** — Firebase Cloud
      Messaging bağımlılığı eklendi ama hiçbir Cloud Function veya
      client-side handler yazılmadı.
- [ ] **Geohash bazlı konum sorgusu** — `getNearbyShops()` henüz yazılmadı
      (TODO); mevcut `getShops()` tüm `shops` koleksiyonunu çekiyor, dükkan
      sayısı büyüdükçe maliyetli. GeoFirestore veya benzeri bir çözüme
      geçilmeli.

## Orta öncelik

- [ ] **Ödeme entegrasyonu** — Sadece fiyat/indirim modeli var
      (`Service.discountPercent`), gerçek bir ödeme sağlayıcısı
      (iyzico, Stripe vb.) bağlanmadı.
- [ ] **İşletme yönetim özellikleri** — Türkiye pazarındaki rakiplerin
      (Salon Randevu, AtlasPlan) güçlü olduğu adisyon, kasa takibi, prim
      hesaplama gibi özellikler kapsam dışı bırakıldı; pazar analizine
      göre bunlar olmadan salon sahiplerini ikna etmek zor olabilir —
      ürün kararı gerekiyor.
- [ ] **Gelir modeli netleştirme** — Fresha tarzı komisyon mu,
      RandevuNet tarzı abonelik+SMS kredisi mi kullanılacak, teknik
      olarak faturalama/komisyon hesaplama mantığı henüz hiç yazılmadı.
- [ ] **Launcher icon** — `AndroidManifest.xml`'den `android:icon`
      referansı kaldırıldı (eksik dosya derleme hatası vermesin diye);
      gerçek bir uygulama ikonu eklenip manifest'e geri bağlanmalı.

## Düşük öncelik / iyileştirme

- [ ] Unit test kapsamı — repository ve viewmodel katmanları için hiç
      test yazılmadı.
- [ ] `BookingScreen` henüz yazılmadı; `BookingViewModel.load()` tarih
      parametresi alıyor. Ekran yazılırken tarih seçici eklenip ileri
      günler de gösterilmeli.
- [ ] Çoklu şube desteği (Türkiye pazarındaki rakiplerin öne çıkan
      özelliği) — veri modelinde `Shop` tekil, zincir/şube ilişkisi yok.

## Notlar

Bu dosyayı güncel tutmak, her oturumda `SESSION.md`'ye yeni bir kayıt
eklemek kadar önemlidir. Bir görevi tamamladığında `[ ]`'i `[x]`'e çevir,
silme — geçmişi görmek gerekebilir.
