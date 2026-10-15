# Doğrulama sonucu — 3 Ekim 2026

Minecraft Java 1.21.11, Java 21, Fabric Loader 0.18.4, Fabric API 0.141.6+1.21.11 ve Fabric Loom 1.15.4 kullanıldı.

## Otomatik testler

**12 test geçti, 0 başarısız test.**

- 5 ekonomi testi: para sınırları, UUID cüzdan kalıcılığı, bozuk dosyanın korunması, kayıt hatasının geri alınması ve fiyat kuralları.
- 7 Minecraft entegrasyon testi: tüm kayıtlı eşyalarda fiyat kapsamı; gerçek vanilla tarifleri; alış/satış bakiyesi; kapasite ve yığınlar; adlandırılmış, hasarlı ve içi dolu shulker kutularının korunması; geçersiz işlemler ve envanter geri alma.
- Pahalı malzemelerden üretilen blokların fiyatı, Fortune ile cevher düşüşleri ve doğrudan al-sat kaybı ayrıca kontrol edildi.

HTML raporu: `build/reports/tests/test/index.html`.

## Gerçek istemci ve entegre sunucu

Fabric'in istemci oyun testi ile Minecraft başlatıldı ve test dünyası oluşturuldu. Test modu dağıtılan JAR'a dahil değildir.

Vanilla testi geçti: GUI ve katalog açıldı, sunucu/istemci ağ paketleri çalıştı; elmas alışında 500 coin kesildi, satışında 125 coin eklendi; satın alınan eşya istemci envanterine ulaştı. Üçüncü taraf test eşyası keşfedildi ve fiyatlandırıldı. Yönetici reload komutu açık mağazayı güncelledi.

Create Fly **1.21.11-6.0.9-5** kurulu oyun testi de geçti. Yukarıdaki akışa ek olarak **`create:shaft` satın alma ve satma** işlemleri gerçek oyun içinde yapıldı. Render edilen Create eşyası ve başarılı satış ekran görüntüsüne kaydedildi.

Gerçek katalog kapsamı:

| Katalog | Eşya sayısı |
| --- | ---: |
| Vanilla, air hariç | 1.504 |
| Create Fly | 689 |
| Vanilla + Create Fly | 2.193 |

Vanilla testinde fiyat hesabına katılan tarif sayısı 1.380; Create Fly kurulu dünyada 2.377. Test modlarının yapay eşyaları teslim edilen CSV dosyalarından çıkarıldı.

Komutlar:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 test runClientGameTest build -PwithCompat
```

Sonuç: **BUILD SUCCESSFUL**.

Bu doğrulama belirtilen sürümlerdeki testleri ve tek oyunculu entegre sunucuyu kapsar. Farklı mod paketlerinin özel üretim tarifleri için yapılandırma fiyatları ayrıca düzenlenebilir.
