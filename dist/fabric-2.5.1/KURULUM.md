# Fabric Balanced Shop 2.5.1 / Minecraft 1.20.1

Gerekenler: Minecraft **1.20.1**, Java **17 veya üzeri**, Fabric Loader **0.16.9 veya üzeri**, Fabric API **0.92.2+1.20.1 veya üzeri aynı Minecraft sürümüne ait paket**. İstemci ve sunucuda aynı mod JAR'ını kullan. Eski Balanced Shop JAR'ını kaldırıp `fabric-balanced-shop-1.20.1-2.5.1.jar` dosyasını `mods` klasörüne koy. Fabric API ayrı kurulur; dağıtım JAR'ına gömülmez.

**2.5.1 uyumluluk düzeltmesi:** Loader 0.16.9 ile çalışacak şekilde derlenir. Bu yükleyicide Fabric API **0.92.2+1.20.1** kullan; API 0.92.12 daha yeni Loader ister. Başka modların daha yüksek Loader gereksinimleri varsa ayrıca karşılanmalıdır. Derleme ve JAR bağımlılık sınırı aynı sürüm matrisinden alınır.

- **B:** Türkçe dinamik pazar; kategori, Türkçe arama, stok/fiyat ve toplu alış/satış. **B → ?:** sekiz sayfalık Türkçe rehber.
- **V:** envanterden satış. **G:** eşya/büyü yükseltme; en çok %90 şans, başarısızlıkta eşya ve ücret kaybolur. **K:** fiziksel akçeler.
- **P:** 3×3–10×10 alan yerleştirme. Çizgileri kontrol et ve sağ tıkla yerleştir; tekli moda aynı panelden dön. Kazma kısayolu ve Create'e özel ticaret ekranı yoktur.
- `/bal`, `/pay Oyuncu 12.50`, yetkililer için `/economy add/remove/set Oyuncu 100`.

Normal Minecraft spawnerları, eskiden yerleştirilmiş olanlar dahil, mob doğurmaz. Bölge yüklü ve tik alıyorken her **80 oyun tikinde (normal hızda 4 saniye)** içindeki bir mobun ganimet tablosu bir kez çalışır. Sağ tık **Üreteç Deposu** ekranını açar. 54 yuvadan tıklayarak veya Shift+tıkla eşyaları al; dışarıdan eşya koyulmaz. Depo dolunca üretim durur; sığmayan son ganimet saklanır, yer açılınca aktarılır. Depo, sayaç ve bekleyen ganimet blok NBT kaydına yazılır. Spawner kırılınca içindekiler yere düşer. Dünya kapalıyken çevrimdışı üretim yoktur. `doMobLoot=false` üretimi durdurur.

Olasılık/miktar tablodaki gibidir. Bağlam: mobun SpawnData verileri, normal oyuncu öldürmesi, Yağma 0, şans 0. Oyuncu öldürme şartlı blaze çubuğu gibi ganimetler dahil edilir. Köylü gibi boş tablolu moblar eşya üretmez. Fabric modları ve veri paketlerinin standart ganimet tabloları desteklenir; özel ölüm kodları, ekipman düşürme ve farklı BlockEntity kullanan özel spawner blokları kapsam dışıdır. Forge'a özgü global loot modifier sistemi Fabric sürümünde bulunmaz.

Kod: ortak ekonomi ve oyun mantığı `versions/forge1201/java` kaynaklarından alınır. Yalnızca Fabric 1.20.1 için `versions/fabric1201/integrate.gradle` uyarlaması ve bu dizindeki yerel Fabric sınıfları uygulanır. Ana/istemci kaynakları ayrıdır. Paketler Fabric Networking API ile okunur, sunucu iş parçacığında doğrulanır. Menüler ExtendedScreenHandlerType ve standart sandık protokolünü kullanır. Blok etkileşimi spawnerın normal `use` yolunda çalışır; Fabric UseBlockCallback korumaları önce değerlendirilir. Alanın her hücresinde de callback kontrolü yapılır. Forge olay/snapshot API'leri Fabric sürümüne dahil edilmez.

Ganimet bağlamı için Fabric'in [FakePlayer API'si](https://maven.fabricmc.net/docs/fabric-api-0.91.0%2B1.20.1/net/fabricmc/fabric/api/entity/FakePlayer.html), çizgi önizlemesi için WorldRenderEvents kullanılır. Gerçek mob dünyaya eklenmez. Modun arayüz dili Türkçedir; Minecraft'ın genel dili değişmez. Kurulu modların mevcut Türkçe dil dosyaları da okunur.

Derleme: `powershell -ExecutionPolicy Bypass -File scripts/build-versions.ps1 -Loader fabric -Minecraft 1.20.1`

Doğrulama: `powershell -ExecutionPolicy Bypass -File scripts/verify-version.ps1 -Loader fabric -Minecraft 1.20.1 -Phase both`

Paketleme: `powershell -ExecutionPolicy Bypass -File scripts/package-fabric1201.ps1`

Dağıtım JAR'ı `versions/fabric/build/1.20.1/libs/fabric-balanced-shop-1.20.1-2.5.1.jar`; geliştirme veya kaynak JAR'larını kurma. Paketleme, sürüm/Java/loader bilgilerini ve gerçek test raporlarını doğrular; Forge/NeoForge veya test sınıfları sızarsa başarısız olur. Forge 2.5.0 yayını korunur; başka Minecraft sürümü bu güncellemede derlenmez.
