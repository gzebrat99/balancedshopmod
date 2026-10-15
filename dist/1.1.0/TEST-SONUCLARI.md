# Balanced Shop 1.1.0 doğrulaması — 3 Ekim 2026

Ortam: Minecraft Java 1.21.11, Java 21, Fabric Loader 0.18.4, Fabric API 0.141.6+1.21.11, Loom 1.15.4, Gradle 9.2.1.

## Otomatik testler

**18 test geçti; 0 başarısızlık, 0 test hatası.** HTML raporu: `build/reports/tests/test/index.html`.

- 6 ekonomi testi: para sınırları, kalıcı UUID cüzdanı, bozuk dosyanın korunması, kayıt hatasında geri alma, nadirlik/mod fiyat kuralları, eski ayarların bir kez yedeklenerek yeni ekonomiye geçirilmesi ve override'ların korunması.
- 12 Minecraft entegrasyon testi: registry ve vanilla tarif kapsamı; alış/satış bakiyesi; yığın birleştirme; envanter kapasitesi; hasarlı, adlandırılmış ve dolu eşyaların korunması; geçersiz işlem; IO geri alma; sıfır başlangıç bakiyesi ve artan fiyatlar; tam uncraft partisi; güvenli tarif filtresi; dolu envanterde uncraft iptali; malzeme değer farkı ücreti, yetersiz bakiye ve kayıt hatasında kaynak eşyanın korunması.

Elmas alış fiyatı 2.000 coin, satış fiyatı 250 coin olarak doğrulandı. Sıfır bakiyeyle bir elmas satışında 250 coin kazanıldı. 3 tahta geri çevrilmedi; 8 tahtadan iki tam tarif partisiyle 2 kütük alındı.

## Gerçek oyun testi

Create olmadan ve **Create Fly 1.21.11-6.0.9-5** ile Minecraft istemcisi ve entegre sunucuda test dünyası oluşturuldu. Her iki çalışma başarılı tamamlandı. Test modları dağıtılan JAR'a dahil değildir.

Doğrulanan akışlar:

1. Mağaza açıldı; ilk bakiye 0 coin. 16 elmas satışı 4.000 coin kazandırdı; bir elmas alışında 2.000 coin kesildi ve satışında 250 coin eklendi.
2. Üçüncü taraf test eşyası otomatik keşfedildi ve fiyatlandırıldı. Açık mağaza reload sonrasında güncellendi.
3. Uncraft GUI açıldı; 8 tahtadan tam 4 tahta tüketildi ve 1 kütük envantere ulaştı.
4. Üçüncü taraf test modunun crafting tarifi geri çevrilerek gerçek demir külçeleri alındı.
5. Yerleştirilen Uncrafting Table'ın kullanım olayı GUI'yi açtı.
6. Create kurulu profilde `create:shaft` alış/satışı ve geri üretimi çalıştı. `create:iron_sheet`, **gerçek `create:pressing/iron_ingot` makine tarifi** üzerinden külçeye geri çevrildi.
7. Sıvı tüketen `create:compacting/andesite_from_flint` tarifinin uncraft kataloğuna alınmadığı doğrulandı.

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
powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 test runClientGameTest build
powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 test runClientGameTest build -PwithCompat
```

Son derleme: **BUILD SUCCESSFUL**. Vanilla oyun günlüğü `build/verification/vanilla-game-test-1.1.0.log`, Create günlüğü `build/verification/create-game-test.log` içindedir. Ekran görüntüleri ve güncel CSV dosyaları `dist/1.1.0` altında teslim edilir.

Bu doğrulama belirtilen sürümleri ve entegre sunucuyu kapsar. Bütün üçüncü taraf mod paketleri test edilmedi. Özel serializer, sıvı, rastgele/çoklu çıktı ve crafting remainder kullanan tariflerin bir kısmı güvenlik nedeniyle geri üretimden hariç tutulur. Test ortamının çevrimdışı Realms ve Create'in ilk ayar dosyası uyarıları, başarılı test akışını engellemedi.
