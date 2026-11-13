# TASK.md

Görev durumları: `[ ]` yapılmadı · `[~]` kısmen yapıldı · `[x]` tamamlandı

## Mevcut kod durumu — kod yazıldı, geçici kopyada derlendi, çalıştırılmadı

Aşağıdaki parçalar kodda mevcut ve `assembleDebug` ile (sahte `google-services.json` kullanan geçici kopyada) derlendi; hiçbiri gerçek bir Firebase projesine karşı veya cihazda çalıştırılmadı.

- [~] **Veri katmanı (okuma)** — `KuaforRepository`: `getShops`, `getShop`,
      `getBarbers`, `getServices`, `getAvailableSlots`, `getMyAppointments`.
      Kod yazıldı, derlendi; bunları çağıran ekran henüz yok.
- [~] **Çakışmasız randevu** — `KuaforRepository.bookAppointment()` transaction'ı
      ve `firestore.rules` içindeki slot/appointment kuralları. Kod yazıldı,
      derlendi. Kurallar `tests/rules` ile, çift rezervasyon engeli
      `functions/emulator` testiyle emülatörde doğrulandı; gerçek istemciyle
      eşzamanlı rezervasyon testi yapılmadı.
- [~] **BookingViewModel** — `load()` ve `bookSlot()`. Kod yazıldı, derlendi;
      bağlı `BookingScreen` henüz yok.
- [~] **Veri modelleri** — `Shop`, `Barber`, `Service`, `TimeSlot`,
      `Appointment`, `Review`. Kod yazıldı, derlendi. (`User` modeli yok.)
- [~] **Proje iskeleti** — Gradle (`build.gradle.kts`, `gradle/libs.versions.toml`,
      Gradle wrapper 8.10.2), `AndroidManifest.xml`, `MainActivity`,
      `KuaforApplication`, `FirebaseModule`, tema dosyaları, `Screen` ve
      placeholder'lı `NavGraph`. Kod yazıldı, derlendi; `values/themes.xml` ve
      launcher ikonu repoda yok.

## Kritik — uygulama çalışmadan önce şart

- [~] **Firebase projesi bağlama** — Firebase Console'da proje oluşturuldu
      mu belirsiz; `google-services.json` henüz `app/` klasörüne
      eklenmedi (doğrulandı: dosya repoda yok). Bu olmadan Auth/Firestore
      hiç çalışmaz. Login kodu sahte bir `google-services.json` ile geçici
      kopyada derlendi; gerçek dosya ile derleme ve çalıştırma yapılmadı.
- [~] **Login / Phone OTP ekranı** — Kod yazıldı, derlendi (geçici kopyada,
      sahte `google-services.json` ile), cihazda test edilmedi. Kapsam:
      `AuthRepository`, `AuthViewModel`, `LoginScreen`, `NavGraph` Splash
      oturum kontrolü (oturum yoksa Login'e, varsa Home'a yönlendirme).
      Açık kalan: "Kodu tekrar gönder" yok (`ForceResendingToken` saklanmıyor).
      (`bookAppointment` oturum yoksa `NotSignedInException` fırlatıyor.)
- [ ] **Firebase Console: Phone Auth** — Authentication içinde Phone
      sağlayıcısı etkinleştirilmeli. Durum doğrulanmalı (konsolda kontrol
      edilmedi).
- [ ] **Firebase Console: SHA-256 parmak izi** — Uygulamanın debug ve release
      SHA-256 değerleri Firebase'e eklenmeli; Phone Auth için gerekli.
      Doğrulanmalı.
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
- [~] **`averageRating` güncelleme Cloud Function'ı** — `onReviewCreated`
      (`functions/index.js`) yazıldı: yorum oluşunca dükkanın
      `averageRating` ve `reviewCount` alanlarını transaction ile yeniden
      hesaplar. `functions` içindeki testler 16/16 geçti; emülatörde (`functions/emulator`) yorum sonrası ortalama doğrulandı. Deploy edilmedi. `addReview` ve `getReviews` henüz
      yazılmadı; `reviews` için yalnızca create kuralı var.
- [~] **Randevu doğrulama Cloud Function'ı** — `onAppointmentCreated`
      (`functions/index.js`) yeni randevuda slot (`isBooked`), hizmet ve
      fiyat (`totalPriceTL`) kontrolü yapar; geçersizse randevuyu
      `REJECTED` + `rejectReason` ile işaretler ve slot bu randevuya
      aitse kilidi (`isBooked = false`) açar. Trigger istemcinin yazmasından
      sonra çalışır (kuralla tam çözüm değil, bkz. `BACKLOG.md`). Aynı slota daha önce oluşturulmuş geçerli bir randevu varsa yenisini `SLOT_ALREADY_TAKEN` ile reddeder (çift rezervasyon). Emülatörde 7/7 doğrulandı. Deploy edilmedi.
- [~] **Firestore kuralları sıkılaştırma** — `appointments` ve `reviews`
      create kurallarında `keys().hasOnly(...)` ve alan tipi/aralık
      kontrolleri eklendi. `tests/rules`: 28 senaryo (25 normal, 3 `[AÇIK]`) geçiyor. Deploy edilmedi.
- [ ] **Cloud Functions ve kuralların deploy'u** — `functions/` ve
      `firestore.rules` gerçek Firebase projesine yüklenmeli. Kökte `firebase.json` (kurallar ve emülatör ayarları) var; proje bağlantısı için `.firebaserc` yok, `firebase use` ile oluşturulmalı.
- [ ] **Randevusuz slot kilidi temizliği** — Müşteri slotu `isBooked = true`
      yapıp randevu yazmadan kalırsa slot kalıcı olarak dolu görünür.
      Mevcut Function yalnızca reddedilen randevunun slotunu serbest bırakır;
      zamanlanmış temizlik işi (ör. Cloud Scheduler) yazılmadı.
- [ ] **Gizlilik politikası** — `docs/gizlilik-politikasi-taslak.md` taslak
      olarak hazır; yayınlanmadı. Hukuki inceleme gerekli.
- [ ] **Push bildirim / randevu hatırlatma** — Firebase Cloud Messaging
      bağımlılığı eklendi ama `FirebaseMessagingService` veya bildirim
      gönderen bir Cloud Function yazılmadı.
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
      test yazılmadı. (Firestore kuralları için `tests/rules` ve Cloud
      Function saf fonksiyonları için `functions/test` mevcut.)
- [x] `MainActivity` içindeki eski login TODO yorumu kaldırıldı
      (oturum yönlendirmesi `NavGraph` Splash'ında).
- [ ] `BookingScreen` henüz yazılmadı; `BookingViewModel.load()` tarih
      parametresi alıyor. Ekran yazılırken tarih seçici eklenip ileri
      günler de gösterilmeli.
- [ ] Çoklu şube desteği (Türkiye pazarındaki rakiplerin öne çıkan
      özelliği) — veri modelinde `Shop` tekil, zincir/şube ilişkisi yok.

## Notlar

Bu dosyayı güncel tutmak, her oturumda `SESSION.md`'ye yeni bir kayıt
eklemek kadar önemlidir. Bir görevi tamamladığında `[ ]`'i `[x]`'e çevir,
silme — geçmişi görmek gerekebilir.
