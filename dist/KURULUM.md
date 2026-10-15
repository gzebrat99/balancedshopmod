# Balanced Shop — Minecraft Java 1.21.11 / Fabric

Vanilla ve aynı Minecraft sürümünü kullanan Fabric modlarının kayıtlı eşyalarını kapsayan alış/satış mağazası.

## Kurulum ve kullanım

1. Minecraft **Java 1.21.11**, **Fabric Loader 0.18.4 veya daha yeni** ve **Fabric API 0.141.6+1.21.11 veya aynı Minecraft sürümü için daha yeni bir Fabric API** kur.
2. `balanced-shop-1.21.11-1.0.0.jar` dosyasını Minecraft profilinin `mods` klasörüne koy.
3. Çok oyunculuda aynı modu sunucuya ve tüm oyuncuların istemcilerine kur. Tek oyunculuda yalnızca kendi `mods` klasörün yeterlidir.
4. Dünyaya girip **B** tuşuna bas veya **`/shop`** yaz. Tuş, Kontroller menüsünden değiştirilebilir.
5. Eşyayı seç, 1 / 16 / 64 adet belirle, satın al veya envanterden sat. Simgenin üzerine gelince birim fiyatlar görünür.

Yeni oyuncunun başlangıç bakiyesi **1.000 coin**. `/balance` bakiyeyi gösterir. Bakiye oyuncunun UUID'sine bağlıdır, yeniden girişte sıfırlanmaz ve her dünyada ayrıdır. Para kazanmak için topladığın sade eşyaları satabilirsin. Gerçek para veya harici bir ekonomi eklentisi kullanılmaz.

Arama, eşya kimliği (`minecraft:diamond`, `create:shaft`) ve sunucunun eşya adıyla çalışır. Mod filtresi, kurulu eşya namespace'leri arasında dolaşır. Arayüz Minecraft diline göre Türkçe veya İngilizce gösterilir.

## Fiyat sistemi

- Air dışında **kayıtlı her eşya kimliği** için fiyat oluşturulur; mod eklenince liste otomatik genişler.
- Vanilla madenler, ganimetler, yemekler, ekipmanlar ve Create'in önemli parçaları/makineleri için temel değerler vardır. Diğer eşyalar malzeme, dayanıklılık ve eşya türüne göre fiyat alır.
- Shaped/shapeless crafting, stonecutting ve fırın tarifleri incelenir. Sonuç fiyatı, en ucuz geçerli malzeme birleşiminin birim maliyetiyle sınırlandırılır. Fırın için küçük bir işlem maliyeti eklenir. Geri verilen kaplar tam maliyetle sayılır.
- Tarif hesabı iki aşamada, aşama başına 64 turla sınırlıdır. Malzeme fiyatı çözülmeyen tariflerde genel fiyat kullanılır. Özel mod işlemleri, olasılıklı üretimler ve bileşen taşıyan tarifler için temel/özel fiyat kullanılır. Modun kendine özgü üretim dengesi gerekiyorsa fiyat override'ları düzenlenebilir.
- Satış fiyatı varsayılan olarak alışın **%25'i**. Doğrudan al-sat para kazandırmaz. Sunucu, istemcinin söylediği fiyatı kabul etmez.
- Coin değerleri tam sayı kuruşlarla tutulur; `100` = `1.00 coin`.

Her dünyada `<dünya>/balancedshop/prices.csv` dosyası tüm kayıtlı eşyaların fiyatını ve ticaret izinlerini gösterir. `wallets.json` aynı klasörde bakiyeleri saklar. Vanilla doğrulamasından üretilen CSV `build/verification/vanilla-prices.csv` konumundadır.

## Create ve diğer modlar

Minecraft sürümü ile mod yükleyicisi eşleşmelidir. Resmî Create'in 1.21.1 JAR'ı bu 1.21.11 profilinde kullanılamaz. **1.21.11 için Create Fly (Fabric portu)** kullanılabilir. Shop, Create sınıflarına doğrudan bağımlı değildir; `create:*` eşyalarını kayıt listesinden keşfeder. Create zorunlu değildir.

Create için zinc/brass/andesite alloy, çarklar, miller, su çarkları, presler, karıştırıcılar, pompalar, kazan parçaları, precision mechanism ve tren parçaları gibi eşyalara özel temel değerler tanımlıdır. Diğer kayıtlı Create eşyaları da otomatik fiyat alır. Aynı keşif mekanizması diğer Fabric modlarında da çalışır.

Sürüm araştırması:

