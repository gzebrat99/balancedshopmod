# Balanced Shop 1.1.0 — Minecraft Java 1.21.11 / Fabric

Tüm kayıtlı eşyaları otomatik fiyatlandıran alış/satış mağazası ve tarifleri geri çeviren Uncrafting Table.

## Kurulum

1. Minecraft Java **1.21.11**, Fabric Loader **0.18.4 veya üzeri**, bu Minecraft sürümüne uygun Fabric API kur. Test edilen API: **0.141.6+1.21.11**.
2. `balanced-shop-1.21.11-1.1.0.jar` dosyasını profilinin `mods` klasörüne koy. Eski Balanced Shop JAR'ını bu klasörden çıkar; iki sürümü aynı anda yükleme.
3. Çok oyunculuda modu hem sunucuya hem bütün oyuncuların istemcilerine kur.

## Mağaza ve ekonomi

- **B** veya **`/shop`** mağazayı açar. Arama, sayfalama, mod filtresi ve 1 / 16 / 64 adet alış/satış vardır.
- Yeni oyuncu **0 coin** ile başlar. Topladığın eşyaları satarak para kazanırsın. `/balance` bakiyeyi gösterir.
- Alış fiyatları 1.0 sürümünün **4 katı**; satış değerleri **2 katı**. Satış, yeni alış değerinin **%12,5'i**. Örnek: elmas alış **2.000 coin**, satış **250 coin**.
- Her eşya kimliğinin fiyatı vardır. Vanilla nadirlik/malzeme değerleri, Create parça değerleri ve üretim tarifleri birlikte kullanılır. Diğer modların eşyaları registry üzerinden otomatik keşfedilir.
- Bakiye UUID ile, her dünyada ayrı tutulur; yeniden girişte korunur. Dünya klasöründeki `balancedshop/prices.csv` fiyat listesidir; `wallets.json` bakiyeleri saklar.
- Satış yalnızca varsayılan bileşenlere sahip hasarsız eşyaları kabul eder. Büyülü, adlandırılmış, dolu kutu veya özel verili eşyalar korunur. Yalnızca 36 envanter slotu kullanılır.
- Teknik/creative eşyalar, spawn egg'ler, incomplete Create parçaları ve özel bileşen seçimi gereken bazı eşyalar fiyatlandırılır; ticaretleri varsayılan olarak kapalıdır.

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

İndirmeler `.tools` altında tutulur ve hash doğrulanır. Create dağıtım JAR'ına dahil edilmez. Diğer sistemlerde Java 21 ile `./gradlew build` kullanılabilir. Asıl JAR `build/libs/balanced-shop-1.21.11-1.1.0.jar` dosyasıdır; sources/test modları `mods` klasörüne konmaz.

Test kapsamı ve sonuçlar `TEST-RESULTS.md`, HTML raporu `build/reports/tests/test/index.html` içindedir.

Sürüm kaynakları: [Fabric 1.21.11](https://www.fabricmc.net/2025/12/05/12111.html), [Fabric test belgeleri](https://github.com/FabricMC/fabric-docs/blob/main/versions/1.21.11/develop/automatic-testing.md), [Create Fly](https://modrinth.com/mod/create-fly/versions).

Lisans: MIT.
