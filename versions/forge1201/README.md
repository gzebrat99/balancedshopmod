# Balanced Shop 2.5.0 — Ganimet depolu üreteçler / Forge 1.20.1

Aktif geliştirme hedefi Minecraft **1.20.1**, Forge **47.3.10 veya üzeri**, Java **17**. Diğer sürümlerin eski kaynakları ve yayınları arşiv olarak korunur; bu güncellemede derlenmezler. Forge paketinde Fabric API gerekmez. İstemci ve sunucuda aynı 2.5.0 JAR kullanılmalıdır.

## Mob doğurmadan ganimet üreten spawner

Bütün normal Minecraft spawnerları (önceden yerleştirilmiş olanlar ve dükkândan alınan türler dahil) artık **80 oyun tikinde, normal hızda 4 saniyede bir**, içindeki **bir canlıya ait bir ganimet çekilişi** yapar. Gerçek mob veya deneyim küresi doğmaz. Yakında oyuncu aranmaz; bölgenin yüklü ve tik alıyor olması gerekir. Dünya kapalıyken veya bölge tik almıyorken üretim yoktur; çevrimdışı süre sonradan telafi edilmez. `doMobLoot=false` üretimi durdurur.

**Sağ tık → Üreteç Deposu**: 54 yuvalı depoyu açar. Tıklayarak ya da Shift + tıkla eşyaları al. Depoya dışarıdan eşya koyulmaz. Depo dolunca üretim durur. Son çekilişin sığmayan eşyaları saklanır ve yer açılınca aktarılır; ganimet silinmez veya yeniden çekilmez. Blok kırılınca depodaki ve bekleyen eşyalar yere düşer. Depo, kalan sayaç ve bekleyen ganimet spawnerın kendi bölge NBT kaydında saklanır; yeni blok kimliği veya dünya dönüşümü gerekmez.

Ganimet için sunucunun canlıya ait gerçek `LootTable` kaydı kullanılır; olasılık ve miktarlara çarpan eklenmez. Koşul bağlamı: SpawnData'daki canlı verileri, yanmayan normal oyuncu öldürmesi, Yağma 0 ve şans 0. Oyuncu öldürme şartlı blaze çubuğu gibi ganimetler çekilişe katılır. Demir golem her çekilişte tablosundaki 3–5 demiri ve 0–2 gelinciği verebilir. Köylü gibi tablosu boş türler eşya üretmez. Modların ve veri paketlerinin ganimet tabloları/Forge global loot modifier işlemleri otomatik okunur. Özel ölüm kodları, LivingDrops olaylarına bağlı ek ödüller, ekipman düşürme veya slime bölünmesi simüle edilmez. Özel spawner blokları farklı bir BlockEntity kullanıyorsa bu değişiklik onlara uygulanmaz.

`spawner/SpawnerLoot`: sunucuda çekiliş, Forge etkileşim korumaları ve yalnızca alınabilen menü. `SpawnerStorage`: 54 yuva, taşan ganimet ve kalıcı kayıt. `SpawnerStorageMixin`: vanilla tikini iptal ederek mob oluşmasını önler ve NBT'yi saklar. `SpawnerRemovalMixin`: kırılma/patlama/blok değiştirmede birikenleri düşürür. Menü native sandık protokolünü kullanır; mesafe, blok varlığı, eşya alma ve dışarıdan ekleme sunucuda doğrulanır. Bozuk üçüncü taraf ganimet tablosu sunucuyu kapatmaz; hata günlüğe yazılır ve bir dakika sonra yeniden denenir.

## Kullanım

- Modun menüleri, miktar etiketleri, para adı (**akçe**) ve eşya/büyü/üreteç adları Türkçe gösterilir. Minecraft'ın genel dil ayarı değiştirilmez. İngilizce oyun diliyle de Türkçe mod arayüzü ve arama kullanılabilir.
- Arama: **elmas**, **elmas bloğu / elmas blok**, **bal kabağı tohumu / balkabagi tohumu**, **demir kazma**, **kırılmazlık / kirilmazlik**, **onarım / tamir**, **köylü / koylu**. İngilizce kayıt kimlikleri de geçerlidir. Türkçe büyük/küçük harfler ve aksansız yazım sunucuda normalize edilir; büyü seviyeleri sayıyla veya Roma rakamıyla aranabilir.
- **B → ?** sekiz sayfalık Türkçe rehberi açar. Uzun sayfalar fare tekerleğiyle kaydırılır; mağazaya dönüş yeni bir güvenli container açar. Aynı içerik [REHBER-TURKCE.md](REHBER-TURKCE.md) dosyasındadır.
- Vanilla adları Minecraft 1.20.1'in yerel asset index 5 Türkçe dil kaydından alınan **3128** eşya/blok/canlı/büyü çeviri anahtarından gelir. Modların kendi `assets/<mod>/lang/tr_tr.json` dosyaları da okunur. Türkçe çevirisi bulunmayan mod eşyaları özgün adlarıyla kalır; kayıt kimliğiyle aranabilir. Create için mil/şaft ve andezit alaşımı gibi yaygın eş adlar desteklenir. Oyuncunun verdiği özel eşya adları ve NBT değiştirilmez.

