# Balanced Shop 1.5.0 doğrulaması — 3 Ekim 2026

Minecraft Java 1.21.11, Java 21, Fabric Loader 0.18.4, Fabric API 0.141.6+1.21.11, Loom 1.15.4, Gradle 9.2.1.

Son doğrulama: `test runClientGameTest build -PwithCompat` başarılı, işlem çıkış kodu 0. Dağıtım dosyası remap edilmiş `balanced-shop-1.21.11-1.5.0.jar`.

## Otomatik testler

**38 test geçti: 0 başarısızlık, 0 test hatası.** Rapor: `build/reports/tests/test/index.html`.

- 9 ekonomi testi: para sınırları, kalıcı cüzdan, bozuk/kaydedilemeyen verilerin korunması, fiyat kuralları, config geçişi, büyü fiyatları ve override doğrulaması; yükseltme şansının değer oranıyla düşmesi ve ücretin pozitif beklenen kazanç döngüsünü engellemesi.
- 29 Minecraft entegrasyon testi: tüm item registry fiyat kapsamı, alış/satış, envanter kapasitesi, özel eşya koruması, sıfır başlangıç, uncraft tarif/ücret/batch kuralları, toplu satış, tüm spawn egg/spawner alışları; yükseltmenin başarıda hedef vermesi, başarısızlıkta seçilen kaynak miktarını tüketmesi, stale kaynak reddi, envanter/funds/save failure koruması, güvenli mod eşyası keşfi; ayrıca 7 fiziksel coin/tüccar testi.

## 1.5.0 coin ve Create köylüsü doğrulaması

- Dört coin türünde 32'şer coin çekme, kısmi yatırma ve hepsini yatırmada toplam bakiye + fiziksel para korunuyor. Envanter doluluğu, yetersiz bakiye, eksik coin, geçersiz adet/tür ve bakiye üst sınırı reddediliyor; coinler ve bakiye değişmiyor.
- `wallets.json.tmp` kayıt hatası simülasyonunda çekme/yatırma eşya veya para değiştirmiyor. Adlandırılmış coinler yatırılmıyor. Normal mağazadan coin alma/satma ve yükseltme ödülü seçimi kapalı; fiyat override'ı ve restricted-item ayarı bunu açamıyor.
- Tüccar sadece ticarete açık Create kayıtlarını listeliyor; shaft ilk sırada. Vanilla, başka namespace ve teknik ürünler takas motorunda da reddediliyor. Fiziksel para üstü kuruş hassasiyetinde doğru; ürün + para üstü sığmıyorsa coin tüketilmiyor. Banka bakiyesi takasa dahil edilmiyor.
- Gerçek GUI üzerinden 64 altın coin çekildi: 6.400 coin bakiyeden düştü ve envantere geldi. Eski işlem paketi yeniden gönderildi, `changed` sonucu geldi; ikinci para üretimi olmadı. Coinler geri yatırılınca ilk bakiye aynen geri geldi. Envanter tooltip'i coin değerini gösterdi.
- **Doğal meslek edinimi:** işsiz yetişkin köylü bileme taşının yanına çağrıldı; vanilla AI ile silah ustası oldu ve `JOB_SITE` hafızası oluştu. Mesleği NBT ile zorla atamadık. `getOffers()` boş döndü; eski zümrüt/silah takasları kapalı. Gerçek istemci etkileşimi Create Tüccarı GUI'sini açtı.
- 683 ticarete açık Create ürünü listelendi; coin olmadan alış düğmesi pasif kaldı. Tüccardan cüzdana gidip coin çekildi, geri dönüldü ve **16 shaft × 8,48 = 135,68 coin** alındı. Envantere 16 shaft geldi; **6.264,32 coin** tam para üstü kaldı. `andesite_alloy` araması külçe ve blok çeşidini buldu.
- Köylü 20 blok uzağa taşındıktan sonra ham alış paketi gönderildi: `trader_closed` sonucu geldi; coinler değişmedi ve alış düğmesi kapandı.
- 427×240 ve 640×400 GUI boyutları, coin modelleri/renkleri ve yeni ekranlar gerçek oyunda doğrulandı. Hareketli panel şeridi, yumuşak düğme vurgusu, kenar ışıkları, coin salınımı ve tüccarda dönen dişli kodda uygulanır. Coin cüzdanı ve tüccar ekran görüntüleri incelendi.

## Create yüklü gerçek oyun testi

Create Fly **1.21.11-6.0.9-5** ile gerçek istemci ve entegre sunucu başlatıldı. Test başarıyla tamamlandı. Test modları ve Create dağıtım JAR'ına dahil değildir.

