# Balanced Shop 1.6.0 — Minecraft Java 1.21.11 / Fabric, Forge, NeoForge

Tüm kayıtlı eşyaları otomatik fiyatlandıran alış/satış mağazası, Uncrafting Table ve değer bazlı Şanslı Yükseltici.

## Üç yükleyici — 1.6.0

Fabric, Forge ve NeoForge için ayrı, kendi yükleyicisinin kayıt, olay ve ağ API'lerini kullanan JAR'lar bulunur. Dosya adları `fabric-`, `forge-`, `neoforge-` ile başlar; mod listesindeki isimler de **Fabric Balanced Shop**, **Forge Balanced Shop**, **NeoForge Balanced Shop** olarak görünür. Ekonomi, tarifler ve GUI kodu ortak kaynaklardan derlenir. Fiyatlar, sıfır başlangıç bakiyesi, başarısızlıkta eşya kaybı ve %90 şans sınırı korunur.

Windows/OneDrive altında kısa süreli dosya kilitlerinde cüzdanın atomik kaydı sınırlı süreyle yeniden denenir. Kilit sürerse işlem önceki bakiyeyi koruyarak reddedilir. Bu davranış gerçek Windows dosya kilidiyle test edilmiştir.

| Yükleyici | JAR | Gereken sürüm |
| --- | --- | --- |
| Fabric | `fabric-balanced-shop-1.21.11-1.6.0.jar` | Loader 0.18.4+, Fabric API 0.141.6+1.21.11 |
| Forge | `forge-balanced-shop-1.21.11-1.6.0.jar` | Forge 61.2.0+ |
| NeoForge | `neoforge-balanced-shop-1.21.11-1.6.0.jar` | NeoForge 21.11.45+ |

Her üç sürüm de Minecraft **1.21.11** ve Java **21** içindir. Forge/NeoForge sürümleri Fabric API gerektirmez. Bir profilin `mods` klasörüne yalnızca kullandığın yükleyicinin JAR'ını koy. Çok oyunculuda sunucu ve bütün istemciler aynı yükleyiciyi ve aynı mod sürümünü kullanmalıdır.

Dağıtımın CSV dosyaları önceki sürümde doğrulanmış, bu sürümde kuralları değişmeyen Fabric/Create Fly fiyat örnekleridir. Kurduğun modlara ve tariflere göre güncel tam liste, kendi dünyanın `balancedshop/prices.csv` ve `enchantments.csv` dosyalarında üretilir.

Create ayrı kurulur. **Create Fly 1.21.11** Fabric içindir; resmi Create'in mevcut Forge/NeoForge sürümleri Minecraft **1.21.1** hedefler. Dolayısıyla 1.21.1 Create JAR'ını bu 1.21.11 portlarına yükleme. Shop'un diğer mod eşyalarını registry üzerinden keşfetmesi üç yükleyicide de çalışır; Forge/NeoForge için gerçek Create 1.21.11 paketiyle doğrulama yapılmamıştır. Port testlerinde iki açıkça belirtilmiş `create:` test eşyası kullanılır; bu eşyalar dağıtım JAR'ına dahil edilmez.

## Create tüccarı ve fiziksel coinler — 1.5.0

Yetişkin, işsiz bir köylünün yakınına **bileme taşı** yerleştir. Vanilla meslek sistemi köylüyü silah ustası yaptığında sağ tıklamak **Create Tüccarı** ekranını açar. Mevcut silah ustaları da aynı tüccar ekranını kullanır. Normal zümrüt/silah teklifleri bu meslekte kapanır; diğer köylü meslekleri normal çalışır. Tüccar sadece ticarete açık `create:` eşyalarını satar. **Shaft, andesite alloy, cogwheel, large cogwheel, gearbox ve mechanical press** öncelikli gösterilir; diğer Create parçaları aranabilir ve sayfalanabilir. Ürün fiyatları ana mağazayla aynı sunucu kataloğundan gelir. 1 azalt/artır okları ve **1 / 16 / 32 / 64** adet düğmeleri vardır.

