# TASK.md

Görev durumları: `[ ]` yapılmadı · `[~]` kısmen yapıldı · `[x]` tamamlandı

## Kritik — uygulama çalışmadan önce şart

- [~] **Firebase projesi bağlama** — Firebase Console'da proje oluşturuldu
      mu belirsiz; `google-services.json` henüz `app/` klasörüne
      eklenmedi. Bu olmadan Auth/Firestore hiç çalışmaz.
- [ ] **Login / Phone OTP ekranı** — `MainActivity` şu an sabit
      `demo-user-uid` kullanıyor. Gerçek `FirebaseAuth` Phone OTP akışı
      (kaynak: zeryab-tahir/Barber-App) yazılmalı, giriş yapmamış
      kullanıcı `NavGraph`'a girmeden login ekranına yönlendirilmeli.
- [ ] **Firestore'a başlangıç verisi** — En az 1 `shop`, birkaç
      `service`, `barber` ve `slot` dokümanı elle veya bir seed script
      ile eklenmeli, aksi halde tüm ekranlar boş görünür.
- [ ] **Gerçek konum izni akışı** — `MainActivity`'deki sabit İzmir
      koordinatı yerine `ACCESS_FINE_LOCATION` izni istenip
      `FusedLocationProviderClient` ile gerçek konum alınmalı.

## Yüksek öncelik

- [ ] **Kuaför/işletme onboarding akışı** — Yeni bir dükkan sahibinin
      `shop`, `service`, `barber` ve `slot` dokümanlarını uygulama
      içinden oluşturabilmesi gerekiyor; şu an bunlar sadece Firestore
      Console'dan elle eklenebiliyor.
- [ ] **`averageRating` güncelleme Cloud Function'ı** — `addReview()`
      çağrıldığında dükkanın ortalama puanını client değil, bir Cloud
      Function güncellemeli (race condition riski, bkz.
      `ARCHITECTURE.md`).
- [ ] **Push bildirim / randevu hatırlatma** — Firebase Cloud
      Messaging bağımlılığı eklendi ama hiçbir Cloud Function veya
      client-side handler yazılmadı.
- [ ] **Geohash bazlı konum sorgusu** — `getNearbyShops()` şu an tüm
      koleksiyonu çekiyor; dükkan sayısı büyüdükçe maliyetli. GeoFirestore
      veya benzeri bir çözüme geçilmeli.

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
- [ ] `BookingScreen`'de sadece "bugün" için slot gösteriliyor —
      tarih seçici eklenip ileri günler de gösterilmeli.
- [ ] Çoklu şube desteği (Türkiye pazarındaki rakiplerin öne çıkan
      özelliği) — veri modelinde `Shop` tekil, zincir/şube ilişkisi yok.

## Notlar

Bu dosyayı güncel tutmak, her oturumda `SESSION.md`'ye yeni bir kayıt
eklemek kadar önemlidir. Bir görevi tamamladığında `[ ]`'i `[x]`'e çevir,
silme — geçmişi görmek gerekebilir.