- Shop: 0 başlangıç, elmas satış/alış bakiyeleri, mod kataloğu, reload, Create shaft alış/satışı doğrulandı.
- Uncraft: 4 tahtadan 1 kütük, üçüncü taraf crafting, Create shaft/alloy ve pressing iron sheet/ingot; sıvı tüketen tariflerin dışlanması; yerleştirilmiş masanın GUI açması doğrulandı.
- Sell GUI: 1 / 16 / 32 / 64 / hepsini sat; 200 elmasın tam satış tutarı ve adlandırılmış 5 elmasın korunması doğrulandı.
- Spawner, trial spawner, cow/blaze yumurtaları GUI üzerinden satın alındı.
- 136 büyü/seviye varyantının tamamı test edildi: vanilla 128, Create 6, test datapack 2. Dağıtım fiyat CSV'sinden test datapack kayıtları çıkarıldı. Mending I, Unbreaking III ve mod büyüsü GUI'den alındı; gerçek stored_enchantments bileşenleri istemciye ulaştı.
- Yükseltme: gerçek ağ isteğiyle elmas → netherite ingot denemesi yapıldı. Deneme 1 elmas ve tam 25 coin tüketti; sunucunun rastgele sonucuna uygun envanter/bakiye geldi. Başarı ve başarısızlık dalları ayrıca deterministik motor testleriyle doğrulandı.
- Aynı eski teklif/işlem paketi tekrar gönderildi: değişmiş teklif reddedildi, ikinci ücret veya ikinci eşya tüketimi olmadı.
- Büyü yükseltme: Sharpness V gerçek bileşeni ve ücret kesintisi; ad/hasar korunması; Smite/Sharpness çakışma filtresi; desteklenmeyen Efficiency'nin kılıçta çıkmaması; Mending uyumluluğu; mod büyüsü ve sade kitabın gerçek büyülü kitaba dönüşmesi doğrulandı.
- Envanterin yerel tooltip olayında elmas 250.00 coin/adet ve 16'lık yığın 4,000.00 coin olarak göründü. Adlandırılmış eşya satışa kapalı gösterildi. Gerçek InventoryScreen tooltip ekran görüntüsü alındı.
- 427×240 küçük GUI ve 640×400 büyük GUI doğrulandı. Büyük düzen şans çemberini ve kaynak/hedef değer kartını gösterdi. Shop, sell, books, uncraft ve upgrade ekran görüntüleri incelendi.

## Kapsam

Vanilla 1504 eşya, Balanced Shop masası ve coinleri 5 eşya ve Create 689 eşya fiyatlandırıldı; test eşyası dahil oyun kataloğu 2199 kayıt içerdi. Dağıtım CSV'sinde test eşyası çıkarılarak 2198 kayıt bırakılır. Coin kayıtlarındaki fiyat nominal değerdir, alış/satış kapalıdır. Registry üzerinden diğer mod eşyaları/büyüleri otomatik keşfedilir; her üçüncü taraf modun özel mekanikleri için genel uyumluluk garantisi verilmez. Fiilen çalıştırılan mod uyumluluk testi Create Fly ve sentetik test datapack'idir.

Oyun logunda Create'ın ilk çalıştırmada bulunmayan kendi config dosyalarını oluşturması ve çevrimdışı Realms oturum mesajları bulunabilir. Bunlar testin başarılı tamamlanmasını etkilemedi. Son koşuda Balanced Shop kaynaklı crash/işlem istisnası görülmedi.

Dağıtım paketi JAR, kurulum, örnek config, fiyat CSV'leri, ekran görüntüleri ve SHA256 içerir. Eski mod JAR'ı kaldırılarak yeni sürüm hem istemci hem sunucuya kurulmalıdır.
## 1.4.0 toplu yükseltme ve alet satış doğrulaması

- Kaynak ve hedefte bağımsız 16/32/64/256 hazır adetler, ±1 okları ve yeni adetli ağ paketleri doğrulandı. Kaynakta 256 → 255 → 256 ve hedefte 32 → 31 → 32 geçişleri gerçek GUI'de yapıldı.
- 256 crafting table → 1 elmas, toplam 1.024 coin kaynak SATIŞ / 2.000 coin hedef ALIŞ değeriyle %33,28 şans verdi. 32 elmas hedefi %1,04 şans verdi. Rastgele toplu denemede dört yığındaki 256 kaynak tamamen tüketildi; sonuç ve ücret sunucu cevabıyla birebir eşleşti.
- Deterministik testler: başarıda 128 elmasın iki yığına teslimi; başarısızlıkta 256 kaynağın tamamının tüketimi; kaynak yığınındaki değişikliğin teklifi geçersiz kılması; 0/negatif/2304 üstü ve mevcut miktar üstü adetlerin reddi.
- 64 istiflenmeyen kılıç ödülü kapasite nedeniyle ücret/eşya almadan reddedildi; 16 kılıç ödülü 16 ayrı slotta verildi. Toplu işlem kayıt hatasında bütün kaynaklar ve bakiye korundu.
- Bütün vanilla kılıç ve kazma türlerinin alış, satış ve yükseltme kaynağı olarak kullanılabilmesi doğrulandı. Hasar fiyatı düşürdü.
- Mending I + Unbreaking III taşıyan yarı hasarlı kılıç gerçek satış GUI'sinde parıltı/dayanıklılık bileşenleriyle göründü. Tooltip, sunucu fiyatı ve verilen coin aynıydı. Ardından daha ucuz hasarlı kılıç hepsini sat ile kendi değeri üzerinden satıldı; adlandırılmış üçüncü kılıç korundu.
- Üst şans sınırı %90, alt sınır %0,1; verilenlerin toplam satış değeri ve hedeflerin toplam alış değerine 0,65 katsayısı uygulanır. Çok yüksek kaynak/hedef oranında %90 sınırı ayrıca test edildi.
- Hasarlı/büyülü aletlerin satışa açılması kasıtlı davranış değişikliğidir. Dolu kutular, özel bileşenler ve adlandırılmış eşyaların satış koruması devam eder; uncraft özel eşya koruması da korunur.