**K**, **`/coins`** veya GUI'lerdeki **◈ Coin** düğmesi cüzdanı açar. Coin türünü ve miktarı seçip **Envantere çek** veya **Bakiyeye yatır** düğmesine bas. **Tüm coinleri yatır** dört türü tek seferde bakiyeye aktarır. Çekme/yatırma bir transferdir, ek ücret alınmaz. Bakır **0,01**, gümüş **1**, altın **100**, platin **10.000** coin değerindedir; 64'lük yığınlar taşınır. Oklarla 1–2304 adet seçilebilir; hazır seçimler 1/16/32/64'tür. Cüzdan sıfırla başlar; önce eşya satarak kazanılır.

Create tüccarı **sadece 36 envanter slotundaki fiziksel coinlerle** ödeme alır. Banka bakiyesi doğrudan harcanmaz. Ödeme bütün sade coinlerin toplamını kullanır; kalan tutar büyükten küçüğe uygun türlere dönüştürülerek tam para üstü verilir. Ürün ve para üstü için yer yetersizse işlem reddedilir, coinler korunur. Köylüden 6 bloktan fazla uzaklaşmak, ölmesi, uyuması veya mesleğinin değişmesi alışverişi durdurur. Başka oyuncular da aynı köylüyü kullanabilir; stok ve köylü seviye geliştirme sınırlaması yoktur.

Coinler normal mağazadan alınamaz, satılamaz ve yükseltme ödülü olamaz; fiyat override'ları bunu açamaz. Adlandırılmış veya özel bileşenli coinler ödeme/yatırmada kullanılmaz. Coinin üzerinde envanterde gerçek para değeri görünür. Creative/yönetici tarafından verilen sade coinler para olarak kullanılabilir. Coin görseli bu proje için **imagegen** ile üretildi; dört renk Minecraft model tint sistemiyle uygulanır. Tüm GUI'lerde hareketli ışık şeridi, yavaş kenar parçacıkları ve yumuşak düğme vurguları vardır; coin ekranında para simgeleri hafifçe hareket eder.

`createVillagersEnabled: false` köylü dönüşümünü kapatır; `/shopadmin reload` uygular. Create kurulu değilse tüccar boş katalog mesajı gösterir. Minecraft 1.21.11 için test edilen Create sürümü **Create Fly 1.21.11-6.0.9-5**'tir. Coinler istemci ve sunucuda kayıtlıdır; yeni JAR iki tarafa da kurulmalıdır.

## Toplu Şanslı Yükseltici — 1.4.0

**G** veya **`/upgrade`** yükselticiyi açar; mağazanın üstündeki **Yükselt** düğmesi de aynı ekrana gider. Envanterinden bir kaynak eşya seç, **Eşya değiştir** veya **Büyü ekle** modunu seç, hedefi ara ve seç. Sol taraftaki **Ver** ve sağ taraftaki **Al** miktarları birbirinden bağımsızdır. Her iki tarafta **◀ / ▶** düğmeleri adedi **1** azaltır/artırır; **16 / 32 / 64 / 256** düğmeleri o adedi doğrudan seçer. Başarı şansı ve ücret sunucudan yeniden hesaplanır. Üst adet sınırı 2304'tür; kaynakta mevcut adet, ödülde gerçek envanter kapasitesi doğrulanır.

