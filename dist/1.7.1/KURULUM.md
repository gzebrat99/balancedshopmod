# Balanced Shop 1.7.1 paketi — 1.20.1 ve 1.21.1

İki Minecraft sürümüne de Fabric, Forge ve NeoForge desteği eklendi. Her hedef kendi Minecraft/yükleyici bağımlılıklarıyla derlenir. Mevcut 1.21.11 / 1.6.0 paketleri korunur.

Hazır paket: `dist/Balanced-Shop-1.7.1-1.20.1-1.21.1-All-Loaders.zip`. ZIP içindeki `1.20.1` ve `1.21.1` klasörlerinde üçer JAR bulunur. Minecraft 1.21.1 NeoForge JAR'ı 1.7.1'e güncellendi; diğer beş JAR'ın mod sürümü 1.7.0'dır. Dosyalar `fabric-`, `forge-`, `neoforge-` ile başlar; mod listesindeki isimler de yükleyici etiketi taşır. Doğrulama kapsamı [TEST-RESULTS.md](TEST-RESULTS.md) dosyasındadır; ZIP içinde `TEST-SONUCLARI.md` ve ham test raporları bulunur.

| Minecraft | Java | Fabric API | Forge | NeoForge |
| --- | --- | --- | --- | --- |
| 1.20.1 | 17+ | 0.92.12+1.20.1 | 47.4.26 | 1.20.1-47.1.106 |
| 1.21.1 | 21+ | 0.116.17+1.21.1 | 52.1.16 | 21.1.243+ |

NeoForge 1.7.1 sürümü 21.1.243 üzerinde test edildi: 39 sunucu ve 24 GUI kontrolü geçti. NeoForge, zorunlu bağımlılıklar için belirtilen [sürüm aralığını](https://docs.neoforged.net/docs/1.21.1/gettingstarted/modfiles/) açılışta denetler.

Minecraft 1.21.1 NeoForge 1.7.1 JAR'ı 21.1.243'e karşı derlenir. NeoForge bağımlılık aralığı `[21.1.243,)` olarak ayarlandı; 21.1.243 kurulu profilde önceki 1.7.0 JAR'ının verdiği `Expected range: [21.1.255,), Actual version: 21.1.243` hatası giderildi. Eski `neoforge-balanced-shop-1.21.1-1.7.0.jar` dosyasını `mods` klasöründen kaldırıp `neoforge-balanced-shop-1.21.1-1.7.1.jar` dosyasını koy. Sunucu kullanıyorsan aynı dosyayı sunucuya da kur. Dünya ve cüzdan dosyalarını koru. Yeni bağımlılık aralığı 21.1.255'i de kabul eder; bütün sonraki yükleyici sürümleri ayrı ayrı test edilmiş değildir.

Fabric için Loader 0.18.4+ ve ilgili Fabric API gerekir. Forge ve NeoForge paketlerine Fabric API yükleme. `mods` klasörüne yalnızca profilinin Minecraft sürümüne ve yükleyicisine uyan tek shop JAR'ını koy; sunucuya da aynı dosyayı kur.

Alış, satış, tüm büyü seviyeleri, uncrafting, şanslı yükseltme, fiziksel coinler, Create tüccarı, envanter satış tooltipi ve animasyonlu GUI'ler korunur. Başlangıç bakiyesi 0; başarısız yükseltmede seçilen eşya miktarı ve ücret kaybolur; azami şans %90'dır. Kayıtlı diğer mod eşyaları otomatik fiyatlandırılır. Desteklenmeyen veya özel veri üreten ters tarifler tahmin edilmez.

**Tuşlar:** B mağaza, V satış, U uncrafting, G yükseltme, K coinler. Büyülü kitaplar mağazadaki kitap bölümünden açılır. Bileme taşında meslek edinen yetişkin silah ustası, Create eşyalarını envanterindeki coinlere satar. Yapılandırma `config/balancedshop.json`; dünya fiyatları `balancedshop/prices.csv` ve `enchantments.csv` içinde üretilir.

1.20.1 sürümü eski NBT ve ağ protokolünü; 1.21.1 sürümü eşya bileşenlerini ve registry-aware paketleri kullanır. 1.20.1 Forge/NeoForge JAR'ları üretim için SRG adlarına yeniden eşlenir ve Mixin refmap içerir. Eski eşya model biçimi ve coin renk sağlayıcıları kullanılır.

Derlemek için proje kökünde:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-versions.ps1 -Loader fabric
powershell -ExecutionPolicy Bypass -File scripts/build-versions.ps1 -Loader forge
powershell -ExecutionPolicy Bypass -File scripts/build-versions.ps1 -Loader neoforge
```

Altı hedefin sunucu/istemci doğrulamaları ve Fabric ekonomi testleri geçtikten sonra `powershell -ExecutionPolicy Bypass -File scripts/package-backports.ps1` JAR içeriklerini ve SHA256 değerlerini denetleyerek ZIP'i oluşturur.

Varsayılan olarak yalnızca 1.20.1 ve 1.21.1 derlenir. Tek hedef için `-Minecraft 1.20.1` / `1.21.1` ekle. Test:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-version.ps1 -Loader neoforge -Minecraft 1.20.1
```

Test profilleri ayrı `versions/<yükleyici>/run/<Minecraft>` dizinlerinde açılır; 1.20.1 yerel portları `*-legacy` dizinlerini kullanır. Testler, dağıtıma alınmayan iki `create:` fixture eşyasıyla registry keşfini sınar. Gerçek Create modunun kendisi bu altı kombinasyonun her birinde denenmiş sayılmaz; ayrıntılar test raporundadır.

Bağımlılık sürümleri [Fabric Maven](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml), [Forge](https://files.minecraftforge.net/net/minecraftforge/forge/), [NeoForge Maven](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml) ve [eski NeoForge Maven](https://maven.neoforged.net/releases/net/neoforged/forge/maven-metadata.xml) üzerinden doğrulandı. Eski NeoForge/Forge üretim eşlemesi [ModDevGradle Legacy](https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md) ile yapılır.