- **B** veya `/shop`: koyu, yarı şeffaf RPG dükkanı. Solda ikonlu **Tümü, Madenler, Bloklar, Tarım, Silah/Zırh, Özel/Nadir** kategorileri; ortada boyuta göre **9–30 slot**; sağda seçili eşya, stok ve alış/satış. Arama, **1x / 16x / 64x / Max**, miktar okları ve kapatma düğmesi bulunur. 32 gibi diğer miktarlar oklarla seçilebilir.
- Altın kenarlar hafifçe parlar; slotların nadirlik ve seçim vurguları vardır. Üstteki coin ikonunun yanında bakiye görünür; üstüne gelince tam tutar gösterilir.
- Eşya ipucunda tam alış/satış fiyatı, stok ve **yeşil ▲ / kırmızı ▼** bulunur. Ok, sunucudaki son işlemden önceki fiyata göre hesaplanır. Değişmeyen fiyatta nötr çizgi gösterilir. Büyük ekranda, seçili eşyanın bu ekran açıkken gözlenen son 16 fiyatından küçük grafik çizilir. Geçmiş uydurulmaz; sunucu yeniden başlatıldığında/yenilendiğinde önceki işlem bilgisi yeniden birikmeye başlar.
- Create sekmesi, özel Create köylü ekranı, paketleri ve köylü ticaretini değiştiren mixin kaldırılmıştır. Köylüler normal davranır. **Create eşyaları silinmez**; genel dükkanda aranıp alınıp satılabilir. Eski `createVillagersEnabled` ayarı artık kullanılmaz.
- Alış ve satış aynı ekrandadır. **Max alış** bakiye, stok, eşyanın yığın sınırı ve envanterdeki boş alanla sınırlıdır. **Max satış** satılabilir miktar, stok üst sınırı ve bakiye üst sınırıyla sınırlıdır.
- Düğmede görünen toplam, sunucunun kademeli fiyat hesabıdır. Liste ve envanter ipucu anlık birim değerini gösterir. Fareyi işlemin üstüne getirince tam toplam ve baz/min/max fiyatlar görünür; liste üstünde stok görünür.
- Fiyat başka oyuncunun işlemiyle değişirse eski fiyatlı işlem reddedilir, teklif güncellenir; yeniden tıklanabilir.
- Kitaplar, özel mob spawnerları, coin, yükseltme ve envanter satış ekranları alt menüde korunur. **V / G / K** işlevleri korunur. Uncrafting geri eklenmez.
- **Toplu kırma tamamen kaldırıldı:** `VeinMiningService`, `VeinPackets` ve Mouse 4 kazma kaydı üretim paketinde bulunmaz. Eski `veinMining*` yapılandırma değerleri işlem başlatmaz. FTB Ultimine bağımsız mod olarak çalışabilir.
- **P** alan yerleştirme panelini açar. **3×3, 4×4, 5×5, 6×6, 7×7, 8×8, 9×9, 10×10** seçeneklerinden birini seçmek alan modunu açar. Ana elde bir blok tutunca yerleştirme öncesi çizgiler görünür; **yalnızca sağ tık** bir alan yerleştirir. Mouse 5 gerekmez. Sağ tık basılı tutulunca sürekli yerleştirme yapılmaz. **P → Tekli yerleştir** normal yerleştirmeye döner. Dünya bağlantısından çıkıldığında alan modu kapanır.
- Baktığınız yüzeye göre zemin/tavan veya dikey duvar çizilir. Altın tel çerçeve bütün alanı gösterir, dolu/değiştirilemeyen hücreler kırmızı görünür; gerçek yerleştirme malzeme, çarpışma ve sunucu koruma kurallarına bağlıdır. Önizleme sunucunun koruma kararını önceden bilemez. Çift boyutlarda alan pozitif düzlem eksenlerine bir hücre fazla uzanır. Ekranın altında seçili boyut ve sağ tık/P yönergesi bulunur.
- Normal oyunda her yerleştirme blok tüketir; eldeki yığın bittiğinde envanterdeki aynı eşya/NBT yığını kullanılır. Malzeme yetmezse kısmi alan oluşur. Katı bloklar kırılmaz/değiştirilmez. Creative normal Minecraft davranışıyla malzeme tüketmez. Sadece ana el kullanılır.
- Sunucu yalnızca 3–10 boyut isteğini kabul eder, hedefi kendi görüş ışınıyla bulur; 8 tick işlem aralığı uygular. Her hücrede dünya sınırı, yükseklik, yüklü chunk, `mayInteract`, `RightClickBlock` ve Forge'un `ItemStack.useOn` yerleştirme/geri alma olayları kullanılır. Modlu bloklar normal BlockItem yerleştirme kurallarını izler; her mod paketiyle birlikte çalışma garantisi değildir.
- Yeni oyuncu **0 coin** ile başlar; tarım ürünleri ve tohum satarak para kazanabilir.