- **Eşya değiştir:** Seçtiğin kaynak adedi alınır; başarıda seçtiğin hedef adedi verilir. Kaynaklar 36 envanter slotundaki **aynı eşya ve aynı bileşenlere sahip** kopyalardan toplanır; birden fazla yığın desteklenir. Vanilla/mod ürünleri, kılıç ve kazmalar dahil listelenir. Hedefin birim fiyatının kaynaktan yüksek olması zorunlu değildir: iki tarafın toplam değerleri şansı belirler. Teknik/creative ve ticareti kapalı ürünler hedef olmaz.
- **Büyü ekle:** Başarıda kaynak eşyaya seçilen gerçek büyü/seviye uygulanır. Mevcut uyumlu büyüler, hasar ve özel ad korunur. Eşyanın desteklemediği, mevcut büyülerle çakışan ve eşit/daha düşük seviyeli büyüler listelenmez. Mod/datapack büyüleri aynı sunucu registry kurallarına göre keşfedilir. Sade kitap da büyülü kitaba dönüşebilir. Dolu kutular ve diğer özel bileşenli eşyalar korunur.
- **Başarısızlıkta seçtiğin kaynak adedinin TAMAMI ve ücret kaybolur.** Başarıda da kaynak adedi ve ücret tüketilir; yalnızca başarıda hedef adedi verilir. Bir deneme tek rastgele sonuç üretir; seçilen miktar topluca riske girer. Seçmediğin miktarlar ve farklı bileşenli kopyalar korunur.
- Başarı olasılığı `clamp(0.65 × (kaynak_birim_SATIŞ × verilen_adet) / (hedef_birim_ALIŞ × alınacak_adet), 0.001, 0.90)`; aralık **%0,1–%90**. Kaynak adedi artınca şans yükselir, hedef adedi artınca düşer. Değer oranı çok yüksek olsa da üst sınır **%90**; kesin başarı yoktur. Hasar ve mevcut büyüler hem kaynak satış değerini hem hedef alet alış değerini etkiler.
- Örnek: **256 crafting table → 1 elmas**: kaynak toplam satış değeri `256 × 4 = 1.024 coin`, hedef alış değeri `2.000 coin`; şans **%33,28**, varsayılan deneme ücreti **25 coin**. Aynı 256 crafting table ile **32 elmas** hedeflenirse şans **%1,04** olur.
- Ücret kuruş cinsinden `max(2500, ceil(max(0, p × hedef - kaynak) × 1.25 + kaynak × 0.02))`. En az **25 coin**; çok ucuz eşya/pahalı hedef durumunda olasılık tabanı ekonomiyi bozmasın diye beklenen kazanç farkı da ücretlendirilir. Yönetici `upgradeDestroyOnFailure: false` yaparsa eşya korunur ve ücret korunmuş eşya için ayrı beklenen kazanç formülüyle hesaplanır.
- Para, tüm ödülün envantere sığması, adet sınırları ve kaynak kopyalarının bileşenleri sunucuda doğrulanır. Tekliften sonra başka kaynak yığını değişmişse de işlem reddedilir. Kapasite/para/kayıt hatasında kaynaklar tüketilmez. Ödüller vanilla/mod maksimum yığın boyutlarıyla birleştirilir; yere eşya atılmaz. Tek kullanımlık teklifler tekrar paketiyle ikinci denemeyi önler.

Mağaza, satış, büyü kitapları, uncrafting ve yükseltici ortak koyu lacivert/aurora tasarımı kullanır: renkli kenarlar, degrade kartlar, özel düğmeler ve belirgin seçim vurguları. Büyük GUI alanında yükseltici şans çemberini gösterir; küçük alanda kompakt olasılık çubuğu kullanır. Tuşlar Kontroller menüsünden değiştirilebilir.

## Kılıç, kazma ve diğer aletlerin değeri

Kılıç, kazma, balta, kürek, çapa, yay, zırh gibi dayanıklılığı olan ekipmanlar satın alınabilir, satılabilir ve yükseltmede kullanılabilir. Satış değeri `(temel alış + mevcut büyülerin kitap fiyatları) × satış oranı × kalan dayanıklılık oranı` üzerinden aşağı yuvarlanır. Hasar değeri düşürür, büyü değeri artırır. Bilinmeyen/geçersiz büyüler ve diğer özel bileşenler satışa alınmaz. Adlandırılmış aletlerin satış koruması devam eder; büyü yükseltme modunda açıkça seçilen adlandırılmış aletin adı korunur.

**Envanterde eşyanın üzerine gelince** adet başına satış değeri, yığın varsa toplam satış tutarı gösterilir. Hasarlı/büyülü aletin değeri, satış ve yükseltmedekiyle aynı hesaplanır. Satış ekranında aletin gerçek büyü parıltısı ve dayanıklılık çubuğu gösterilir. Tooltip sunucunun fiyatlarını kullanır; girişte ve reload sırasında yenilenir. Adlandırılmış/özel verili/korumalı ürünlerde **Satılamaz** yazar. Sandık tooltip'lerinde de çalışır; her hover için ağ isteği göndermez.

## Kurulum

1. Minecraft Java **1.21.11**, Java **21** ve yukarıdaki tablodan seçtiğin yükleyiciyi kur. Fabric kullanıyorsan uygun Fabric API'yi de kur.
2. Seçtiğin yükleyiciye ait **1.6.0** JAR'ını profilinin `mods` klasörüne koy. Eski Balanced Shop JAR'ını bu klasörden çıkar; iki sürümü aynı anda yükleme.
3. Çok oyunculuda modu hem sunucuya hem bütün oyuncuların istemcilerine kur.

