# Balanced Shop 1.4.0 — Minecraft Java 1.21.11 / Fabric

Tüm kayıtlı eşyaları otomatik fiyatlandıran alış/satış mağazası, Uncrafting Table ve değer bazlı Şanslı Yükseltici.

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

1. Minecraft Java **1.21.11**, Fabric Loader **0.18.4 veya üzeri**, bu Minecraft sürümüne uygun Fabric API kur. Test edilen API: **0.141.6+1.21.11**.
2. `balanced-shop-1.21.11-1.4.0.jar` dosyasını profilinin `mods` klasörüne koy. Eski Balanced Shop JAR'ını bu klasörden çıkar; iki sürümü aynı anda yükleme.
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

Mağaza aynı sürümdeki Fabric modlarının tüm kayıtlı eşyalarını fiyatlandırır. Uncrafting, sunucunun gerçek `CraftingRecipe` tariflerini tarar; standart shaped/shapeless kullanan diğer modlara otomatik uygulanır. Datapack `/reload` ve `/shopadmin reload` geri üretim kataloğunu da yeniler; eski önizlemeyle işlem yapılmaz.

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

Java 21 ve Gradle Wrapper 9.2.1 kullanılır. Windows:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/bootstrap.ps1
powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 build
powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 runClientGameTest
```

Create uyumluluk testi ve dağıtım:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/create-compat.ps1
powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 test runClientGameTest build -PwithCompat
powershell -ExecutionPolicy Bypass -File scripts/package.ps1
```

İndirmeler `.tools` altında tutulur ve hash doğrulanır. Create dağıtım JAR'ına dahil edilmez. Diğer sistemlerde Java 21 ile `./gradlew build` kullanılabilir. Asıl JAR `build/libs/balanced-shop-1.21.11-1.4.0.jar` dosyasıdır; sources/test modları `mods` klasörüne konmaz.

Test kapsamı ve sonuçlar `TEST-RESULTS.md`, HTML raporu `build/reports/tests/test/index.html` içindedir.

Sürüm kaynakları: [Fabric 1.21.11](https://www.fabricmc.net/2025/12/05/12111.html), [Fabric test belgeleri](https://github.com/FabricMC/fabric-docs/blob/main/versions/1.21.11/develop/automatic-testing.md), [Create Fly](https://modrinth.com/mod/create-fly/versions).

Lisans: MIT.
