package dev.eymen.balancedshop.screen;

import dev.eymen.balancedshop.keybind.MarketKeybind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** A Turkish, in-game guide for the actual mechanics shipped in this version. */
public final class GuideScreen extends Screen {
    public record Page(String title,String text){}
    public static final List<Page> PAGES=List.of(
        new Page("Başlangıç ve kısayollar","Yeni oyuncular sıfır akçe ile başlar. Tarım ürünleri, tohumlar ve diğer satılabilir eşyalarla gelir elde et.\n\nB: Mağaza · V: Envanterden satış · G: Şanslı yükseltici · K: Akçe cüzdanı · P: Alan yerleştirme.\n\nMağazanın sağ üstündeki ? bu rehberi açar. Farenin tekerleği uzun sayfaları kaydırır."),
        new Page("Türkçe arama","Eşyaları Türkçe adlarıyla veya kayıt kimliğiyle ara: elmas, elmas bloğu, bal kabağı tohumu, demir kazma. Türkçe harf olmadan da yazabilirsin: kirilmazlik, balkabagi tohumu.\n\nBüyüler: Onarım (tamir), Kırılmazlık, Verimlilik. Üreteçler: köylü, demir golem, zombi.\n\nModların mevcut Türkçe çevirileri okunur. Çevirisi olmayan özel mod eşyalarında kayıt kimliğini de kullanabilirsin."),
        new Page("Alış, satış ve stok","B ile mağazayı aç; kategori ve eşya seç. 1×, 16×, 64× veya Tümü düğmesini kullan. Oklarla diğer miktarları seçebilirsin.\n\nSATIN AL veya SAT düğmesindeki toplamı kontrol et. Alım stok azaltır ve fiyatı yükseltir; satış stok artırır ve fiyatı düşürür. Eski teklif değişirse fiyat yenilenir, tekrar tıkla.\n\nAltın bakiye göstergesinin üzerine gelerek tam tutarı gör. Eşya ipuçlarında fiyat, stok ve yükseliş/düşüş okları bulunur."),
        new Page("Tarım ve envanter satışı","V ile envanter satışını aç. Eşyayı seçip 1, 16, 32, 64, tümü veya oklarla belirlediğin miktarı sat. Normal eşyalardaki kesin stok hesabını B ekranında görebilirsin.\n\nBal kabağı ve karpuz tohumları, buğday, patates gibi çiftlik ürünleri başlangıç geliri sağlar. Maden blokları malzeme oranıyla değerlenir: bir elmas bloğu dokuz elmaslık stoktur.\n\nAletlerin hasarı ve büyüleri satış değerini etkileyebilir."),
        new Page("Büyü kitapları ve üreteçler","B → Kitaplar: büyüyü Türkçe ara, seviyesini ve miktarını seç, satın al. Örneğin Onarım veya Kırılmazlık III.\n\nB → Üreteç: köylü, demir golem ve diğer canlı türlerini ara. Her türün ayrı fiyatı vardır. Genel çağırma yumurtaları yerine türü belirlenmiş üreteçler alınır.\n\nÜreteç mob doğurmaz. Her 4 saniyede (80 oyun tiki) bir mobun ganimet tablosu çalışır; eşyalar 54 yuvalı depoda birikir. Sağ tıklayıp depoyu aç, eşyaları al. Shift + tık topluca alır. Depoya dışarıdan eşya koyulmaz.\n\nDüşürme miktarı ve olasılığı tablodaki gibidir; Yağma büyüsü ve şans bonusu yoktur. Oyuncu öldürme şartlı ganimetler de değerlendirilir. Köylü gibi ganimetsiz canlılar eşya üretmez. Mod ve veri paketi ganimet tabloları okunur; yalnızca özel ölüm koduyla verilen ödüller bu sisteme dahil değildir.\n\nDepo dolunca üretim durur; taşan son ganimet kaybolmadan bekler. Üreteç kırılırsa birikenler yere düşer. Üretim yalnızca yüklü ve tik alan bölgelerde sürer; kapalı dünyada birikmez. doMobLoot kapalıysa üretim durur."),
        new Page("Şanslı yükseltici","G ile aç. Kaynak eşyayı seç; eşya değiştirme veya büyü ekleme modunu kullan. Hedefi Türkçe ara. Verilecek ve alınacak miktarı oklarla ayarla.\n\nŞans, kaynakların satış değeri ve hedeflerin alış değerine göre hesaplanır; en fazla %90 olur. İşlem ücretini ve riski onaylamadan önce kontrol et.\n\nBaşarısızlıkta verdiğin eşyalar ve ücret kaybolur. Başarıda seçtiğin hedef veya büyü uygulanır."),
        new Page("Akçe ve para komutları","K ile bakiyendeki parayı fiziksel akçe olarak envantere çek veya geri yatır. Bakır, gümüş, altın ve platin akçeler farklı değerlere sahiptir.\n\n/bal: bakiyeni gösterir.\n/pay Oyuncu 12.50: çevrimiçi oyuncuya para gönderir.\n\nYetkililer: /economy add Oyuncu 100, /economy remove Oyuncu 25, /economy set Oyuncu 0. Tutarlar en fazla iki ondalık basamak kullanır."),
        new Page("Çizgilerle alan yerleştirme","P ile 3×3–10×10 arasında boyut seç. Eline blok al ve bir yüzeye bak. Altın çizgiler yerleştirilecek alanı, kırmızı çizgiler dolu hücreleri gösterir. Sağ tık bir alan yerleştirir; ek fare tuşu gerekmez.\n\nYüzeye göre zemin veya duvar oluşur. Envanterden aynı tür ve özel verideki bloklar tüketilir. Malzeme yetmezse durur; mevcut katı bloklar ezilmez. Sunucu korumaları işlemi engelleyebilir.\n\nP → Tekli yerleştir normal sağ tıklamaya döner. Çizgiler tek başına blok koymaz veya kırmaz.")
    );
    private int page,scroll,x,y,w,h,maxScroll;
    private Button previous,next;
    public GuideScreen(){super(Component.literal("Dengeli Pazar · Türkçe Rehber"));}
    public static void open(){MarketKeybind.closeMarket();Minecraft.getInstance().setScreen(new GuideScreen());}
    @Override protected void init(){
        w=Math.min(480,width-12);h=Math.min(340,height-12);x=(width-w)/2;y=(height-h)/2;
        previous=addRenderableWidget(Button.builder(Component.literal("<"),b->turn(-1)).bounds(x+12,y+h-30,25,20).build());
        next=addRenderableWidget(Button.builder(Component.literal(">"),b->turn(1)).bounds(x+43,y+h-30,25,20).build());
        addRenderableWidget(Button.builder(Component.literal("Mağazaya dön"),b->onClose()).bounds(x+w-120,y+h-30,108,20).build());update();
    }
    private void turn(int delta){page=Math.max(0,Math.min(PAGES.size()-1,page+delta));scroll=0;update();}
    private void update(){previous.active=page>0;next.active=page<PAGES.size()-1;}
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        renderBackground(g);RpgTheme.frame(g,x,y,w,h,RpgTheme.GOLD,true);
        g.drawCenteredString(font,title,x+w/2,y+13,RpgTheme.GOLD);
        g.drawString(font,PAGES.get(page).title(),x+14,y+34,RpgTheme.CYAN);
        var lines=font.split(Component.literal(PAGES.get(page).text()),w-32);int viewport=h-94;
        maxScroll=Math.max(0,lines.size()*11-viewport);scroll=Math.min(scroll,maxScroll);
        g.enableScissor(x+12,y+53,x+w-12,y+h-41);
        for(int i=0;i<lines.size();i++)g.drawString(font,lines.get(i),x+16,y+55+i*11-scroll,RpgTheme.TEXT,false);
        g.disableScissor();g.drawCenteredString(font,(page+1)+" / "+PAGES.size(),x+w/2,y+h-24,RpgTheme.MUTED);
        if(maxScroll>0)g.drawString(font,"Kaydır ↕",x+78,y+h-24,RpgTheme.MUTED);
        super.render(g,mx,my,partial);
    }
    @Override public boolean mouseScrolled(double mx,double my,double delta){scroll=Math.max(0,Math.min(maxScroll,scroll-(int)(delta*22)));return true;}
    @Override public void onClose(){MarketKeybind.open();}
    @Override public boolean isPauseScreen(){return false;}
}