## Mağaza ve ekonomi

- **B** veya **`/shop`** mağazayı açar. Arama, sayfalama, mod filtresi ve 1 / 16 / 64 adet alış/satış vardır.
- Yeni oyuncu **0 coin** ile başlar. Topladığın eşyaları satarak para kazanırsın. `/balance` bakiyeyi gösterir.
- Alış fiyatları 1.0 sürümünün **4 katı**; satış değerleri **2 katı**. Satış, yeni alış değerinin **%12,5'i**. Örnek: elmas alış **2.000 coin**, satış **250 coin**.
- Her eşya kimliğinin fiyatı vardır. Vanilla nadirlik/malzeme değerleri, Create parça değerleri ve üretim tarifleri birlikte kullanılır. Diğer modların eşyaları registry üzerinden otomatik keşfedilir.
- Bakiye UUID ile, her dünyada ayrı tutulur; yeniden girişte korunur. Dünya klasöründeki `balancedshop/prices.csv` fiyat listesidir; `wallets.json` bakiyeleri saklar.
- Sade eşyalar ve hasarı/büyüsü fiyatlandırılabilen aletler satılır. Adlandırılmış, dolu kutu ve diğer özel bileşenli eşyalar korunur. Yalnızca 36 envanter slotu kullanılır; zırh slotu/yan elden otomatik satış yapılmaz.
- Teknik/creative eşyalar, incomplete Create parçaları ve potion gibi özel bileşen seçimi gereken bazı eşyalar fiyatlandırılır; ticaretleri varsayılan olarak kapalıdır. Büyü kitaplarının bütün seviyeleri ayrı bölümden satın alınabilir.

## Bütün büyü kitapları

Mağazanın üstündeki **Büyü kitapları** düğmesine veya katalogdaki büyülü kitap simgesine tıkla. **`/bookshop`** da aynı GUI'yi açar. Her büyünün 1'den kendi maksimum seviyesine kadar bütün normal seviyeleri ayrı ürünlerdir. Mending, Unbreaking III, hazine büyüleri ve lanetler dahil bütün sunucu kayıtları listelenir; mod ve datapack büyüleri otomatik keşfedilir.

Aramaya `mending`, `unbreaking` veya tam büyü kimliğini yaz. Satırların üzerinde büyü ve seviye adları görünür; üzerine gelince kimlik ve birim fiyat gösterilir. Bir kitap seç ve 1 / 16 / 64 adet satın al. Satın alınan kitap gerçek `stored_enchantments` bileşenini taşır; büyü ve seviye örs için kullanılabilir. Kitaplar vanilla kurallarıyla tek tek slot kaplar; envanter kapasitesi yetmeyen toplu alımlar reddedilir.

Varsayılan örnekler: **Mending I 40.000 coin**, **Unbreaking I 4.000**, **II 8.000**, **III 16.000 coin**. Seviyeler yükseldikçe fiyat artar; genel çarpan ve mod çarpanı uygulanır. Tüm kitap fiyatları dünya klasöründeki `balancedshop/enchantments.csv` dosyasına yazılır. Büyülü kitapların satışı ve uncraft işlemi, özel eşya koruması nedeniyle kapalıdır.

Belirli seviyeye mutlak fiyat vermek için config'e `"enchantmentPrices": { "minecraft:mending@1": 4000000, "minecraft:unbreaking@3": 1600000 }` ekleyebilirsin. Değerler kuruş cinsindedir. Eski config dosyaları bu yeni alan bulunmasa da çalışır. `/shopadmin reload` ve datapack `/reload`, kitap listesini ve fiyatları da yeniler.

## Spawner ve çağırma yumurtaları

Normal **spawner**, **trial spawner** ve bütün vanilla **çağırma yumurtaları** mağazadan alınabilir. Aramaya `minecraft:spawner` veya örneğin `minecraft:cow_spawn_egg` yazabilirsin. Modların `_spawn_egg` kimlikli yumurtaları da otomatik fiyatlandırılır.

Varsayılan fiyatlar: spawner **100.000 coin**, trial spawner **500.000 coin**, domuz yumurtası **4.000 coin**, inek yumurtası **6.000 coin**, zombi yumurtası **20.000 coin**, creeper yumurtası **40.000 coin**, blaze yumurtası **80.000 coin**. Nadir ve boss yaratıkların yumurtaları daha pahalıdır. Diğer yumurtalar için genel kategori fiyatı uygulanır; tüm değerler CSV'de bulunur ve config override ile değiştirilebilir.

