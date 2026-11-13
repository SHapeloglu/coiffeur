# Gizlilik Politikası — TASLAK

> **Bu bir taslaktır.** Yayınlanmadan önce hukuki inceleme yapılmalı (KVKK uyumu dahil). Köşeli parantez içindeki alanlar doldurulmadı.
> Taslak, mevcut kod tabanındaki veri modellerine ve manifest izinlerine göre yazıldı. Henüz yazılmamış özellikler (giriş, konum kullanımı, bildirimler) "planlanan" olarak işaretlendi ve yayından önce gözden geçirilmeli.

**Son güncelleme:** [tarih]
**Veri sorumlusu:** [şirket/kişi adı, adres]
**İletişim:** [e-posta]

---

## 1. Bu politika ne kapsar?

Bu politika, coiffeur mobil uygulamasının (Android) ve ilgili web sayfalarının kişisel verileri nasıl işlediğini açıklar. Uygulama, kullanıcıların yakındaki kuaför ve berberleri bulmasını, hizmet ve stilist seçip randevu almasını ve kuaförlerin gelen randevuları görmesini sağlar.

## 2. Hangi verileri topluyoruz?

| Veri | Kimden / nasıl | Amaç |
|---|---|---|
| Telefon numarası | Giriş sırasında kullanıcının girdiği numara (planlanan: SMS doğrulama) | Hesap oluşturma ve kimlik doğrulama |
| Firebase kullanıcı kimliği (uid) | Firebase Authentication tarafından oluşturulur | Randevu ve yorumların hesaba bağlanması |
| Ad / görünen ad | Yorum yazarken gösterilen isim | Yorumlarda kimlik gösterimi |
| Randevu bilgileri | Seçilen kuaför, berber, hizmet, tarih, saat, ücret | Randevunun oluşturulması ve gösterilmesi |
| Yorum ve puan | Kullanıcının kuaför hakkında yazdığı | Diğer kullanıcılara bilgi |
| Konum (izin verilirse) | Cihaz konumu (planlanan kullanım) | Yakındaki kuaförleri göstermek |
| Bildirim belirteci (planlanan) | Firebase Cloud Messaging | Randevu hatırlatma bildirimleri |

Uygulama, kuaför hesapları için kuaförün adres, telefon ve hizmet bilgilerini de tutar. Bu bilgiler dükkan sahibi tarafından girilir.

**Konum izni:** Uygulama konum izni ister (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`). Konum yalnızca yakındaki kuaförleri göstermek için kullanılacaktır. [Konumun sunucuya kaydedilip kaydedilmeyeceği netleştirilmeli.]

## 3. Verileri nasıl kullanıyoruz?

- Hesap oluşturma ve giriş yapma
- Randevu oluşturma, göstermek ve yönetmek
- Yorum ve puanları görüntülemek
- [Planlanan] Yakındaki kuaförleri bulmak ve randevu hatırlatmaları göndermek
- Hizmetin güvenliğini ve doğru çalışmasını sağlamak

Verileri reklam amacıyla kullanmıyoruz. [Doğrulanmalı: analitik / çökme raporlama araçları kullanılıyorsa eklenmeli.]

## 4. Verileri kimlerle paylaşıyoruz?

- **Google Firebase** (Authentication, Firestore, Storage, Cloud Messaging): verilerin barındırılması ve kimlik doğrulama. Veriler Firebase altyapısında işlenir.
- **Kuaförler:** Bir randevu oluşturulduğunda, randevunun ilgili kuaföre görünür hale gelmesi için randevu bilgileri (ad, hizmet, saat) kuaföre gösterilir.
- Yasal zorunluluk durumunda yetkili kurumlar.

Verileri satmıyoruz.

## 5. Veriler ne kadar saklanır?

[Saklama süreleri belirlenmeli. Örnek: hesap silinene kadar; randevu kayıtları [süre] boyunca.]

## 6. Veri güvenliği

Veriler Firebase güvenlik kuralları ile korunur. Kullanıcılar yalnızca kendi randevularını okuyabilir ve oluşturabilir. Ancak hiçbir sistem tam güvenli değildir.

## 7. Kullanıcı hakları

Kişisel Verilerin Korunması Kanunu (KVKK) kapsamında; verilerinizin işlenip işlenmediğini öğrenme, düzeltme, silme ve itiraz etme hakkına sahipsiniz. Taleplerinizi [iletişim e-postası] adresine iletebilirsiniz.

[Hesap silme özelliği eklenene kadar: "Hesabınızın silinmesini talep etmek için [e-posta] adresine yazın" ifadesi kullanılmalı.]

## 8. Çocuklar

Uygulama 18 yaş altındaki kullanıcılara yönelik değildir. [Yaş sınırı hukuki incelemeyle netleştirilmeli.]

## 9. Politika değişiklikleri

Bu politika değiştirilebilir. Önemli değişiklikler uygulama içinde veya bu sayfada duyurulur.

---

## Yayın öncesi kontrol listesi (sadece iç kullanım, yayınlanmayacak)

- [ ] Veri sorumlusu ve iletişim bilgileri dolduruldu
- [ ] Saklama süreleri belirlendi
- [ ] Konum verisinin sunucuya kaydedilip kaydedilmediği netleştirildi
- [ ] Analitik / çökme raporlama araçları varsa eklendi
- [ ] Hesap silme mekanizması (veya talep yolu) belirlendi
- [ ] Yaş sınırı netleştirildi
- [ ] Hukuki inceleme yapıldı
