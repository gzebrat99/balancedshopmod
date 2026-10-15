# Balanced Shop 1.7.1 paketi doğrulaması — 4 Ekim 2026

1.7.1 düzeltmesi Minecraft 1.21.1 NeoForge paketine aittir. Önceki `[21.1.255,)` koşulu, kurulu 21.1.243 sürümünü engelliyordu. Yeni JAR gerçek NeoForge 21.1.243'e karşı derlendi; koşul `[21.1.243,)` oldu. Bu yükleyiciyle gerçek dedicated sunucu açılışı ve gerçek istemci/entegre sunucu testi yeniden çalıştırıldı: **39 sunucu + 24 GUI kontrolü, 0 başarısızlık**. Her iki açılış kaydında `NeoForge mod loading, version 21.1.243, for MC 1.21.1` doğrulandı. Diğer beş JAR önceki doğrulanmış 1.7.0 sürümüdür; bunların oyun testleri bu düzeltme sırasında yeniden çalıştırılmadı.

Minecraft 1.20.1 ve 1.21.1 için toplam altı ayrı hedef derlendi. Native portlar kendi Forge/NeoForge bağımlılıklarını, kayıtlarını, olaylarını ve ağ API'lerini kullanır. 1.20.1 NeoForge, resmi eski `net.neoforged:forge:1.20.1-47.1.106` bağımlılığıyla derlenir.

| Minecraft | Yükleyici | Mod sürümü | Sunucu | İstemci / GUI | Ekonomi JUnit |
| --- | --- | --- | --- | --- | --- |
| 1.20.1 | Fabric 0.18.4 / API 0.92.12+1.20.1 | 1.7.0 | 39 PASS | 24 PASS | 10 PASS |
| 1.20.1 | Forge 47.4.26 | 1.7.0 | 39 PASS | 24 PASS | Ortak ekonomi kodu |
| 1.20.1 | NeoForge 47.1.106 | 1.7.0 | 39 PASS | 24 PASS | Ortak ekonomi kodu |
| 1.21.1 | Fabric 0.18.4 / API 0.116.17+1.21.1 | 1.7.0 | 39 PASS | 24 PASS | 10 PASS |
| 1.21.1 | Forge 52.1.16 | 1.7.0 | 39 PASS | 24 PASS | Ortak ekonomi kodu |
| 1.21.1 | NeoForge 21.1.243 | 1.7.1 | 39 PASS | 24 PASS | Ortak ekonomi kodu |

Altı hedefte 378 Minecraft doğrulaması ve iki Fabric hedefinde toplam 20 JUnit testi geçti: **398 kontrol, 0 başarısızlık**. Sunucu senaryosu GUI testinin entegre sunucusunda da tekrar çalışır; bu tekrarlar toplama ayrıca eklenmedi. Fabric 1.20.1 testleri Java 21 üzerinde yürütüldü ve JAR Java 17 bytecode hedefiyle üretildi. 1.20.1 native testler gerçek Java 17; 1.21.1 testler Java 21 ile yürütüldü.

Sunucu kontrolü gerçek Minecraft registry ve tariflerini kullanır. Sıfır başlangıç bakiyesi, tüm kayıtlı eşyaların pozitif fiyatı, satın alınabilen spawner/yumurtalar, Mending ve Unbreaking III dahil bütün vanilla büyü seviyeleri, 16 eşya satışı, 32 eşya alımı ve kesin ücret kontrol edilir. Aletlerin hasara göre değeri, özel isimli eşyaların korunması, dört coin türünün çekimi, para korunumu, toplu yatırma ve sahte isimli coin reddi sınanır. Gerçek Mending kitabı satın alınır; 4 tahta → 1 kütük ters tarifi çalıştırılır. Yükseltmede toplu kaynak miktarı, kesin başarısızlık/başarı ve %90 üst sınırı doğrulanır. Create registry eşyalarının keşfi ve tüccarın yalnızca Create eşyaları listelemesi kontrol edilir.

Uncrafting Table üretim tarifi, `mineable/axe` blok etiketi ve native Minecraft loot motoruyla kırılınca kendi eşyasını düşürmesi her altı hedefte ayrıca test edildi. 1.20.1 kaynakları `recipes`, `loot_tables` ve `tags/blocks`; 1.21.1 kaynakları `recipe`, `loot_table` ve `tags/block` dizinlerine yerleştirilir.

İstemci testi gerçek oyun dünyasında Shop, Sell, Book, Uncraft, Upgrade, Coin ve Create Trader ekranlarını açar. Her GUI'nin gerçek loader ağı üzerinden sunucu yanıtını alması, açık kalması ve widget oluşturması kontrol edilir. Coin dokusu, çeviriler ve sunucudan gelen envanter satış tooltipi doğrulanır. Test çalıştırıcısı ilk açılıştaki erişilebilirlik ekranından da test dünyasını açabilir; başarısızlık/başarı dosyaları her çalıştırmada sıfırlanır.

Ekonomi JUnit testleri tutar/overflow sınırlarını, config geçişlerini, fiyat override kurallarını, büyü fiyatlarını, kalıcı cüzdanı ve başarısız kaydın eski verileri korumasını sınar. Gerçek Windows `NOSHARE_DELETE` dosya kilidi açılır, atomik kayıt kilit bırakıldıktan sonra yeniden denenir. Değer bazlı olasılık ve ücretin pozitif beklenen kazanç döngüsü yaratmaması kontrol edilir.

Testler `create:shaft` ve `create:andesite_alloy` için iki **sentetik registry kaydı** kullanır. Gerçek Create modunun kendisi bu altı profilin her birinde yüklenip denenmiş değildir. Fixture model uyarıları bu geliştirme kayıtlarına aittir; fixture sınıfları ve `assets/create` kaynakları dağıtım JAR'larına alınmaz. Registry tabanlı fiyatlandırma başka mod eşyalarını keşfeder; her modun özel makine tarifi veya özel veri taşıyan eşyası için tam uyumluluk iddiası yoktur. Ters tarifler güvenli ve desteklenen biçimlerle sınırlıdır.

Ham raporlar `versions/<proje>/run/<Minecraft>/<server|client>/verification` dizinlerindedir. `forge-legacy` ve `neoforge-legacy` projeleri 1.20.1'i hedefler. Ekonomi XML raporları `versions/fabric/build/<Minecraft>/test-results/test` içindedir. Dağıtım ZIP'ine on iki PASS raporu ve iki JUnit XML raporu eklenir.

Paketleyici altı JAR'da tam Minecraft sürümünü, matrix'teki mod sürümünü, yükleyici isim etiketini ve Java class sürümünü denetler. Native JAR'larda Fabric API referansı ve test fixture sızıntısı bulunmaması kontrol edilir. 1.20.1 Forge/NeoForge üretim JAR'larının SRG yeniden eşlemesi ve iki Mixin'in gerçek `save` / `getOffers` refmap kayıtları doğrulanır. Tarif, loot, balta etiketi, eski coin modeli ve coin PNG kaynağı aranır. Yeni NeoForge JAR'ının bağımlılık aralığı ve sunucu/istemci testlerinin gerçekten matrix'teki NeoForge sürümünde çalışması açılış kayıtlarından kontrol edilir. Kaynak JAR, dağıtım kopyası ve ZIP içindeki JAR SHA256 değerleri eşleştirilir. Kaynak üretimi artık matrix ve mod sürümü değişimlerini de takip eder; eski bağımlılık metadata'sı yeniden kullanılmaz.
