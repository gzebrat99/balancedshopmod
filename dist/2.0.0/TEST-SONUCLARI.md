# Balanced Shop 2.0.0 doğrulaması

Minecraft 1.20.1 ve 1.21.1 için altı ayrı Fabric, Forge ve NeoForge hedefi derlendi. Her hedefin dedicated sunucusu ve gerçek Minecraft istemcisi çalıştırıldı.

| Minecraft | Yükleyici | Sunucu | İstemci / GUI | Ekonomi JUnit |
| --- | --- | ---: | ---: | ---: |
| 1.20.1 | Fabric 0.18.4 / API 0.92.12+1.20.1 | 189 PASS | 35 PASS | 14 PASS |
| 1.20.1 | Forge 47.3.10 | 189 PASS | 35 PASS | Ortak ekonomi kodu |
| 1.20.1 | NeoForge 47.1.106 | 189 PASS | 35 PASS | Ortak ekonomi kodu |
| 1.21.1 | Fabric 0.18.4 / API 0.116.17+1.21.1 | 192 PASS | 35 PASS | 14 PASS |
| 1.21.1 | Forge 52.1.16 | 192 PASS | 35 PASS | Ortak ekonomi kodu |
| 1.21.1 | NeoForge 21.1.243 | 192 PASS | 35 PASS | Ortak ekonomi kodu |

**1.353 Minecraft kontrolü + 28 JUnit testi = 1.381 başarılı kontrol.** Entegre sunucuda tekrarlanan sunucu kontrolleri bu toplama tekrar eklenmedi. Son koşuların PASS raporları pakette bulunur; başarısızlık işaretleri temizlenmeden test tekrarları yapılmaz.

Sunucu kontrolleri gerçek registry, tarif, envanter, dünya, blok varlığı ve loot kodunu kullanır. Sıfır başlangıç bakiyesi, bütün kayıtlı eşyaların fiyatı, bütün farm ürünlerinin satılabilmesi ve her tanımlı depolama bloğunun malzeme toplamına eşit satış fiyatı sınanır. İsim verilmiş eşyanın satışı, hasarlı aletin değeri, 7 adet balkabağı tohumu satışı, 16 eşya satışı ve 32 eşya alımı doğrulanır.

Genel spawner, çağırma yumurtası ve eski masa ürünlerinin markette gizlenmesi; villager/iron golem dahil bütün çağrılabilir mobların spawner katalogları; mob türüne özel fiyat ve satış doğrulanır. Bir türü satarken diğer türün kafesi korunur. En pahalı ender dragon spawner alınır; tüm spawner fiyatlarının işlem sınırına sığması kontrol edilir. Survival oyuncusu gerçek spawner eşyasını yerleştirir, seçilen iron golem türü block entity NBT kaydından okunur. Gündüz farmına uygun ışık kuralları da kayıtta doğrulanır.

Vein mining planı aynı blok türü, adet ve yarıçap sınırı için sınanır. Bedrock ve blok varlıkları reddedilir. Gerçek `ServerPlayerGameMode.destroyBlock` çağrısı aleti yıpratır ve bloğu kaldırır. Her loader'ın normal kırma olayına bir koruma callback'i bağlanır; iptal edilen blok kırılmaz. Bu kontroller yeni blok kırma yolunun normal loader izinlerini kullandığını doğrular. Mouse 4'ün fiziksel donanım basışı otomatik test tarafından yapılmaz; istemci tuş bağlaması ve basılı durum paketi kodla sağlanır.

Bütün mevcut vanilla büyü seviyeleri, Mending, Unbreaking III, gerçek büyülü kitap satın alımı, coin çekme/yatırmada para korunumu ve isim verilmiş sahte coin reddi sınanır. Şanslı yükseltmede %90 sınırı, kesin başarısızlıkta tüm verilen miktarın tüketimi, kesin başarı ve alete büyü hedefleri test edilir. Eski çalışma masası kayıt kimliği, axe etiketi ve loot kaydı korunur; ters üretim tarifi bulunmaz.

İstemci testleri Shop, Sell, Book, Spawner, Upgrade, Coin, Create Trader ve Market Hub olmak üzere sekiz ekranı açar. Ekranların gerçek loader ağı üzerinden yanıt alması, kontrollerin oluşması ve pencereye sığması kontrol edilir. Çeviriler, coin dokusu ve envanter satış tooltipi sınanır. Gerçek ekran görüntüleri ayrıca incelendi: 1.21.1'de başlıkları bulanıklaştıran vanilla arka plan çizimi ve coin tint saydamlığı düzeltildi. Test istemcisi odağın değişmesiyle durmaz; son test akışı gerçek OpenGL penceresini gizleyerek klavye müdahalesinden korunur. Sunucu yanıtı için sınırlı bekleme uygulanır.

JUnit testleri kayıt kalıcılığı, bozuk cüzdanın korunması, başarısız kayıtta bellek geri alımı, gerçek Windows dosya kilidiyle yeniden deneme, eski ayar yedekleme/geçişi, mod override'larının ve cüzdanların korunması, fiyat/olasılık sınırları ve aşırı yüksek malzeme fiyatlarının blok fiyatı kırpılması üzerinden arbitraj üretememesi dahil 14 senaryoyu kapsar.

Create keşfi `create:shaft` ve `create:andesite_alloy` için iki sentetik registry kaydıyla test edilir. Gerçek Create modunun tamamı her profilde yüklenip denenmedi. Fixture modellerinin uyarıları test kayıtlarına aittir; bu fixture sınıfları ve kaynakları dağıtım JAR'larına alınmaz. Başka modların kayıtlı eşyaları ve uygun mob türleri keşfedilir; her modun özel tarif, enerji, içerik veya spawn mekanikleri için eksiksiz uyumluluk iddiası yoktur. Spawner kaydının doğru olması sınanır; her mob için uzun süreli farm verimi ayrı ayrı ölçülmedi.

Paketleme kontrolü altı JAR'ın Minecraft/mod sürümünü, loader isim etiketini, Java bytecode sürümünü, gereken modelleri ve coin PNG kaynağını denetler. Native paketlerde Fabric API referansı, test fixture sızıntısı ve Uncrafting uygulama sınıfı bulunmaması; 1.20.1 native JAR'larda SRG yeniden eşleme ve iki Mixin refmap kaydı kontrol edilir. Forge 47.3.10 ve NeoForge 21.1.243 sürümleri gerçek açılış kayıtlarından doğrulanır. Kaynak JAR, dağıtım kopyası ve ZIP içeriğinin SHA256 değerleri eşleştirilir.

1.20.1 JAR'ları Java 17 bytecode; 1.21.1 JAR'ları Java 21 bytecode ile üretilir. 1.20.1 native oyun koşuları Java 17, Fabric 1.20.1 test koşuları Java 21 üzerinde çalışır. Güncel hedefler `versions` projeleridir; geçmiş 1.21.11 yayınları değiştirilmedi.