Komutlar coin cinsindendir; en fazla iki ondalık basamak kullanılır (`12.50`).

```text
/bal
/balance
/pay Oyuncu 12.50
/economy add Oyuncu 100
/economy remove Oyuncu 25.50
/economy set Oyuncu 0
```

`/pay` çevrimiçi oyuncular arasında çalışır. Kendine, sıfır/negatif tutarda veya bakiyeyi aşan transfer reddedilir. `/economy` izin seviyesi **2** ister; konsol da kullanabilir. Eski `/shopadmin reload` ve `/shopadmin set` aynı yeni bakiyelere erişir.

## Modüler yapı

Kaynak kökü: `versions/forge1201/java/dev/eymen/balancedshop/`.

| Paket / sınıf | Görev |
| --- | --- |
| `keybind/MarketKeybind` | Mevcut B kaydını tekrar oluşturmadan yeni açılış paketine bağlar; istemci ekran kaydı. |
| `menu/ShopMenu` | Kayıtlı `AbstractContainerMenu`; oyuncuya özel rastgele oturum kimliği. Sanal ürün listesi gerçek eşya slotu değildir. |
| `screen/MarketScreen` | `AbstractContainerScreen`, `EditBox`, sekmeler, sayfalama, miktar ve işlem düğmeleri. Para/stok değiştirme yetkisi yoktur. |
| `network/MarketNetwork` | Forge `SimpleChannel`; yönü sınırlı Open/Request/Page paketleri. Sunucu ana iş parçacığında doğrulama ve yanıt. |
| `data/EconomyData` | Overworld `SavedData`: UUID bakiyeleri, stok havuzları, stok sürümü, tek seferlik JSON taşıma işareti. |
| `data/DynamicMarket` | Baz/min/max fiyat tanımı, stok fiyatı, kademeli teklif, envanter işlem planı ve commit. |
| `command/EconomyCommands` | `RegisterCommandsEvent`; kesin ondalık ayrıştırma, izin ve bakiye kontrolleri. |
| `placement/AreaPlacement` | Sunucuda hedef, alan, malzeme, koruma ve yerleştirme paketi. |
| `placement/PlacementClient`, `PlacementScreen` | P boyut/tekli modu, sürekli tel çerçeve önizleme ve sağ tık. |
| `localization/TurkishText` | Sunucuda/istemcide Türkçe ad çözümü, mod çevirileri ve Türkçe arama normalizasyonu. |
| `screen/GuideScreen` | Türkçe oyun içi rehber; sayfa, kaydırma ve güvenli mağazaya dönüş. |
| `verification/*` | Gerçek Forge sunucu/istemci testleri. Üretim JAR'ına konulmaz. |

`integrate.gradle` mevcut ortak kodu sadece Forge 1.20.1 için bu sınıflara bağlar. `WalletStore` eski kitap/coin/yükseltme servislerinin kullandığı bir cephe olarak kalır; oyundaki çağrıları `EconomyData` üzerinden çalışır. JSON kurucusu yalnızca eski uyumluluk testlerinde kullanılır. `Catalogue` baz fiyatları tutar, dinamik piyasa bağlandığında sorgu ve satış değerlerini güncel fiyatlardan üretir. Eski satış servisi de aynı dinamik işlem motoruna yönlendirilir.

## Fiyat ve stok hesabı