## Ayrı satış ekranı

**V** veya **`/sellshop`** satış GUI'sini açar. Mağazanın üstündeki **Satış ekranı** düğmesiyle de geçebilirsin. Envanterinden bir eşyaya tıklayınca **Hepsini sat / 1x / 16x / 32x / 64x** seçenekleri görünür. Düğmenin üzerinde bekleyince kazanılacak coin gösterilir.

**Hepsini sat**, seçtiğin eşyanın 36 depolama slotundaki tüm satılabilen kopyalarını tek işlemde satar; 64 adetten büyük toplamlar da desteklenir. Farklı eşya türlerini satmaz. Hasarlı/büyülü aletler gerçek değerleriyle satılır; adlandırılmış ve diğer özel verili kopyalar, zırh slotları ve yan el korunur. Yetersiz miktarlı seçenekler pasiftir. Başarılı satıştan sonra envanter, seçenekler ve bakiye yenilenir.

## Uncrafting Table

**U** veya **`/uncraft`** geri üretim arayüzünü açar. Tuşlar Kontroller menüsünden değiştirilebilir. Yerleştirilmiş Uncrafting Table'a sağ tıklayarak da açabilirsin.

Masa tarifi:

```text
       Demir külçesi
Tahta  Çalışma masası  Tahta
Tahta      Tahta      Tahta
```

GUI'de envanterinden eşya seç, geri alacağın malzemeleri incele ve **Geri dönüştür** düğmesine bas. `<` / `>` aynı eşyanın alternatif tarifleri arasında geçer. 1 / 4 / 16 / 64 tarif partisi seçilebilir; yeterli eşya gerekir.

- Tarifin **tam çıktı miktarı** tüketilir. Örneğin **4 tahta → 1 kütük**; tek tahta ile bir kütük üretilemez.
- Tagged malzemelerde en ucuz geçerli tür seçilir ve işlemden önce gösterilir. Minecraft üretimde kullanılan malzeme türünü saklamadığı için eski türü geri getirme garantisi yoktur.
- Geri alınan malzemelerin alış değeri, tüketilen eşyanın alış değerini aşmıyorsa işlem ücretsizdir; aşıyorsa aradaki fark coin olarak alınır. Ücret önceden gösterilir; alış/uncraft/sat döngüsü para çoğaltmaz.
- Hasarlı, büyülü, adlandırılmış, dolu ve özel verili eşyalar geri çevrilmez. Envanter doluysa malzemeler yere atılmaz, işlem reddedilir. Cüzdan kaydı başarısız olursa eşya tüketilmez.

## Mod uyumluluğu

Mağaza seçtiğin yükleyicideki aynı Minecraft sürümüne uygun modların tüm kayıtlı eşyalarını fiyatlandırır. Uncrafting, sunucunun gerçek `CraftingRecipe` tariflerini tarar; standart shaped/shapeless kullanan diğer modlara otomatik uygulanır. Datapack `/reload` ve `/shopadmin reload` geri üretim kataloğunu da yeniler; eski önizlemeyle işlem yapılmaz.

**Create Fly 1.21.11-6.0.9-5** ile gerçek oyun testi yapılmıştır. Shop, Create'e doğrudan bağımlı değildir. Create için mekanik crafting ve yalnızca eşya kullanan, tek ve kesin çıktı veren pressing, mixing, compacting, cutting, milling, crushing tariflerine adaptör vardır.

Sıvı tüketen, rastgele/çoklu çıktı veren, kap geri bırakan veya bilinmeyen özel üretim mantığı kullanan tarifler güvenli biçimde atlanır. Bu nedenle her modun her özel makine tarifini geri çevirmek garanti edilmez. Vanilla eritme ve stonecutting geri üretime dahil edilmez. Aynı sürüm ve yükleyiciye uygun modlar kullanılmalıdır.

## Ayarlar ve güncelleme

`config/balancedshop.json` ilk açılışta oluşur. Para değerleri kuruş cinsindedir: `100` = `1 coin`.