- [Fabric'in 1.21.11 geliştirme notları](https://www.fabricmc.net/2025/12/05/12111.html)
- [Create'in resmî sürüm durumu](https://github.com/Creators-of-Create/Create/wiki/dev.status)
- [Create Fly sürümleri](https://modrinth.com/mod/create-fly/versions)
- [Fabric 1.21.11 otomatik test belgeleri](https://github.com/FabricMC/fabric-docs/blob/main/versions/1.21.11/develop/automatic-testing.md)

## Ticaret kuralları

Normal eşyalara GUI'den erişilir. Teknik/creative bloklar, spawn egg'ler, incomplete Create parçaları ve bileşen seçimi gerektiren potion/enchanted book/filled map gibi eşya kimliklerinin fiyatı vardır fakat alış varsayılan olarak kapalıdır. Bu GUI yalnızca eşyaların varsayılan ItemStack'lerini üretir; enchantment, potion etkisi veya map içeriği seçimi sunmaz.

Satış yalnızca varsayılan bileşenlere sahip, hasarsız eşyaları kabul eder. Büyülü, yeniden adlandırılmış, özel verili veya içi dolu eşyalar satılmaz. Sadece envanterin 36 depolama slotu kullanılır; takılı zırh ve yan el korunur. Yetersiz bakiye, eksik eşya ve dolu envanter işlemi reddeder. Bir kayıt hatası oluşursa işlemde değiştirilen envanter ve bellek bakiyesi geri alınır. Bozuk bakiye dosyası otomatik olarak sıfırlanmaz; mağaza açılmaz ve günlüğe hata yazılır.

## Yapılandırma ve yönetici komutları

İlk dünya açılışında `config/balancedshop.json` oluşturulur. Örnek:

```json
{
  "startingBalance": 100000,
  "sellRatio": 0.25,
  "allowRestrictedItems": false,
  "sellingEnabled": true,
  "transactionCooldownTicks": 4,
  "prices": {
    "minecraft:diamond": { "buy": 50000, "buyable": true, "sellable": true },
    "create:precision_mechanism": { "buy": 150000, "buyable": true, "sellable": true }
  },
  "namespaceMultipliers": { "create": 1.25 }
}
```

Override `buy` alanı kuruş cinsindedir. Satış aynı `sellRatio` ile hesaplanır. Override fiyatları tarif hesabıyla düşürülmez. `allowRestrictedItems` teknik eşyaların alışını açar; tek tek kontrol için override tercih edilebilir. Satış oranı `0..0.5`, işlem aralığı `1..200` tick aralığında olmalıdır. Geçersiz yapılandırma yeniden yüklenirken önceki katalog korunur.

- `/shopadmin reload`: yapılandırmayı ve fiyat kataloğunu yeniden yükler; açık mağazalar güncellenir.
- `/shopadmin set <oyuncu> <coin>`: bakiyeyi belirtilen tam coin değerine ayarlar.
- Vanilla `/reload`: başarılı datapack yeniden yüklemesinde fiyatlar tariflerle birlikte güncellenir.

Yönetici komutları permission level 2 gerektirir. `startingBalance` değişikliği yeniden başlatıldıktan sonra ilk kez oluşturulan cüzdanlara uygulanır; mevcut bakiyeleri değiştirmez.

## Derleme ve test

Gerekli ortam: Java 21, Gradle Wrapper 9.2.1. Windows için proje içine Java 21 indirip SHA-256 doğrulayan kurulum betiği vardır:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/bootstrap.ps1
powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 build
powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 runClientGameTest
```

Create Fly ile oyun içi uyumluluk testi için:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/create-compat.ps1
powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 runClientGameTest -PwithCompat
```

Bu betik yalnızca `.tools/compat` içine Minecraft 1.21.11 için Create Fly ve gerekli mod bağımlılıklarını indirir, SHA-512 doğrular. İndirilen modlar dağıtım JAR'ına dahil edilmez.

Normal Java 21 kurulumu olan diğer sistemlerde `./gradlew build` kullanılabilir. Asıl mod JAR'ı `build/libs/balanced-shop-1.21.11-1.0.0.jar` dosyasıdır. `-sources.jar` kaynak kod paketidir, `mods` klasörüne konulmamalıdır. Test modları dağıtım JAR'ına dahil edilmez.

Testler para taşmaları, cüzdan kalıcılığı, bozuk/kaydedilemeyen dosyalar, gerçek vanilla kayıt/tarif kapsamı, alış/satış bakiyesi, envanter kapasitesi, yığın birleştirme, özel eşya koruması ve geçersiz işlemleri kapsar. Oyun içi test, gerçek istemci/sunucu bağlantısıyla mağazayı açar, alış/satış yapar, üçüncü taraf test eşyasının keşfini ve reload davranışını doğrular. Raporlar `build/reports/tests/test/` altındadır.

Lisans: MIT.
