# Balanced Shop 2.2.0 — koyu RPG arayüzü / Forge 1.20.1

Aktif geliştirme hedefi Minecraft **1.20.1**, Forge **47.3.10 veya üzeri**, Java **17**. Diğer sürümlerin eski kaynakları ve yayınları arşiv olarak korunur; bu güncellemede derlenmezler. Forge paketinde Fabric API gerekmez. İstemci ve sunucuda aynı 2.2.0 JAR kullanılmalıdır.

## Kullanım

- **B** veya `/shop`: koyu, yarı şeffaf RPG dükkanı. Solda ikonlu **Tümü, Madenler, Bloklar, Tarım, Silah/Zırh, Özel/Nadir** kategorileri; ortada boyuta göre **9–30 slot**; sağda seçili eşya, stok ve alış/satış. Arama, **1x / 16x / 64x / Max**, miktar okları ve kapatma düğmesi bulunur. 32 gibi diğer miktarlar oklarla seçilebilir.
- Altın kenarlar hafifçe parlar; slotların nadirlik ve seçim vurguları vardır. Üstteki coin ikonunun yanında bakiye görünür; üstüne gelince tam tutar gösterilir.
- Eşya ipucunda tam alış/satış fiyatı, stok ve **yeşil ▲ / kırmızı ▼** bulunur. Ok, sunucudaki son işlemden önceki fiyata göre hesaplanır. Değişmeyen fiyatta nötr çizgi gösterilir. Büyük ekranda, seçili eşyanın bu ekran açıkken gözlenen son 16 fiyatından küçük grafik çizilir. Geçmiş uydurulmaz; sunucu yeniden başlatıldığında/yenilendiğinde önceki işlem bilgisi yeniden birikmeye başlar.
- Create sekmesi, özel Create köylü ekranı, paketleri ve köylü ticaretini değiştiren mixin kaldırılmıştır. Köylüler normal davranır. **Create eşyaları silinmez**; genel dükkanda aranıp alınıp satılabilir. Eski `createVillagersEnabled` ayarı artık kullanılmaz.
- Alış ve satış aynı ekrandadır. **Max alış** bakiye, stok, eşyanın yığın sınırı ve envanterdeki boş alanla sınırlıdır. **Max satış** satılabilir miktar, stok üst sınırı ve bakiye üst sınırıyla sınırlıdır.
- Düğmede görünen toplam, sunucunun kademeli fiyat hesabıdır. Liste ve envanter ipucu anlık birim değerini gösterir. Fareyi işlemin üstüne getirince tam toplam ve baz/min/max fiyatlar görünür; liste üstünde stok görünür.
- Fiyat başka oyuncunun işlemiyle değişirse eski fiyatlı işlem reddedilir, teklif güncellenir; yeniden tıklanabilir.
- Kitaplar, özel mob spawnerları, coin, yükseltme ve envanter satış ekranları alt menüde korunur. **V / G / K / Mouse 4** işlevleri korunur. Uncrafting geri eklenmez.
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

İlk komutun varsayılanı yalnızca **Forge 1.20.1**. Üretim JAR'ı `versions/forge-legacy/build/1.20.1/libs/forge-balanced-shop-1.20.1-2.2.0.jar` altında; `dev` JAR kullanılmamalıdır. Eski Balanced Shop JAR'ını kaldırıp bunu koyun. Çerçeveler kodla çizilir; harici kaynak paketi gerekmez.

Uygulama temeli: resmi Forge [menü belgeleri](https://docs.minecraftforge.net/en/1.20.1/gui/menus/), [SimpleImpl paketleri](https://docs.minecraftforge.net/en/1.20.1/networking/simpleimpl/), [SavedData](https://docs.minecraftforge.net/en/1.20.1/datastorage/saveddata/).