Her normal eşyanın baz fiyatı mevcut tarif/özel fiyat sisteminden gelir. Stok malzeme birimleriyle başlar: **4096**. Üst stok sınırı **1 milyar** birimdir. Otomatik, sınırsız stok yenileme yoktur; satış stok ekler.

```text
anlık alış = clamp(baz × 8192 / (4096 + stok), baz / 2, baz × 2)
```

Hesap tam sayı kuruşlarla yapılır; genel para/fiyat sınırları da uygulanır. Satış oranı mevcut yapılandırmadan gelir ve pozitif en küçük ödeme 1 kuruştur. Alım stok azaltır, fiyat yükselir. Satış stok artırır, fiyat düşer. Her birim sırayla hesaplandığı için 64'lü alış 64 tekli alışla aynı maliyettedir; alış-satış turu kâr üretmez. Hasarlı/büyülü eşyalarda önceki değer hesabı korunur.

Elmas, demir, altın, diğer sıkıştırılmış madenler ve tanımlı tarım blokları kendi malzemeleriyle **ortak stok** kullanır. Örneğin bir elmas bloğu dokuz elmaslık stok tüketir; fiyatı dokuz ardışık elmas alımına eşittir. Bu eşleme `FarmEconomy.BUNDLES` içindedir. Yeni mod eşyaları kayıt defterinden otomatik fiyatlanır; her modun özel NBT içeriğinin/enerjisinin değeri ayrıca hesaplanmaz.

Özel büyü kitabı ve mob spawner varyantları ile coin bozma kendi mevcut fiyat kurallarını kullanır; normal eşya stok havuzuna katılmaz. Şanslı yükseltmenin %90 sınırı ve başarısızlıkta eşya kaybı korunur. V envanter ekranındaki çoklu satış önizlemesi mevcut birim fiyatlardan tahmindir; kesin kademeli toplam B ekranında gösterilir.

## Güvenlik ve kayıt

İstemci; oyuncu UUID'si, para, fiyat veya verilecek eşya NBT'si gönderemez. Paket yalnızca istek taşır. Sunucu menü kimliği, rastgele oturum, artan sıra numarası, miktar sınırı, canlı oyuncu, işlem aralığı, stok sürümü, bakiye ve envanteri doğrular. Para ve envanter değiştirilmeden önce işlem bütünüyle planlanır. Ekrandan kitap/coin gibi eski GUI'lere geçerken gerçek container kapatılır.

Veri yolu:

```text
<dünya>/data/balancedshop_economy.dat
```

Overworld kaydı bütün boyutlarda ortaktır. Değişiklikler `setDirty()` ile işaretlenir ve Minecraft'ın dünya kaydıyla yazılır. `/pay`, iki hesabı doğruladıktan sonra aynı sunucu işinde birlikte değiştirir. Bozuk mevcut ekonomi dosyası sessizce sıfırlanmaz; ekonomi kapatılıp hata günlüğe yazılır.

İlk geçişte `<dünya>/balancedshop/wallets.json` okunur, `wallets.pre-2.1.0.json` yedeği oluşturulur. Asıl JSON silinmez. NBT taşıma işareti sonraki başlatmalarda eski paranın tekrar yüklenmesini engeller. Normal dünya kaydı ve kapatma kalıcılığı kullanılır; dünya ve oyuncu envanteriyle birlikte yedek alınmalıdır.

## Derleme ve doğrulama

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-versions.ps1
powershell -ExecutionPolicy Bypass -File scripts/verify-version.ps1 -Loader forge -Minecraft 1.20.1 -Phase both
```

İlk komutun varsayılanı yalnızca **Forge 1.20.1**. Üretim JAR'ı `versions/forge-legacy/build/1.20.1/libs/forge-balanced-shop-1.20.1-2.5.0.jar` altında; `dev` JAR kullanılmamalıdır. Eski Balanced Shop JAR'ını kaldırıp bunu koyun. Çerçeveler kodla çizilir; harici kaynak paketi gerekmez. Türkçe arama indeksi ve mod dil dosyaları `resources/assets/balancedshop/lang/` altından pakete eklenir; çalışma anında çeviri indirilmez.

Uygulama temeli: resmi Forge [menü belgeleri](https://docs.minecraftforge.net/en/1.20.1/gui/menus/), [SimpleImpl paketleri](https://docs.minecraftforge.net/en/1.20.1/networking/simpleimpl/), [SavedData](https://docs.minecraftforge.net/en/1.20.1/datastorage/saveddata/).
