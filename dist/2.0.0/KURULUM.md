# Balanced Shop 2.0.0 — Çiftlik Pazarı

Minecraft **1.20.1 ve 1.21.1** için Fabric, Forge ve NeoForge paketleri. ZIP içinde iki Minecraft klasörü ve toplam altı JAR bulunur. Profiline uygun **tek** JAR kullan; istemciye ve sunucuya aynı 2.0.0 dosyasını kur. Eski shop JAR'ını değiştir; dünya ve `wallets.json` dosyaları korunur.

| Minecraft | Java | Fabric Loader / API | Forge | NeoForge |
| --- | --- | --- | --- | --- |
| 1.20.1 | 17+ | 0.18.4+ / 0.92.12+1.20.1 | 47.3.10+ | 1.20.1-47.1.106 |
| 1.21.1 | 21+ | 0.18.4+ / 0.116.17+1.21.1 | 52.1.16+ | 21.1.243+ |

Forge ve NeoForge için Fabric API gerekmez. 1.20.1 NeoForge, resmi eski NeoForge dağıtımıdır. Loader şartları önceki 47.3.10 ve 21.1.243 hatalarını gideren değerlerde tutuldu. Tablodaki temel loader sürümleri test edildi; sonraki her loader sürümü ayrı ayrı denenmedi.

## Yeni sistem

- **B:** animasyonlu çiftlik pazarı ana ekranı. Çiftlik, maden, alet, blok ve Create filtreleri.
- **V:** ayrı satış ekranı. Eşyayı seç; tümünü, 1/16/32/64 adet veya oklarla seçtiğin miktarı sat.
- Alışta miktar okları ve 1/16/32/64 düğmeleri var. Satın almadan önce fiyatı/işlem toplamını kontrol edebilirsin.
- **G:** eşya veya büyü yükseltmesi. Verilen ve alınan adetler ayrı seçilir; şans, verilen satış değeri ve hedefin alış değeriyle hesaplanır. En fazla %90; başarısızlıkta verilen eşyalar ve işlem ücreti kaybolur.
- **K:** coin çekme/yatırma. Coinler yalnızca bu ekran üzerinden cüzdana yatırılır; markette tekrar satılarak para üretilemez.
- **Mouse 4 basılı:** baktığın bloktan başlayarak bağlı aynı tür blokları kaz. Sol tıklama gerekmez. Varsayılan: basış başına 64 blok, 6 blok bağlantı yarıçapı ve oyuncuya en fazla 12 blok mesafe. Alet hızı, dayanıklılık, düşen eşyalar ve loader'ın normal blok kırma izinleri kullanılır. Sandık/spawner gibi blok varlıkları ve bedrock kazılmaz. Tuş, Minecraft kontrollerinden değiştirilebilir.
- Bütün kayıtlı eşyalara fiyat atanır. Tohumlar, ürünler, isim verilmiş eşyalar ve aletler satılabilir. Aletlerin dayanıklılığı ve bilinen büyüleri değerlerini etkiler. Modlara özel iç içerik/enerji bilgisi ayrıca fiyatlanmaz.
- Normal markette çağırma yumurtaları ve genel spawner bulunmaz. **Spawner** ekranında villager, iron golem ve diğer çağrılabilir mobların kendi spawnerları vardır. Modların kayıtlı mob türleri de keşfedilir. Yeterli alan, oyuncu yakınlığı ve Minecraft'ın normal spawn koşulları gerekir; aydınlık farm için spawner ışık aralığı 0–15'tir.
- Spawner satışında seçilen mob türü satılır; diğer türlerin kafesleri korunur.
- Uncrafting ekranı, U tuşu ve ters üretim kaldırıldı. Önceden yerleştirilmiş masalar normal çalışma masası olarak çalışır; eski kayıt kimliği dünya uyumluluğu için tutulur.

Create kuruluysa eşyaları otomatik listelenir ve fiyatlandırılır. Bileme taşı mesleğindeki silahçı köylü yalnızca Create ürünlerini envanterdeki coinlerle satar. Create zorunlu bağımlılık değildir. Büyü ekranında Mending, Unbreaking III ve diğer kayıtlı büyülerin tüm mevcut seviyeleri bulunur. Envanterde eşyanın üstüne gelince birim ve yığın satış değeri görünür.

## Ekonomi

Yeni oyuncu **0 coin** ile başlar. Eski cüzdanlar sıfırlanmaz. Maden/depolama blokları malzeme toplamına göre fiyatlanır: elmas bloğu = 9 elmas, demir bloğu = 9 demir külçesi. Blok satışında da aynı eşitlik uygulanır.

| Ürün | Birim alış | Birim satış |
| --- | ---: | ---: |
| Buğday | 64 | 8 |
| Havuç / patates | 48 | 6 |
| Balkabağı | 192 | 24 |
| Buğday / balkabağı / karpuz / pancar tohumu | 16 | 2 |
| Elmas | 2.000 | 250 |
| Elmas bloğu | 18.000 | 2.250 |
| Demir külçesi | 120 | 15 |
| Demir bloğu | 1.080 | 135 |
| Villager spawner | 1.500.000 | 187.500 |
| Iron golem spawner | 2.500.000 | 312.500 |

Tablo varsayılan ayarlarla coin cinsindendir. ZIP'teki CSV dosyalarında vanilla katalog fiyatları bulunur; `buyable=false` satırları normal satın alma ürünü değildir. Dünya açıldığında tüm kurulu modları kapsayan `world/balancedshop/prices.csv`, `spawners.csv` ve `enchantments.csv` üretilir.

`config/balancedshop.json` ayar şeması 3'e taşınır. Eski dosya yedeklenir; eski vanilla maden, depolama bloğu ve ürün fiyat override'ları temizlenir. Diğer mod fiyat override'ları ve cüzdanlar korunur. `spawnerPrices` ile mob kimliğine özel fiyat yazılabilir: örneğin `"minecraft:iron_golem": 250000000` değeri **2.500.000 coin** eder; ayarlar tam sayı yüzdelik coin birimindedir. `veinMiningEnabled`, `veinMiningMaxBlocks` ve `veinMiningRadius` sunucudan yönetilir. `/shopadmin reload` fiyatları yeniler; `/shopadmin set <oyuncu> <coin>` yönetici bakiyesi ayarlar.

`/shop`, `/sellshop`, `/bookshop`, `/spawners`, `/upgrade`, `/coins` ekran komutlarıdır. `/balance` bakiye gösterir. Türkçe/İngilizce metinler Minecraft dil seçimine uyar.

## Derleme ve doğrulama

Güncel altı hedef `versions` projelerinden üretilir. `src` ortak kaynak şablonlarıdır; geçmiş 1.21.11 / 1.6.0 yayınları `dist` içinde korunur.

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-versions.ps1 -Loader fabric
powershell -ExecutionPolicy Bypass -File scripts/build-versions.ps1 -Loader forge
powershell -ExecutionPolicy Bypass -File scripts/build-versions.ps1 -Loader neoforge
powershell -ExecutionPolicy Bypass -File scripts/verify-version.ps1 -Loader fabric -Minecraft 1.21.1 -Phase both
powershell -ExecutionPolicy Bypass -File scripts/package-backports.ps1
```

Test kapsamı `TEST-SONUCLARI.md` dosyasında ve `verification` klasöründedir. Kayıt tabanlı uyumluluk test edildi; her mod paketi ve modlara özel mekanik için eksiksiz uyumluluk iddiası yoktur.
