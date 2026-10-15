package dev.eymen.balancedshop.verification;

import dev.eymen.balancedshop.*;
import dev.eymen.balancedshop.client.ShopClient;
import dev.eymen.balancedshop.menu.ShopMenu;
import dev.eymen.balancedshop.network.MarketNetwork;
import dev.eymen.balancedshop.screen.MarketScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.world.item.Items;
import java.util.concurrent.CompletableFuture;

/** Drives real C2S/S2C traffic and screen widgets, including client inventory synchronization. */
public final class ClientMarketVerification {
    private static int step,wait,checks,oldScale;
    private static CompletableFuture<Void> setup;
    private static long buyTotal,sellTotal;
    public static int checks(){return checks;}
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);checks++;}
    private static Object get(Object o,String field) throws Exception {var f=o.getClass().getDeclaredField(field);f.setAccessible(true);return f.get(o);}
    private static void set(Object o,String field,Object value) throws Exception {var f=o.getClass().getDeclaredField(field);f.setAccessible(true);f.set(o,value);}
    private static void advance(){step++;wait=0;}
    public static boolean tick(Minecraft c) throws Exception {
        if(++wait>300)throw new AssertionError("Market GUI test timeout at "+step+" screen="+c.screen);
        if(step==0){
            var server=c.getSingleplayerServer();var id=c.player.getUUID();
            setup=CompletableFuture.runAsync(()->{
                try{var p=server.getPlayerList().getPlayer(id);p.getInventory().clearContent();BalancedShop.wallets().set(id,100_000_000);BalancedShop.wallets().data().stock("minecraft:diamond",4096);p.inventoryMenu.sendAllDataToRemote();}
                catch(Exception e){throw new RuntimeException(e);}
            },server);advance();return false;
        }
        if(step==1){if(!setup.isDone())return false;setup.join();ShopClient.open();advance();return false;}
        if(step>=2&&step<=10){
            if(!(c.screen instanceof MarketScreen s))return false;
            MarketNetwork.Page data=(MarketNetwork.Page)get(s,"data");if(data==null)return false;
            if(step==2){
                check(c.player.containerMenu instanceof ShopMenu,"B/shop opens a registered server container");
                @SuppressWarnings("unchecked") var tabs=(java.util.List<Button>)get(s,"tabs");
                check(tabs.size()==6&&tabs.stream().allMatch(b->b.getX()<((EditBox)uncheckedGet(s,"search")).getX()),"Icon categories form a left sidebar");
                check(tabs.stream().noneMatch(b->b.getMessage().getString().contains("Create")),"Create category removed");
                check((Integer)get(s,"columns")>=3&&(Integer)get(s,"capacity")>=9,"Responsive multi-column item grid");
                for(String label:java.util.List.of("1x","16x","64x","Tümü"))check(s.children().stream().anyMatch(w->w instanceof Button b&&b.getMessage().getString().equals(label)),"Quantity preset "+label);
                try{Class.forName("dev.eymen.balancedshop.client.CreateTraderScreen");throw new AssertionError("Create screen remains");}catch(ClassNotFoundException expected){check(true,"Create GUI class removed");}
                set(s,"selected","minecraft:diamond");set(s,"quantity",64);((EditBox)get(s,"search")).setValue("minecraft:diamond");advance();return false;
            }
            if(step==3){
                var b=(Button)get(s,"buy");if(!data.selected().equals("minecraft:diamond")||data.buy().quantity()!=64||!b.active)return false;
                check(data.rows().stream().allMatch(r->r.id().contains("minecraft:diamond")),"EditBox search filters native response");
                buyTotal=data.buy().total();b.onPress();advance();return false;
            }
            if(step==4){
                if(!data.message().equals("bought")&&!get(s,"status").equals("Alış tamamlandı"))return false;
                check(c.player.getInventory().countItem(Items.DIAMOND)==64,"Actual 64x purchase reaches client inventory");
                check(data.balance()==100_000_000-buyTotal,"GUI purchase charges exact displayed total");
                check(data.rows().stream().filter(r->r.id().equals("minecraft:diamond")).findFirst().orElseThrow().stock()==4032,"GUI purchase updates stock");
                var rising=data.rows().stream().filter(r->r.id().equals("minecraft:diamond")).findFirst().orElseThrow();
                check(MarketScreen.priceTooltip(rising).stream().anyMatch(t->t.getString().contains("▲")&&t.getStyle().getColor().getValue()==0x72E5A2),"Price tooltip shows real green upward movement");
                check(MarketScreen.priceTooltip(rising).stream().anyMatch(t->t.getString().equals("Stok: 4032")),"Price tooltip shows synchronized stock");
                net.minecraft.client.Screenshot.grab(new java.io.File("."),"market-dynamic-trade.png",c.getMainRenderTarget(),notice->{});
                ((Button)s.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("Tümü")).findFirst().orElseThrow()).onPress();advance();return false;
            }
            if(step==5){
                if(wait<8)return false;
                var b=(Button)get(s,"sell");if(data.sell().quantity()!=64||!b.active||((Integer)get(s,"quantity"))!=0)return false;
                sellTotal=data.sell().total();b.onPress();advance();return false;
            }
            if(step==6){
                if(wait<8)return false;
                if(!data.message().equals("sold")&&!get(s,"status").equals("Satış tamamlandı"))return false;
                check(c.player.getInventory().countItem(Items.DIAMOND)==0,"Actual Max sale removes items on client");
                check(data.balance()==100_000_000-buyTotal+sellTotal&&sellTotal<buyTotal,"GUI sale conserves money and spread");
                check(data.rows().stream().filter(r->r.id().equals("minecraft:diamond")).findFirst().orElseThrow().stock()==4096,"GUI sale restores stock");
                var falling=data.rows().stream().filter(r->r.id().equals("minecraft:diamond")).findFirst().orElseThrow();
                check(MarketScreen.priceTooltip(falling).stream().anyMatch(t->t.getString().contains("▼")&&t.getStyle().getColor().getValue()==0xEF7888),"Price tooltip shows real red downward movement");
                long seq=(Long)get(s,"sequence")+1;set(s,"sequence",seq);
                var menu=s.getMenu();MarketNetwork.CHANNEL.sendToServer(new MarketNetwork.Request(menu.containerId,menu.session,seq,1,"minecraft:diamond",1,"minecraft:diamond","",0,-1));
                advance();return false;
            }
            if(step==7){
                if(!data.message().equals("changed")&&!get(s,"status").equals("Fiyat yenilendi"))return false;
                check(c.player.getInventory().countItem(Items.DIAMOND)==0,"Stale price request cannot buy items");
                check(data.balance()==100_000_000-buyTotal+sellTotal,"Stale price request cannot change money");
                s.resize(c,320,240);
                check(s.children().stream().filter(w->w instanceof AbstractWidget).map(w->(AbstractWidget)w).allMatch(w->w.getX()>=0&&w.getY()>=0&&w.getX()+w.getWidth()<=320&&w.getY()+w.getHeight()<=240),"GUI fits minimum 320x240 scaled resolution");
                EditBox search=(EditBox)get(s,"search");search.setFocused(true);s.setFocused(search);
                check(s.keyPressed(69,0,0)&&c.screen==s,"Inventory key does not close focused search");
                s.resize(c,c.getWindow().getGuiScaledWidth(),c.getWindow().getGuiScaledHeight());advance();return false;
            }
            if(step==8){
                if(wait<10)return false;
                oldScale=c.options.guiScale().get();c.options.guiScale().set(1);c.resizeDisplay();
                ((EditBox)get(s,"search")).setValue("");
                @SuppressWarnings("unchecked") var tabs=(java.util.List<Button>)get(s,"tabs");tabs.get(1).onPress();advance();return false;
            }
            if(step==9){
                if(wait<25||data.rows().size()!=30)return false;
                check((Integer)get(s,"capacity")==30,"Large screen receives a full 30-slot server page");
                check(s.children().stream().filter(w->w instanceof AbstractWidget).map(w->(AbstractWidget)w).allMatch(w->w.getX()>=0&&w.getY()>=0&&w.getX()+w.getWidth()<=s.width&&w.getY()+w.getHeight()<=s.height),"Large layout stays inside screen");
                net.minecraft.client.Screenshot.grab(new java.io.File("."),"market-rpg-large.png",c.getMainRenderTarget(),notice->{});
                advance();return false;
            }
            if(step==10){
                if(wait<10)return false;
                c.options.guiScale().set(oldScale);c.resizeDisplay();
                ((Button)s.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("Kitaplar")).findFirst().orElseThrow()).onPress();advance();return false;
            }
        }
        if(step==11){
            if(!(c.screen instanceof dev.eymen.balancedshop.client.BookScreen)||get(c.screen,"data")==null)return false;
            check(c.player.containerMenu==c.player.inventoryMenu,"Book navigation closes shop container correctly");
            check(get(c.screen,"data")!=null,"Existing book screen receives packets after menu navigation");
            c.setScreen(null);net.minecraft.client.KeyMapping.click(dev.eymen.balancedshop.placement.PlacementClient.MENU.getKey());advance();return false;
        }
        if(step==12){
            if(!(c.screen instanceof dev.eymen.balancedshop.placement.PlacementScreen s))return false;
            check(true,"P key opens placement size screen");
            for(int size=3;size<=10;size++){
                final String label=size+"×"+size;
                var b=(Button)s.children().stream().filter(w->w instanceof Button btn&&btn.getMessage().getString().equals(label)).findFirst().orElseThrow();
                b.onPress();check(dev.eymen.balancedshop.placement.PlacementClient.size()==size,"Placement preset "+label);
            }
            s.resize(c,320,240);check(s.children().stream().filter(w->w instanceof AbstractWidget).map(w->(AbstractWidget)w).allMatch(w->w.getX()>=0&&w.getY()>=0&&w.getX()+w.getWidth()<=320&&w.getY()+w.getHeight()<=240),"Placement screen fits 320x240");
            s.resize(c,c.getWindow().getGuiScaledWidth(),c.getWindow().getGuiScaledHeight());advance();return false;
        }
        if(step==13){
            if(wait<10)return false;
            net.minecraft.client.Screenshot.grab(new java.io.File("."),"area-placement.png",c.getMainRenderTarget(),notice->{});c.setScreen(null);
            var server=c.getSingleplayerServer();var id=c.player.getUUID();
            setup=CompletableFuture.runAsync(()->{
                var p=server.getPlayerList().getPlayer(id);p.getInventory().clearContent();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                var center=new net.minecraft.core.BlockPos(140,80,140);
                for(var pos:dev.eymen.balancedshop.placement.AreaPlacement.plane(center,net.minecraft.core.Direction.UP,10)){
                    p.serverLevel().getChunkAt(pos);p.serverLevel().setBlockAndUpdate(pos.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                    for(int dy=0;dy<5;dy++)p.serverLevel().setBlockAndUpdate(pos.above(dy),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
                p.getInventory().setItem(p.getInventory().selected,new net.minecraft.world.item.ItemStack(Items.COBBLESTONE,64));
                p.getInventory().setItem((p.getInventory().selected+1)%9,new net.minecraft.world.item.ItemStack(Items.COBBLESTONE,36));p.inventoryMenu.sendAllDataToRemote();
                p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();
                p.connection.teleport(140.5,82,140.5,0,90);
            },server);advance();return false;
        }
        if(step==14){
            if(!setup.isDone()||wait<20||c.player.getInventory().countItem(Items.COBBLESTONE)!=100)return false;setup.join();
            dev.eymen.balancedshop.placement.PlacementClient.disable();
            var normal=new net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered(1,c.options.keyUse,net.minecraft.world.InteractionHand.MAIN_HAND);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(normal);check(!normal.isCanceled(),"Disabled area mode preserves vanilla right-click");
            dev.eymen.balancedshop.placement.PlacementClient.select(10);check(dev.eymen.balancedshop.placement.PlacementClient.enabled(),"Choosing size enables area mode without a held modifier");advance();return false;
        }
        if(step==15){
            if(wait<15)return false;
            check(c.player.getInventory().countItem(Items.COBBLESTONE)==100,"Preview alone consumes nothing");
            check(c.level.getBlockState(new net.minecraft.core.BlockPos(140,80,140)).isAir(),"Preview alone never places or breaks blocks");
            check(dev.eymen.balancedshop.placement.PlacementClient.previewCells()==100,"100 wireframe cells render before any right-click");
            net.minecraft.client.Screenshot.grab(new java.io.File("."),"area-preview.png",c.getMainRenderTarget(),notice->{});
            var input=new net.minecraftforge.client.event.InputEvent.InteractionKeyMappingTriggered(1,c.options.keyUse,net.minecraft.world.InteractionHand.MAIN_HAND);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(input);check(input.isCanceled(),"Area placement cancels duplicate vanilla use");
            advance();return false;
        }
        if(step==16){
            if(wait<15||c.player.getInventory().countItem(Items.COBBLESTONE)!=0)return false;
            check(c.player.getInventory().countItem(Items.COBBLESTONE)==0,"Native placement packet consumes exactly 100 client materials");
            var center=new net.minecraft.core.BlockPos(140,80,140);
            check(dev.eymen.balancedshop.placement.AreaPlacement.plane(center,net.minecraft.core.Direction.UP,10).stream().allMatch(pos->c.level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.COBBLESTONE)),"100 placed blocks synchronize to the client");
            dev.eymen.balancedshop.placement.PlacementClient.disable();check(!dev.eymen.balancedshop.placement.PlacementClient.enabled()&&dev.eymen.balancedshop.placement.PlacementClient.previewCells()==0,"Disabling area mode clears preview");
            ShopClient.open();advance();return false;
        }
        if(step==17){
            if(!(c.screen instanceof MarketScreen s)||get(s,"data")==null)return false;
            check(dev.eymen.balancedshop.localization.TurkishText.name(new net.minecraft.world.item.ItemStack(Items.DIAMOND)).getString().equals("Elmas"),"Turkish item display under the client's English locale");
            check(net.minecraft.client.resources.language.I18n.get("shop.balancedshop.buy").equals("Satın al"),"Turkish UI fallback under English locale");
            check(net.minecraft.client.resources.language.I18n.get("books.balancedshop.search").contains("onarım"),"Turkish enchantment search hint");
            ((EditBox)get(s,"search")).setValue("bal kabagi tohumu");advance();return false;
        }
        if(step==18){
            if(!(c.screen instanceof MarketScreen s)||wait<15)return false;
            var data=(MarketNetwork.Page)get(s,"data");if(data==null||data.rows().isEmpty()||data.rows().stream().noneMatch(r->r.id().equals("minecraft:pumpkin_seeds")))return false;
            if(!data.selected().equals("minecraft:pumpkin_seeds")||!get(s,"selected").equals("minecraft:pumpkin_seeds"))return false;
            check(data.rows().stream().allMatch(r->r.id().equals("minecraft:pumpkin_seeds")),"ASCII Turkish search returns pumpkin seeds through native packets");
            check(data.selected().equals("minecraft:pumpkin_seeds"),"Turkish search replaces the previous hidden selection and quote");
            check(MarketScreen.priceTooltip(data.rows().get(0)).get(0).getString().equals("Bal Kabağı Tohumu"),"Market item tooltip uses the Turkish name");
            net.minecraft.client.Screenshot.grab(new java.io.File("."),"market-turkish.png",c.getMainRenderTarget(),notice->{});
            ((Button)s.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals("?")).findFirst().orElseThrow()).onPress();advance();return false;
        }
        if(step==19){
            if(!(c.screen instanceof dev.eymen.balancedshop.screen.GuideScreen guide)||wait<10)return false;
            check(c.player.containerMenu==c.player.inventoryMenu,"Opening guide closes the real shop container");
            check(guide.getTitle().getString().contains("Türkçe Rehber"),"Guide has a Turkish title");
            check(dev.eymen.balancedshop.screen.GuideScreen.PAGES.size()==8,"Guide covers eight gameplay topics");
            net.minecraft.client.Screenshot.grab(new java.io.File("."),"guide-turkish.png",c.getMainRenderTarget(),notice->{});
            var next=(Button)guide.children().stream().filter(w->w instanceof Button b&&b.getMessage().getString().equals(">")).findFirst().orElseThrow();
            for(int page=1;page<8;page++){next.onPress();check((Integer)get(guide,"page")==page,"Guide page navigation "+page);}
            guide.resize(c,320,240);check(guide.children().stream().filter(w->w instanceof AbstractWidget).map(w->(AbstractWidget)w).allMatch(w->w.getX()>=0&&w.getY()>=0&&w.getX()+w.getWidth()<=320&&w.getY()+w.getHeight()<=240),"Turkish guide fits minimum screen");
            guide.onClose();advance();return false;
        }
        if(step==20){
            if(!(c.screen instanceof MarketScreen)||!(c.player.containerMenu instanceof ShopMenu))return false;
            check(true,"Guide returns to a fresh native shop container");
            var server=c.getSingleplayerServer();var id=c.player.getUUID();
            setup=CompletableFuture.runAsync(()->{
                var p=server.getPlayerList().getPlayer(id);p.closeContainer();p.getInventory().clearContent();
                var pos=new net.minecraft.core.BlockPos(145,82,140);var level=p.serverLevel();level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.SPAWNER.defaultBlockState());
                var spawner=(net.minecraft.world.level.block.entity.SpawnerBlockEntity)level.getBlockEntity(pos);
                MobSpawnerItem.configure(spawner,level,pos,"minecraft:iron_golem");
                var store=dev.eymen.balancedshop.spawner.SpawnerLoot.storage(spawner);store.setItem(0,new net.minecraft.world.item.ItemStack(Items.IRON_INGOT,10));store.errorCooldown=1200;
                p.connection.teleport(144.5,82,140.5,0,0);
                var event=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,net.minecraft.world.InteractionHand.MAIN_HAND,pos,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.WEST,pos,false));
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
                if(!event.isCanceled()||!(p.containerMenu instanceof dev.eymen.balancedshop.spawner.SpawnerLoot.StorageMenu))throw new AssertionError("Right click did not open native spawner storage");
            },server);advance();return false;
        }
        if(step==21){
            if(!setup.isDone())return false;setup.join();
            if(!(c.screen instanceof net.minecraft.client.gui.screens.inventory.ContainerScreen)||!(c.player.containerMenu instanceof net.minecraft.world.inventory.ChestMenu menu)||wait<15||!menu.getSlot(0).getItem().is(Items.IRON_INGOT))return false;
            check(menu.getRowCount()==6,"Right-click spawner opens 54-slot native storage GUI");
            check(c.screen instanceof dev.eymen.balancedshop.screen.SpawnerStorageScreen,"Spawner storage uses the Turkish RPG screen");
            check(c.screen.getTitle().getString().contains("Üreteç Deposu"),"Spawner storage title is Turkish");
            check(menu.getSlot(0).getItem().getCount()==10,"Stored loot synchronizes through native container packets");
            net.minecraft.client.Screenshot.grab(new java.io.File("."),"spawner-storage.png",c.getMainRenderTarget(),notice->{});
            c.gameMode.handleInventoryMouseClick(menu.containerId,0,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,c.player);advance();return false;
        }
        if(step==22){
            if(wait<10||c.player.getInventory().countItem(Items.IRON_INGOT)!=10)return false;
            check(c.player.containerMenu.getSlot(0).getItem().isEmpty(),"Shift-click removes the stored loot");
            check(c.player.getInventory().countItem(Items.IRON_INGOT)==10,"Taking loot credits exactly 10 iron to client inventory");
            int slot=-1;for(int i=54;i<c.player.containerMenu.slots.size();i++)if(c.player.containerMenu.getSlot(i).getItem().is(Items.IRON_INGOT)){slot=i;break;}
            c.gameMode.handleInventoryMouseClick(c.player.containerMenu.containerId,slot,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,c.player);advance();return false;
        }
        if(step==23){
            if(wait<10)return false;
            check(c.player.getInventory().countItem(Items.IRON_INGOT)==10&&c.player.containerMenu.getSlot(0).getItem().isEmpty(),"Server rejects shift-click insertion into output-only storage");
            c.player.closeContainer();advance();return false;
        }
        if(step==24){
            if(c.player.containerMenu!=c.player.inventoryMenu||wait<10)return false;
            check(true,"Spawner storage closes to the normal player inventory");return true;
        }
        return false;
    }
    private static Object uncheckedGet(Object o,String name){try{return get(o,name);}catch(Exception e){throw new RuntimeException(e);}}
}
