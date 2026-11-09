# SESSION.md

Bu dosya, projede yapılan çalışma oturumlarının kısa bir günlüğüdür.
Yeni bir AI asistan oturumu başladığında burayı okuyarak "daha önce ne
yapıldı, nerede kaldık" sorusuna hızlıca cevap bulabilirsin. Her oturum
sonunda yeni bir madde ekle, eskileri silme.

---

## Oturum 1 — İlk mimari kurulum

**Yapılanlar:**
- 6 açık kaynak kuaför/berber uygulaması incelendi (kaynak listesi
  `ARCHITECTURE.md`'de) ve özellik kıyaslaması yapıldı.
- Melez mimari kararlaştırıldı: Kotlin + Jetpack Compose + Firebase + Hilt.
- Veri modelleri oluşturuldu: `User`, `Shop`, `Barber`, `Service`,
  `TimeSlot`, `Appointment`, `Review`.
- `KuaforRepository` yazıldı: konum bazlı yakın dükkan arama, hizmet/berber
  listeleme, **Firestore transaction ile çakışmasız randevu oluşturma**,
  randevu iptali, değerlendirme ekleme.
- ViewModel katmanı: `NearbyShopsViewModel`, `BookingViewModel`,
  `MyAppointmentsViewModel`, `BarberDashboardViewModel`.
- Compose ekranları: `NearbyShopsScreen`, `BookingScreen`,
  `MyAppointmentsScreen`, `BarberDashboardScreen`.
- Navigasyon grafiği (`NavGraph.kt`) kuruldu.
- `firestore.rules` yazıldı — slot manipülasyonunu engelleyen kural dahil.

**Durum:** Proje Android Studio'da açıldı ama Gradle sync için gereken
temel dosyalar eksikti.

---

## Oturum 2 — Projeyi derlenebilir hale getirme

**Yapılanlar:**
- Eksik dosyalar eklendi: `settings.gradle.kts`,
  `gradle/wrapper/gradle-wrapper.properties`, `AndroidManifest.xml`,
  `MainActivity.kt`, `KuaforApplication.kt` (`@HiltAndroidApp`),
  `di/FirebaseModule.kt`, tema dosyaları (`Color.kt`, `Theme.kt`),
  `strings.xml`, `themes.xml`.
- Kullanıcıya Android Studio'da adım adım kurulum rehberi verildi:
  dosyaları projeye kopyala → `google-services.json` ekle → Gradle sync →
  emülatör/cihaz seç → çalıştır → Firestore'a test verisi ekle.

**Durum:** Proje şu an derlenebilir durumda ama **gerçek bir
`google-services.json` bağlanmadı** ve Firestore'da henüz test verisi yok.
Login/OTP ekranı yazılmadı — `MainActivity` sabit `demo-user-uid` ve sabit
İzmir konumu kullanıyor.

**Sıradaki adım:** `TASK.md`'ye bak.

---

## Oturum 3 — Sektör kıyaslaması ve repo dokümantasyonu

**Yapılanlar:**
- Türkiye pazarı (Salon Randevu, AtlasPlan, RandevuNet, Kuaförüm Yanımda,
  Zamanlı) ve global pazar (Fresha, Booksy, Vagaro, SQUIRE, GlossGenius)
  karşılaştırıldı; sonuçlar `ARCHITECTURE.md`'nin "Pazar konumlandırması"
  bölümüne eklendi.
- Proje `SHapeloglu/coiffeur` adıyla boş bir GitHub reposuna taşınmak
  üzere `CLAUDE.md`, `ARCHITECTURE.md`, `SESSION.md`, `TASK.md`
  dosyaları oluşturuldu.

**Durum:** Bu dört dosya henüz repoya push edilmedi.

**Sıradaki adım:** Kod dosyalarının da (önceki oturumlarda oluşturulan
`KuaforApp/` klasörü) bu repoya taşınması gerekiyor — şu an ayrı bir
zip olarak duruyor.

---

## Oturum 4 — 2026-11-08 — Çalışma dosyalarının kodla senkronlanması

**Yapılanlar:**
- Proje iskeleti (Gradle, manifest, `KuaforRepository`, `bookAppointment`
  transaction'ı, `firestore.rules`, Hilt, placeholder `NavGraph`, tema,
  `BookingViewModel`) kod okunarak incelendi.
- `TASK.md`: gerçek kod durumuna göre güncellendi; "kod yazıldı, derlenmedi"
  bölümü eklendi. Eski kayıtlarda geçen ama kodda bulunmayan öğeler
  düzeltildi (aşağıya bakın).
- `BACKLOG.md`: kodda eksik TODO'lar (login, geohash, yorum, iptal, Cloud
  Function, fiyat doğrulaması) listelendi.

