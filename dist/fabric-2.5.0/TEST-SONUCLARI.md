# Fabric Balanced Shop 2.5.0 / Minecraft 1.20.1 doğrulaması

Tarih: 11 Ekim 2026. Fabric Loader **0.18.4**, Fabric API **0.92.12+1.20.1**, Minecraft **1.20.1**, Java **17**. Gradle/Loom Java 21 ile, Minecraft ve testler Java 17 toolchain ile çalıştırıldı. Üretim sınıflarının Java 17 bytecode hedefi paketlemede ayrıca doğrulanır.

**597 sunucu + 103 istemci = 700 kontrol başarılı.** Entegre sunucuda tekrar edilen sunucu kontrolleri toplama ikinci kez eklenmez. Esas raporlar `verification/fabric-1.20.1-server.txt` ve `fabric-1.20.1-client.txt` dosyalarıdır. Paketleme eksik/başarısız raporu kabul etmez.

- Sunucu: ekonomi, tüm kayıtlı eşya fiyatları, kitap seviyeleri, spawner varyantları, fiziksel akçeler, şanslı yükseltme ve kayıp/ödül miktarları. Sıfır başlangıç bakiyesi, hasarlı aletler, tohum satışı ve maden bloklarında sıkıştırma oranları.
- Dünya SavedData kaydı, eski JSON cüzdanının taşınması/yedeklenmesi, yeniden başlatma işareti, bozuk veriyi reddetme, para taşması/eksi miktarlar ve gerçek Brigadier `/bal`, `/pay`, `/economy add/remove/set` komutları.
- Dinamik stok/fiyat sınırları, ardışık birim/toplu işlem eşitliği, eski teklifin reddi, oturum/sıra numarası ve sunucuda miktar/bakiye/envanter kontrolleri.
- Türkçe ad ve arama: İngilizce genel dil altında elmas, elmas bloğu/blogu/blok; bal kabağı/balkabağı tohumu ve Türkçe harfsiz arama; onarım/tamir, kırılmazlık ve büyü seviyeleri; köylü, demir golem ve Create eş adları. Özel isim/NBT korunur. Paket içinde 3128 vanilla çeviri anahtarı vardır.
- Alan yerleştirme: altı yönde 3–10 boyutlar, geçersiz boyutların reddi, gerçek 100 blok yerleştirme, malzeme tüketimi/refill, NBT farklarını koruma, mevcut blokları ezmeme, uzaklık/spectator/creative kontrolleri. Fabric UseBlockCallback hedef hücreyi reddettiğinde blok ve malzeme korunur. Forge olay/snapshot testleri Fabric callback eşdeğerleriyle değiştirilmiştir.
- Spawner: gerçek vanilla serverTick mixiniyle 79 tikte ganimet yok; 80. tikte bir mob ganimeti. Mob dünyaya eklenmez. Dolu depoda durma, taşan son ganimeti kaybetmeden saklama ve alan açılınca devam etme. Tam NBT yüklemesinde eşyalar, yuva konumları, sayaç ve taşan ganimet korunur. doMobLoot kapalıyken üretim durur.
- Ganimet: 64 örnekte demir golem miktar sınırları ve blaze çubuğunun bazen düşüp bazen düşmemesi; köylüde boş sonuç, kırmızı koyunda kırmızı yün, geçersiz türde güvenli boş sonuç ve katalogdaki her mobun tablosunu çalıştırma. Kırılan spawnerın birikmiş/bekleyen eşyaları tam bir kez düşer.
- Spawner deposuna dışarıdan eşya ekleme, cursor swap ve ters yönde Shift-tık engellenir. Uzaklaşınca menü geçersizleşir. **Gerçek ServerPlayerGameMode.useItemOn yolu üzerinde Fabric UseBlockCallback reddi depoyu açmayı engeller.**
- İstemci: mevcut yedi ekran, B menüsü, arama, native C2S/S2C üzerinden gerçek 64'lü alış/toplu satış, stok/bakiye/envanter senkronizasyonu, küçük ekran düzeni ve Türkçe rehberin sekiz sayfası. Create özel ekranı ve kazma servisi bulunmaz.
- P kısayolu, sekiz boyut seçeneği, 100 hücrelik çizgi önizlemesi ve önizlemede malzeme tüketilmemesi. **Minecraft.startUseItem doğrudan çağrılarak Fabric sağ tık mixininin** 100 blok yerleştirme paketi ve tam envanter tüketimi doğrulanır. Kapalı mod normal etkileşimi korur.
- Gerçek blok sağ tıklaması Türkçe RPG depo ekranını açar. 10 demir native sandık paketleriyle görünür; Shift-tıkla envantere tam aktarılır, ters yöndeki aktarım reddedilir ve menü normal envantere kapanır.

Gerçek istemci görüntüleri: `market-turkish.png`, `guide-turkish.png`, `spawner-storage.png`, `area-preview.png`.

Kapsam: standart Fabric API ve vanilla Minecraft ortamı. Create için shaft/andesite_alloy test kayıtları kullanılır; tam Create/mod paketi birlikte çalışma testi yapılmadı. Modların ganimet tabloları desteklenir; özel ölüm kodları veya farklı BlockEntity kullanan özel spawner blokları bu test kapsamının dışındadır. Üretim paketi test sürücülerini ve sentetik Create eşyalarını içermez. Forge/NeoForge bağımlılıkları bulunamaz; mixin ve kaynakların dağıtıma girdiği paketleme sırasında kontrol edilir.