```json
{
  "schemaVersion": 2,
  "startingBalance": 0,
  "priceMultiplier": 4.0,
  "sellRatio": 0.125,
  "allowRestrictedItems": false,
  "sellingEnabled": true,
  "createVillagersEnabled": true,
  "transactionCooldownTicks": 4,
  "prices": {},
  "namespaceMultipliers": { "create": 1.25 }
}
```

Özel fiyat örneği: `"prices": { "minecraft:diamond": { "buy": 200000, "buyable": true, "sellable": true } }`. Override mutlak alış fiyatıdır; genel çarpan ve tarif hesabı bu değeri değiştirmez. Satış yine `sellRatio` ile hesaplanır. `priceMultiplier` 1–100, `sellRatio` 0–0.5, işlem aralığı 1–200 tick olmalıdır.

1.0 sürümünün ayar dosyası ilk açılışta `balancedshop-v1.backup.json` olarak yedeklenir ve yeni ekonomi varsayılanları uygulanır. Mevcut oyuncuların kazanılmış bakiyeleri korunur. Eski oyuncuyu da sıfırlamak istersen yönetici olarak **`/shopadmin set <oyuncu> 0`** kullan.

- `/shopadmin reload`: ayarları, fiyatları ve uncraft tariflerini yeniler.
- `/shopadmin set <oyuncu> <coin>`: bakiyeyi tam coin değerine ayarlar.
- Yönetici komutları permission level 2 ister. Başlangıç bakiyesi değişikliği, yeniden başlatmadan sonra yeni oluşturulan cüzdanlara uygulanır.

## Derleme ve doğrulama

Java 21 kullanılır. Fabric/NeoForge Gradle Wrapper 9.2.1, Forge kendi Gradle Wrapper 9.3.1 sürümünü kullanır. Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/bootstrap.ps1
powershell -ExecutionPolicy Bypass -File scripts/loader-gradle.ps1 fabric test build
powershell -ExecutionPolicy Bypass -File scripts/loader-gradle.ps1 forge build
powershell -ExecutionPolicy Bypass -File scripts/loader-gradle.ps1 neoforge build
powershell -ExecutionPolicy Bypass -File scripts/loader-gradle.ps1 forge runClient -Pverify
powershell -ExecutionPolicy Bypass -File scripts/loader-gradle.ps1 neoforge runClient -Pverify
```

Create uyumluluk testi ve dağıtım:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/create-compat.ps1
powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 test runClientGameTest build -PwithCompat
powershell -ExecutionPolicy Bypass -File scripts/package-loaders.ps1
```

İndirmeler `.tools` altında tutulur ve hash doğrulanır. Create dağıtım JAR'ına dahil edilmez. JAR'lar Fabric için `build/libs`, Forge için `ports/forge/build/libs`, NeoForge için `ports/neoforge/build/libs` altında oluşur. `scripts/package-loaders.ps1` üç dağıtım JAR'ını kurulum belgesi, test sonuçları ve SHA256 dosyasıyla `dist/1.6.0` altında toplar. Sources/test JAR'ları `mods` klasörüne konmaz.

`-Pverify` yalnızca geliştirme doğrulaması içindir: istemciyi açar, ayrı bir test dünyası oluşturur, gerçek ağ/GUI işlemlerini yürütür ve kapatır. Sonuçlar her portun `run/client/verification` klasöründedir. Başarı için `server.txt` ve `client.txt` PASS içermelidir; `FAILED.txt` varsa test başarısızdır. Gradle çıkış kodu tek başına doğrulama sonucu değildir. Test sürücüsü ve sentetik mod eşyaları dağıtım JAR'larından çıkarılır.

Test kapsamı ve sonuçlar `TEST-RESULTS.md`, HTML raporu `build/reports/tests/test/index.html` içindedir.

Sürüm kaynakları: [Fabric 1.21.11](https://www.fabricmc.net/2025/12/05/12111.html), [Fabric test belgeleri](https://github.com/FabricMC/fabric-docs/blob/main/versions/1.21.11/develop/automatic-testing.md), [Create Fly](https://modrinth.com/mod/create-fly/versions).

Port kaynakları: [Forge 1.21.11 indirmeleri](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.21.11.html), [NeoForge 1.21.11 MDK](https://github.com/NeoForgeMDKs/MDK-1.21.11-ModDevGradle), [resmi Create sürüm hedefi](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/gradle.properties).

Lisans: MIT.
