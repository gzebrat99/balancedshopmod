# Forge 1.20.1 / Balanced Shop 2.1.0 doğrulaması

Tarih: 10 Ekim 2026. Gerçek Minecraft 1.20.1, Forge **47.3.10**, Java **17.0.20.1** ile çalıştırıldı. Başka Minecraft/yükleyici hedefi bu yayın için derlenmedi.

- **270 sunucu kontrolü geçti.** Önceki ekonominin, kitapların, spawnerların, coinlerin, yükseltmenin ve vein mining'in 189 kontrolü; yeni piyasanın ve dünya ekonomisinin 81 kontrolü.
- **49 istemci kontrolü geçti.** Sekiz GUI gerçek yerel istemci/sunucu paketleriyle açıldı; ayrıca B kısayolu, EditBox araması, 64'lü alış, Max satış, istemci envanter senkronizasyonu, kesin toplamlar, stok değişimi, eski teklifin reddi, 320×240 yerleşimi, arama odağı ve kitap ekranına container kapatarak geçiş doğrulandı.
- Dünya `data/balancedshop_economy.dat` dosyasından gerçek disk okuması yapıldı. Sunucu yeniden başlatıldığında önceki **24680 kuruş** test işareti doğru okundu.
- JSON geçişi, yedekleme, yeniden başlatmada tekrar para yüklememe, bozuk kaydı reddetme; eksi/taşan/kişinin kendisine yaptığı transferler test edildi.
- `/bal`, `/pay`, `/economy add/remove/set` gerçek Brigadier komut ağacında çalıştırıldı; izin seviyesi ve kuruş hassasiyeti doğrulandı.
- Stok azalınca fiyat artışı, satışla düşüş, min/max sınırları, stok tükenmesi, dolu envanter, parasız alım, tohum satışı, hasarlı aletler, tekli/toplu fiyat eşitliği ve bütün tanımlı sıkıştırılmış blokların malzeme oranları doğrulandı.
- Sunucunun sakladığı teklif ile işlem karşılaştırılır; değişen Max miktarı, yabancı container/oturum ve yinelenen sıra numarası kabul edilmez.

Toplam **319 farklı kontrol**. Entegre sunucuda tekrarlanan sunucu kontrolleri bu toplama ikinci kez eklenmedi. `verification/server.txt`, `client.txt`, `market.txt` raporları yayın paketinde bulunur.

İstemci ekran görüntüsü: yayın paketindeki `market-preview.png`. Üretim JAR'ı paketlenirken mod sürümü, Forge alt sınırı, Java 17 sınıfları, SRG/Mixin eşlemeleri, yeni modüller ve test sınıflarının dışarıda kaldığı ayrıca doğrulanır.

Kapsam: Create uyumluluğunun mevcut otomatik testleri `create:shaft` ve `create:andesite_alloy` kayıtlarını test eşyaları olarak ekler. Tam Create modu/mod paketi kurulmuş bir birlikte çalışma testi değildir. Yeni modların özel NBT/enerji değerleri bu test kapsamının dışındadır. Kaynak kodunda GUI açıklamaları ve mimari ayrıntılar `README.md` dosyasındadır.
