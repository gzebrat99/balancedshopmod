# Balanced Shop 1.2.2 doğrulaması — 3 Ekim 2026

Ortam: Minecraft Java 1.21.11, Java 21, Fabric Loader 0.18.4, Fabric API 0.141.6+1.21.11, Loom 1.15.4, Gradle 9.2.1.

## Otomatik testler

**23 test geçti; 0 başarısızlık, 0 test hatası.** HTML raporu: `build/reports/tests/test/index.html`.

- 8 ekonomi testi: para sınırları, kalıcı UUID cüzdanı, bozuk dosyanın korunması, kayıt hatasında geri alma, nadirlik/mod fiyat kuralları, eski ayarların bir kez yedeklenerek yeni ekonomiye geçirilmesi, kitapların seviye/hazine/lanet/mod fiyatları, fiyat üst sınırı ve kitap override doğrulaması.
- 15 Minecraft entegrasyon testi: registry ve vanilla tarif kapsamı; alış/satış bakiyesi; yığın birleştirme; envanter kapasitesi; hasarlı, adlandırılmış ve dolu eşyaların korunması; geçersiz işlem; IO geri alma; sıfır başlangıç bakiyesi ve artan fiyatlar; tam uncraft partisi; güvenli tarif filtresi; dolu envanterde uncraft iptali; malzeme değer farkı ücreti; ayrı satış ekranının 1/16/32/64 miktarları, birden çok yığının toplu satışı, özel eşyaların/yan elin korunması ve toplu satışın kayıt hatasında geri alınması.

Elmas alış fiyatı 2.000 coin, satış fiyatı 250 coin olarak doğrulandı. Sıfır bakiyeyle bir elmas satışında 250 coin kazanıldı. 3 tahta geri çevrilmedi; 8 tahtadan iki tam tarif partisiyle 2 kütük alındı.

## Gerçek oyun testi

1.2.2, **Create Fly 1.21.11-6.0.9-5** ile Minecraft istemcisi ve entegre sunucuda test dünyası oluşturularak doğrulandı. Çalışma başarılı tamamlandı. Önceki 1.1.0 sürümünün Create olmadan yapılan oyun doğrulaması da korunmaktadır. Test modları dağıtılan JAR'a dahil değildir.

Doğrulanan akışlar:

1. Mağaza açıldı; ilk bakiye 0 coin. 16 elmas satışı 4.000 coin kazandırdı; bir elmas alışında 2.000 coin kesildi ve satışında 250 coin eklendi.
2. Üçüncü taraf test eşyası otomatik keşfedildi ve fiyatlandırıldı. Açık mağaza reload sonrasında güncellendi.
3. Uncraft GUI açıldı; 8 tahtadan tam 4 tahta tüketildi ve 1 kütük envantere ulaştı.
4. Üçüncü taraf test modunun crafting tarifi geri çevrilerek gerçek demir külçeleri alındı.
5. Yerleştirilen Uncrafting Table'ın kullanım olayı GUI'yi açtı.
6. Create kurulu profilde `create:shaft` alış/satışı ve geri üretimi çalıştı. `create:iron_sheet`, **gerçek `create:pressing/iron_ingot` makine tarifi** üzerinden külçeye geri çevrildi.
7. Sıvı tüketen `create:compacting/andesite_from_flint` tarifinin uncraft kataloğuna alınmadığı doğrulandı.
8. Mağazadaki satış düğmesi ayrı GUI'yi açtı. Eşyaya tıklayınca beş miktar seçeneği görünür ve etkin oldu. 200 sade elmas üzerinde 1, 16, 32 ve 64 adet satışlar tek tek yapıldı; kalan **87 elmas Hepsini sat** ile tek işlemde satıldı. Toplam 50.000 coin eklendi. Adlandırılmış 5 elmas envanterde korundu. GUI envanteri ve bakiye her işlemden sonra sunucuyla eşleşti.
9. Spawner, trial spawner, inek ve blaze çağırma yumurtaları **mağaza GUI'sinden** satın alındı; doğru fiyat bakiyeden kesildi ve eşyalar istemci envanterine ulaştı. Entegrasyon testinde **87 vanilla çağırma yumurtasının tamamı** tek tek satın alındı ve varsayılan ItemStack bileşenlerinin korunduğu doğrulandı. Spawner 100.000, trial spawner 500.000, inek yumurtası 6.000, blaze yumurtası 80.000 coin. Command block alışının kapalı kaldığı ayrıca kontrol edildi.
10. Büyü kitapları GUI'si mağaza düğmesiyle açıldı. **43 vanilla büyü / 128 seviye varyantı**, **2 Create büyüsü / 6 varyant** ve test datapack'inin **1 büyüsü / 2 varyantı** keşfedildi. Sunucunun registry'sindeki maksimum seviyelerin toplamı kitap kataloğu sayısıyla eşleşti. Bu **136 varyantın tamamı** tek tek satın alınarak büyü kimliği, seviye, `stored_enchantments` bileşeni ve bakiye kesintisi doğrulandı. Teslim edilen CSV, test varyantları çıkarıldıktan sonra **134 kitap** içerir.
11. Mending I, Unbreaking III ve test modunun ikinci seviye büyüsü gerçek istemci/sunucu GUI akışıyla satın alındı; doğru büyüler istemci envanterine ulaştı. Kitap fiyatı override'ı doğrulandı. Eksik para, dolu envanter, bilinmeyen büyü, geçersiz seviye/miktar ve cüzdan kayıt hatasında kitap üretilmedi. 64 kitaplık alım kapasite nedeniyle reddedildi; 16 kitaplık alımda 16 ayrı slot kullanıldı ve tam fiyat kesildi. Mod büyüsü araması reload sonrasında korundu.

Fiyat listesi kapsamı:

| İçerik | Eşya sayısı |
| --- | ---: |
| Vanilla, air hariç | 1.504 |
| Create Fly | 689 |
| Uncrafting Table | 1 |
| Toplam, test eşyaları hariç | 2.194 |

Create kurulu testte 1.839 güvenli geri üretim tarifi bulundu; bu sayı üçüncü taraf test tarifini de içerir. Alternatif tarifler GUI'de ayrı seçilir. Varsayılan seçim, en düşük ücretten sonra en küçük tam çıktı partisini tercih eder.

Komutlar:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 test runClientGameTest build -PwithCompat
```

Son derleme: **BUILD SUCCESSFUL**. Önceki vanilla oyun günlüğü `build/verification/vanilla-game-test-1.1.0.log`, güncel Create günlüğü `build/verification/create-game-test.log` içindedir. Satış GUI'si dahil ekran görüntüleri ve güncel CSV dosyaları `dist/1.2.2` altında teslim edilir.

Bu doğrulama belirtilen sürümleri ve entegre sunucuyu kapsar. Bütün üçüncü taraf mod paketleri test edilmedi. Özel serializer, sıvı, rastgele/çoklu çıktı ve crafting remainder kullanan tariflerin bir kısmı güvenlik nedeniyle geri üretimden hariç tutulur. Test ortamının çevrimdışı Realms ve Create'in ilk ayar dosyası uyarıları, başarılı test akışını engellemedi.
