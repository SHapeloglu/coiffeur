# BACKLOG.md — coiffeur Fikir Havuzu

Önceliklendirilmiş işler `TASK.md`'de (kritik → yüksek → orta → düşük). Bu dosya henüz sıraya girmemiş fikirler içindir; bir fikir somutlaşınca `TASK.md`'ye taşınır.

> Not: Repo şu an yalnız dokümantasyon içeriyor (README, ARCHITECTURE, CLAUDE, SESSION, TASK); Kotlin kaynak kodu GitHub'a henüz gönderilmemiş. Kod eklenene kadar bu fikirler tasarım düzeyinde.

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
