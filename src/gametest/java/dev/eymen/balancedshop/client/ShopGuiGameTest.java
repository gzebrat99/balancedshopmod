package dev.eymen.balancedshop.client;

import dev.eymen.balancedshop.ShopPackets;
import dev.eymen.balancedshop.UncraftingPackets;
import dev.eymen.balancedshop.SellPackets;
import dev.eymen.balancedshop.BookPackets;
import dev.eymen.balancedshop.BookCatalogue;
import dev.eymen.balancedshop.BookPurchase;
import dev.eymen.balancedshop.ShopConfig;
import dev.eymen.balancedshop.WalletStore;
import dev.eymen.balancedshop.Money;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.item.Items;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

@SuppressWarnings({"UnstableApiUsage","unchecked"})
public final class ShopGuiGameTest implements FabricClientGameTest {
    private static <T> T field(Object screen,String name) {
        try { Field field=screen.getClass().getDeclaredField(name);field.setAccessible(true);return (T)field.get(screen); }
        catch(Exception e) { throw new AssertionError(e); }
    }
    private static ShopPackets.Page page(ShopScreen screen) { return field(screen,"data"); }
    private static UncraftingPackets.Preview preview(UncraftingScreen screen){return field(screen,"data");}
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    @Override public void runTest(ClientGameTestContext context) {
        try(TestSingleplayerContext world=context.worldBuilder().create()) {
            world.getServer().runCommand("gamemode survival @a");
            context.runOnClient(client->ShopClient.open());
            context.waitFor(client->client.screen instanceof ShopScreen screen && page(screen)!=null);
            context.runOnClient(client->{
                ShopScreen screen=(ShopScreen)client.screen;
                check(page(screen).total()>1400,"All registered items should be in the shop");
                check(page(screen).balance()==0,"Starting wallet should be zero");
            });
            context.waitTicks(3);
            context.runOnClient(client->client.getToastManager().clear());context.waitTicks(2);Path screenshot=context.takeScreenshot("balanced-shop-catalogue");
            try { Path report=Path.of("build/verification/shop-gui.png");Files.createDirectories(report.getParent());Files.copy(screenshot,report,StandardCopyOption.REPLACE_EXISTING); }
            catch(Exception e) { throw new AssertionError(e); }
            context.runOnClient(client->((EditBox)field((ShopScreen)client.screen,"search")).setValue("minecraft:diamond"));
            context.waitFor(client->page((ShopScreen)client.screen).entries().stream().anyMatch(e->e.id().equals("minecraft:diamond")));
            context.runOnClient(client->{
                ShopScreen screen=(ShopScreen)client.screen;
                List<ShopPackets.Entry> entries=page(screen).entries();
                int index=0;while(!entries.get(index).id().equals("minecraft:diamond")) index++;
                ((List<Button>)field(screen,"slots")).get(index).onPress(null);
            });
            world.getServer().runCommand("give @a minecraft:diamond 16");context.waitTicks(5);
            context.runOnClient(client->((Button)field(client.screen,"amount")).onPress(null));
            context.clickScreenButton("shop.balancedshop.sell");
            context.waitFor(client->page((ShopScreen)client.screen).message().equals("sold"));
            context.runOnClient(client->check(page((ShopScreen)client.screen).balance()==400000,"Selling 16 diamonds should earn 4000 coins"));
            context.runOnClient(client->{((Button)field(client.screen,"amount")).onPress(null);((Button)field(client.screen,"amount")).onPress(null);});
            context.waitTicks(5);
            context.clickScreenButton("shop.balancedshop.buy");
            context.waitFor(client->page((ShopScreen)client.screen).message().equals("bought"));
            context.runOnClient(client->{
                check(page((ShopScreen)client.screen).balance()==200000,"Purchase should debit 2000 coins");
                check(client.player.getInventory().contains(new net.minecraft.world.item.ItemStack(Items.DIAMOND)),"Purchased item should sync to client inventory");
            });
            context.waitTicks(5);
            context.clickScreenButton("shop.balancedshop.sell");
            context.waitFor(client->page((ShopScreen)client.screen).message().equals("sold"));
            context.runOnClient(client->check(page((ShopScreen)client.screen).balance()==225000,"Sale should credit 250 coins"));
            context.runOnClient(client->((EditBox)field((ShopScreen)client.screen,"search")).setValue("shopcompat_test:machine"));
            context.waitFor(client->page((ShopScreen)client.screen).entries().stream().anyMatch(e->e.id().equals("shopcompat_test:machine")));
            context.runOnClient(client->{
                var entry=page((ShopScreen)client.screen).entries().getFirst();
                check(entry.buy()>0 && entry.buyable() && entry.sellable(),"Third-party items should be discovered and priced");
            });
            if(FabricLoader.getInstance().isModLoaded("create")) {
                context.runOnClient(client->((EditBox)field((ShopScreen)client.screen,"search")).setValue("create:shaft"));
                context.waitFor(client->page((ShopScreen)client.screen).entries().stream().anyMatch(e->e.id().equals("create:shaft")));
                context.runOnClient(client->{
                    ShopScreen screen=(ShopScreen)client.screen;
                    List<ShopPackets.Entry> entries=page(screen).entries();
                    int index=0;while(!entries.get(index).id().equals("create:shaft")) index++;
                    var entry=entries.get(index);
                    check(entry.buy()>0 && entry.buyable(),"Create shaft should have a valid price");
                    ((List<Button>)field(screen,"slots")).get(index).onPress(null);
                });
                context.clickScreenButton("shop.balancedshop.buy");
                context.waitFor(client->page((ShopScreen)client.screen).message().equals("bought"));
                context.waitTicks(5);context.clickScreenButton("shop.balancedshop.sell");
                context.waitFor(client->page((ShopScreen)client.screen).message().equals("sold"));
                context.runOnClient(client->client.getToastManager().clear());context.waitTicks(2);context.takeScreenshot("balanced-shop-create-trade");
            }
            world.getServer().runCommand("shopadmin reload");context.waitTicks(5);
            context.runOnClient(client->check(page((ShopScreen)client.screen).message().equals("reloaded"),"Configuration reload should update open shops"));
            context.runOnClient(client->client.setScreen(null));
            world.getServer().runCommand("clear @a");world.getServer().runCommand("give @a minecraft:oak_planks 8");context.waitTicks(5);
            context.runOnClient(client->ShopClient.openUncraft());
            context.waitFor(client->client.screen instanceof UncraftingScreen screen && preview(screen)!=null && preview(screen).consume()==4);
            context.runOnClient(client->{
                var data=preview((UncraftingScreen)client.screen);check(data.fee()==0,"Planks should uncraft without a fee");
                check(data.returns().stream().anyMatch(m->m.id().equals("minecraft:oak_log")&&m.count()==1),"Four planks should recover one oak log");
            });
            context.runOnClient(client->client.getToastManager().clear());context.waitTicks(2);context.takeScreenshot("uncrafting-table-preview");
            context.clickScreenButton("uncraft.balancedshop.uncraft");
            context.waitFor(client->preview((UncraftingScreen)client.screen).message().equals("done"));
            context.runOnClient(client->{
                check(client.player.getInventory().getItem(0).getCount()==4,"Must consume exactly four planks");
                check(client.player.getInventory().contains(new net.minecraft.world.item.ItemStack(Items.OAK_LOG)),"Recovered log should synchronize");
            });
            context.runOnClient(client->client.setScreen(null));
            world.getServer().runCommand("clear @a");world.getServer().runCommand("give @a shopcompat_test:machine");context.waitTicks(5);
            context.runOnClient(client->ShopClient.openUncraft());
            context.waitFor(client->client.screen instanceof UncraftingScreen screen && preview(screen)!=null && preview(screen).total()>0);
            context.clickScreenButton("uncraft.balancedshop.uncraft");
            context.waitFor(client->preview((UncraftingScreen)client.screen).message().equals("done"));
            context.runOnClient(client->check(client.player.getInventory().contains(new net.minecraft.world.item.ItemStack(Items.IRON_INGOT)),"Third-party recipe must recover materials"));
            if(FabricLoader.getInstance().isModLoaded("create")){
                context.runOnClient(client->client.setScreen(null));
                world.getServer().runCommand("clear @a");world.getServer().runCommand("give @a create:shaft 64");context.waitTicks(5);
                context.runOnClient(client->ShopClient.openUncraft());
                context.waitFor(client->client.screen instanceof UncraftingScreen screen && preview(screen)!=null && preview(screen).total()>0);
                context.runOnClient(client->client.getToastManager().clear());context.waitTicks(2);context.takeScreenshot("uncrafting-create-preview");
                context.clickScreenButton("uncraft.balancedshop.uncraft");
                context.waitFor(client->preview((UncraftingScreen)client.screen).message().equals("done"));
                context.runOnClient(client->client.getToastManager().clear());context.waitTicks(2);context.takeScreenshot("uncrafting-create-complete");
                context.runOnClient(client->client.setScreen(null));
                world.getServer().runCommand("clear @a");world.getServer().runCommand("give @a create:iron_sheet");context.waitTicks(5);
                context.runOnClient(client->ShopClient.openUncraft());
                context.waitFor(client->client.screen instanceof UncraftingScreen screen && preview(screen)!=null && preview(screen).total()>0);
                context.runOnClient(client->{
                    var data=preview((UncraftingScreen)client.screen);
                    check(data.recipe().equals("create:pressing/iron_ingot"),"Create pressing adapter must find the real machine recipe");
                    check(data.returns().stream().anyMatch(m->m.id().equals("minecraft:iron_ingot")),"Iron sheet must recover its ingot");
                });
                context.clickScreenButton("uncraft.balancedshop.uncraft");
                context.waitFor(client->preview((UncraftingScreen)client.screen).message().equals("done"));
                world.getServer().runOnServer(server->{
                    try {
                        Field recipes=dev.eymen.balancedshop.UncraftingService.class.getDeclaredField("recipes");recipes.setAccessible(true);
                        var reverse=(dev.eymen.balancedshop.UncraftingCatalogue)recipes.get(null);
                        check(reverse.recipe("create:compacting/andesite_from_flint")==null,"Fluid-consuming recipes must not be reversed");
                    }catch(ReflectiveOperationException e){throw new AssertionError(e);}
                });
            }
            context.runOnClient(client->client.setScreen(null));
            world.getServer().runCommand("setblock 8 -59 8 balancedshop:uncrafting_table");
            world.getServer().runOnServer(server->{
                var player=server.getPlayerList().getPlayers().getFirst();
                var pos=new net.minecraft.core.BlockPos(8,-59,8);
                server.overworld().getBlockState(pos).useWithoutItem(server.overworld(),player,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false));
            });
            context.waitForScreen(UncraftingScreen.class);context.waitTicks(3);
            context.runOnClient(client->client.setScreen(null));
            world.getServer().runCommand("clear @a");world.getServer().runCommand("give @a minecraft:diamond 200");context.waitTicks(5);
            world.getServer().runOnServer(server->{
                var named=new net.minecraft.world.item.ItemStack(Items.DIAMOND,5);
                named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Keep me"));
                var player=server.getPlayerList().getPlayers().getFirst();player.getInventory().setItem(8,named);player.inventoryMenu.broadcastChanges();
            });
            context.runOnClient(client->ShopClient.open());
            context.waitFor(client->client.screen instanceof ShopScreen screen&&page(screen)!=null);
            context.clickScreenButton("sell.balancedshop.open");
            context.waitFor(client->client.screen instanceof SellScreen&&field(client.screen,"data")!=null);
            long[] saleBalance={0};
            context.runOnClient(client->{
                SellPackets.Page data=field(client.screen,"data");saleBalance[0]=data.balance();
                ((List<Button>)field(client.screen,"slots")).get(27).onPress(null);
                check(((List<Button>)field(client.screen,"options")).stream().allMatch(b->b.visible&&b.active),"All five sell amounts should appear after clicking an item");
            });
            context.runOnClient(client->client.getToastManager().clear());
            context.waitTicks(5);context.runOnClient(client->client.getToastManager().clear());context.waitTicks(2);context.takeScreenshot("sell-amount-options");
            int sold=0;
            for(int i=1;i<=4;i++) {
                int option=i;sold+=new int[]{0,1,16,32,64}[i];long expected=saleBalance[0]+sold*25000L;
                context.runOnClient(client->((List<Button>)field(client.screen,"options")).get(option).onPress(null));
                context.waitFor(client->{SellPackets.Page data=field(client.screen,"data");return data.message().equals("sold")&&data.balance()==expected;});
                context.waitTicks(5);
            }
            context.runOnClient(client->((List<Button>)field(client.screen,"options")).get(0).onPress(null));
            context.waitFor(client->{SellPackets.Page data=field(client.screen,"data");return data.message().equals("sold")&&data.balance()==saleBalance[0]+200*25000L;});
            context.runOnClient(client->{
                check(client.player.getInventory().getItem(8).getCount()==5,"Bulk sell must preserve named diamonds");
                SellPackets.Page data=field(client.screen,"data");
                check(data.inventory().stream().noneMatch(s->s.id().equals("minecraft:diamond")&&s.plain()),"Sell all must consume every plain matching stack");
            });
            context.runOnClient(client->client.getToastManager().clear());context.waitTicks(2);context.takeScreenshot("sell-all-complete");context.runOnClient(client->client.setScreen(null));
            world.getServer().runCommand("clear @a");world.getServer().runCommand("shopadmin set Player0 1000000");
            context.runOnClient(client->ShopClient.open());
            context.waitFor(client->client.screen instanceof ShopScreen screen&&page(screen)!=null);
            for(String id:List.of("minecraft:spawner","minecraft:trial_spawner","minecraft:cow_spawn_egg","minecraft:blaze_spawn_egg")) {
                context.runOnClient(client->((EditBox)field(client.screen,"search")).setValue(id));
                context.waitFor(client->page((ShopScreen)client.screen).entries().stream().anyMatch(e->e.id().equals(id)));
                long[] expected={0};
                context.runOnClient(client->{
                    ShopScreen screen=(ShopScreen)client.screen;var entries=page(screen).entries();
                    int index=0;while(!entries.get(index).id().equals(id)) index++;
                    check(entries.get(index).buyable(),"Spawner and spawn eggs must be purchasable: "+id);
                    expected[0]=page(screen).balance()-entries.get(index).buy();
                    ((List<Button>)field(screen,"slots")).get(index).onPress(null);
                });
                context.waitTicks(5);context.clickScreenButton("shop.balancedshop.buy");
                context.waitFor(client->page((ShopScreen)client.screen).message().equals("bought")&&page((ShopScreen)client.screen).balance()==expected[0]);
                context.runOnClient(client->check(client.player.getInventory().contains(new net.minecraft.world.item.ItemStack(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id)))),"Purchased item must reach inventory: "+id));
            }
            context.runOnClient(client->client.getToastManager().clear());context.waitTicks(3);
            context.runOnClient(client->client.getToastManager().clear());context.waitTicks(2);context.takeScreenshot("spawner-spawn-egg-purchase");context.runOnClient(client->client.setScreen(null));
            world.getServer().runOnServer(server->{
                try {
                    var books=new BookCatalogue(server,new ShopConfig(),Path.of("build/verification/all-enchantment-prices.csv"));
                    var wallets=new WalletStore(Path.of("build/verification/book-test-wallets.json"),0);
                    var inventory=new net.minecraft.world.entity.player.Inventory(null,new net.minecraft.world.entity.EntityEquipment());
                    var player=java.util.UUID.randomUUID();wallets.set(player,Money.MAX/2);
                    int expected=server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).listElements().mapToInt(e->e.value().getMaxLevel()).sum();
                    check(books.all().size()==expected,"Every registered enchantment and every legal level must be listed");
                    check(books.entry("shopcompat_test:compatibility",2)!=null,"Mod/datapack enchantments must be discovered");
                    for(var entry:books.all()) {
                        inventory.clearContent();long before=wallets.balance(player);
                        check(BookPurchase.buy(books,wallets,inventory,player,entry.id(),entry.level(),1).equals("bought"),"Every book variant must be purchasable");
                        check(wallets.balance(player)==before-entry.buy(),"Book cost must debit exactly");
                        var enchantments=inventory.getItem(0).get(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS);
                        check(enchantments!=null&&enchantments.size()==1,"Book must contain exactly the selected enchantment");
                        var enchantment=enchantments.entrySet().iterator().next();
                        check(enchantment.getIntValue()==entry.level()&&enchantment.getKey().unwrapKey().orElseThrow().identifier().toString().equals(entry.id()),"Book level and id must be correct");
                    }
                    inventory.clearContent();wallets.set(player,0);
                    check(BookPurchase.buy(books,wallets,inventory,player,"minecraft:mending",1,1).equals("funds"),"Insufficient funds must not create books");
                    wallets.set(player,1000000000);
                    check(BookPurchase.buy(books,wallets,inventory,player,"minecraft:unbreaking",4,1).equals("invalid"),"Illegal enchantment level must be rejected");
                    check(BookPurchase.buy(books,wallets,inventory,player,"missing:unknown",1,1).equals("invalid"),"Unknown enchantment must be rejected");
                    check(BookPurchase.buy(books,wallets,inventory,player,"minecraft:mending",1,2).equals("invalid"),"Illegal purchase amount must be rejected");
                    for(int i=0;i<36;i++) inventory.setItem(i,new net.minecraft.world.item.ItemStack(Items.COBBLESTONE,64));
                    long before=wallets.balance(player);
                    check(BookPurchase.buy(books,wallets,inventory,player,"minecraft:mending",1,1).equals("space"),"Full inventory must reject book purchase");
                    check(wallets.balance(player)==before&&inventory.getItem(0).is(Items.COBBLESTONE),"Rejected purchase must preserve inventory and balance");
                    inventory.clearContent();
                    check(BookPurchase.buy(books,wallets,inventory,player,"minecraft:mending",1,64).equals("space"),"64 nonstacking books need more than 36 slots");
                    Path blocked=Path.of("build/verification/book-test-wallets.json.tmp");Files.createDirectory(blocked);
                    try {
                        try {BookPurchase.buy(books,wallets,inventory,player,"minecraft:mending",1,1);throw new AssertionError("Expected failed wallet save");}
                        catch(java.io.IOException expectedFailure){check(wallets.balance(player)==before&&inventory.getItem(0).isEmpty(),"Save failure must preserve wallet and inventory");}
                    }finally{Files.delete(blocked);}
                    var override=new ShopConfig();override.enchantmentPrices.put("minecraft:mending@1",12300L);
                    var changed=new BookCatalogue(server,override,Path.of("build/verification/override-book-prices.csv"));
                    check(changed.entry("minecraft:mending",1).buy()==12300,"Configured book price must override the multiplier");
                    inventory.clearContent();long bulkBefore=wallets.balance(player);
                    check(BookPurchase.buy(books,wallets,inventory,player,"minecraft:mending",1,16).equals("bought"),"16 books should fit 16 empty slots");
                    check(wallets.balance(player)==bulkBefore-books.entry("minecraft:mending",1).buy()*16,"Bulk book price must debit exactly");
                    for(int i=0;i<16;i++) check(inventory.getItem(i).is(Items.ENCHANTED_BOOK)&&inventory.getItem(i).getCount()==1,"Books must respect vanilla stack size");
                }catch(java.io.IOException e){throw new AssertionError(e);}
            });
            world.getServer().runCommand("clear @a");world.getServer().runCommand("shopadmin set Player0 1000000");
            context.runOnClient(client->ShopClient.open());
            context.waitFor(client->client.screen instanceof ShopScreen screen&&page(screen)!=null);
            context.clickScreenButton("books.balancedshop.open");
            context.waitFor(client->client.screen instanceof BookScreen&&field(client.screen,"data")!=null);
            context.runOnClient(client->check(((BookPackets.Page)field(client.screen,"data")).total()>100,"All enchantment levels should be visible"));
            context.runOnClient(client->client.getToastManager().clear());context.waitTicks(3);context.runOnClient(client->client.getToastManager().clear());context.waitTicks(2);context.takeScreenshot("all-enchantment-levels");
            for(String id:List.of("minecraft:mending","minecraft:unbreaking","shopcompat_test:compatibility")) {
                int level=id.equals("minecraft:unbreaking")?3:id.equals("shopcompat_test:compatibility")?2:1;
                context.runOnClient(client->((EditBox)field(client.screen,"search")).setValue(id));
                context.waitFor(client->{BookPackets.Page data=field(client.screen,"data");return data.entries().stream().anyMatch(e->e.id().equals(id)&&e.level()==level);});
                long[] expectedBalance={0};
                context.runOnClient(client->{
                    BookPackets.Page data=field(client.screen,"data");int index=0;while(!(data.entries().get(index).id().equals(id)&&data.entries().get(index).level()==level)) index++;
                    expectedBalance[0]=data.balance()-data.entries().get(index).buy();((List<Button>)field(client.screen,"rows")).get(index).onPress(null);
                });
                context.waitTicks(5);context.clickScreenButton("shop.balancedshop.buy");
                context.waitFor(client->{BookPackets.Page data=field(client.screen,"data");return data.message().equals("bought")&&data.balance()==expectedBalance[0];});
                context.runOnClient(client->{
                    boolean found=false;for(int i=0;i<36;i++){
                        var enchantments=client.player.getInventory().getItem(i).getOrDefault(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
                        if(enchantments.entrySet().stream().anyMatch(e->e.getIntValue()==level&&e.getKey().unwrapKey().orElseThrow().identifier().toString().equals(id))) found=true;
                    }
                    check(found,"Purchased book must sync its exact enchantment and level to client inventory");
                });
                if(id.equals("minecraft:unbreaking")){context.runOnClient(client->client.getToastManager().clear());context.waitTicks(2);context.takeScreenshot("unbreaking-iii-purchase");}
            }
            world.getServer().runCommand("shopadmin reload");context.waitTicks(5);
            context.runOnClient(client->check(((BookPackets.Page)field(client.screen,"data")).total()==2,"Book reload must preserve mod enchantment catalogue"));
            context.runOnClient(client->client.setScreen(null));
            UpgradeGameChecks.run(context,world);
            CoinGameChecks.run(context,world);
        }
    }
}
