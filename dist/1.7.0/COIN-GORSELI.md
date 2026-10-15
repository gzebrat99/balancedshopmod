# Coin görseli

`src/main/resources/assets/balancedshop/textures/item/coin.png`: 64×64, RGBA, şeffaf PNG. Bu proje için imagegen becerisi ve yerleşik image_gen aracıyla üretildi. Prompt: tek, önden görünen gümüş coin; ortasında büyük kabartma C; Minecraft envanterine uygun keskin piksel sanatı; yalnızca gri tonları; koyu basamaklı çerçeve ve metalik kenar; şeffaf arka plan; başka metin veya nesne yok.

Üretilen 1280×1280 görsel en yakın komşu örneklemesiyle 64×64 boyutuna getirildi; alfa korunmuştur. Kaynak görsel varsayılan generated_images konumunda bırakıldı. Bakır, gümüş, altın ve platin renkleri `assets/balancedshop/items/*_coin.json` içindeki Minecraft `minecraft:constant` tint kaynaklarıyla uygulanır; ayrı harici texture indirilmedi.

`scripts/prepare-coin-assets.ps1` geliştirme sırasında boyutlandırma/model/dil kaynaklarını hazırlamak için kullanıldı; normal Gradle build için bu betiği çalıştırmak gerekmez. GUI animasyonları Java koduyla çizilir ve bu texture'a bağımlı değildir.