**Doğrulanan bulgular (eski kayıtlarla çelişenler):**
- `google-services.json` `app/` altında yok.
- `MainActivity` artık `demo-user-uid` veya sabit İzmir koordinatı
  kullanmıyor; oturum kontrolü yok (TODO).
- Oturum 1'de adı geçen `NearbyShopsViewModel`, `MyAppointmentsViewModel`,
  `BarberDashboardViewModel` ile `NearbyShopsScreen`, `BookingScreen`,
  `MyAppointmentsScreen`, `BarberDashboardScreen` ve `User` modeli repoda
  yok. Ekranlar `NavGraph`'ta placeholder olarak duruyor.
- Oturum 2'de adı geçen `gradle/wrapper/gradle-wrapper.properties` ve
  `values/themes.xml` repoda yok.
- `getNearbyShops()` ve `addReview()` henüz yazılmadı (yalnızca TODO).

**Derleme:** Yapılmadı. Gradle sürümleri (`libs.versions.toml`) doğrulanmadı.

**Commit:** Yapılmadı. `app/`, `firestore.rules` ve Gradle dosyaları git'te
hâlâ untracked.

**Sıradaki adım:** `TASK.md` Kritik bölümü. Önce `google-services.json`
eklenip Gradle wrapper oluşturulmalı, ardından derleme denenmeli.

---

## Oturum 5 — 2026-11-09 — Login/OTP, Cloud Function, kural sıkılaştırma

**Yapılanlar:**
- Login / Phone OTP (SMS) akışı yazıldı: `data/repository/AuthRepository.kt`
  (`FirebaseAuth` erişiminin tek geçtiği yer), `ui/viewmodel/AuthViewModel.kt`,
  `ui/screens/LoginScreen.kt`. `NavGraph`'taki Splash oturum kontrolü ile
  oturum yoksa Login, varsa Home'a yönlendirme eklendi.
- Cloud Functions (`functions/`): `onAppointmentCreated` (slot, hizmet ve
  fiyat doğrulaması; geçersizse `REJECTED` + `rejectReason`, uygun durumda
  slot kilidini açar) ve `onReviewCreated` (dükkanın `averageRating` ve
  `reviewCount` alanlarını transaction ile hesaplar). Fiyat hesabı
  `functions/lib/pricing.js` içinde saf fonksiyon olarak ayrıldı.
- `firestore.rules` sıkılaştırıldı: `appointments` ve `reviews` create
  kurallarında `keys().hasOnly(...)`, alan tipi ve aralık kontrolleri.
- `tests/rules`: 27 senaryo (24 normal, 3 `[AÇIK]`) yazıldı.
- Gizlilik politikası taslağı: `docs/gizlilik-politikasi-taslak.md`
  (yayınlanmadı; hukuki inceleme gerekli).
- `TASK.md`, `BACKLOG.md`, `ARCHITECTURE.md` ve `README.md` bu duruma göre
  güncellendi.

**Derleme ve test sonuçları:**
- Kotlin derlemesi geçici bir kopyada başarılı oldu (sahte
  `google-services.json` ile). Gerçek proje dosyası ile derleme ve cihazda
  çalıştırma yapılmadı.
- `functions`: `npm test` ile saf fonksiyon testleri 16/16 geçti.
- `tests/rules`: 27 senaryo çalıştırıldı; sonuçlar beklenen durumda
  (3 `[AÇIK]` senaryo, bilinen boşlukların hâlâ açık olduğunu gösterir).

**Yarıda kalan ajanlar:** Sunucu yeniden başlatıldığı için bu oturumda
yarıda kalan ajan işleri yeniden yapıldı. Doğrulama için ilgili dosyalar
baştan okunup kontrol edildi.

**Açık kalanlar / doğrulanmalı:**
- Cihazda Login/OTP akışı test edilmedi.
- Firebase Console: Phone Auth sağlayıcısının etkin olup olmadığı
  doğrulanmalı; uygulamanın debug/release SHA-256 değerleri eklenmeli.
- `google-services.json` henüz `app/` altında yok.
- Cloud Functions ve `firestore.rules` deploy edilmedi; proje bağlantısı
  dosyalarının (`firebase.json`, `.firebaserc`) repoda olup olmadığı
  doğrulanmalı.
- Cloud Function'lar emülatörde çalıştırılmadı.
- `MainActivity` içindeki eski login TODO yorumu ve "Kodu tekrar gönder"
  için `ForceResendingToken` saklanması açık.
- Randevusuz slot kilidi için zamanlanmış temizlik işi yazılmadı.
- Gizlilik politikası yayınlanmadı.

**Commit:** Yapılmadı.

**Sıradaki adım:** Önce Firebase Console ayarları (Phone Auth, SHA-256,
`google-services.json`) yapılıp Login akışı gerçek cihazda denenmeli. Ardından
Cloud Functions ve kurallar emülatörde çalıştırılıp deploy edilmeli.
