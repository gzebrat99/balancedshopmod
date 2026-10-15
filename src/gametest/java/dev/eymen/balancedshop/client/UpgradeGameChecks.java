package dev.eymen.balancedshop.client;
import dev.eymen.balancedshop.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.client.gui.components.*;
import java.nio.file.Path;
import java.util.*;
@SuppressWarnings({"UnstableApiUsage","unchecked"})
final class UpgradeGameChecks {
    private static <T> T field(Object object,String name){try{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return (T)f.get(object);}catch(Exception e){throw new AssertionError(e);}}
    private static void check(boolean ok,String message){if(!ok) throw new AssertionError(message);}
    static void run(ClientGameTestContext context,TestSingleplayerContext world){
        world.getServer().runOnServer(server->{try{
            var cfg=new ShopConfig();var catalogue=new Catalogue(server,cfg,Path.of("build/verification/upgrade-prices.csv"));
            var books=new BookCatalogue(server,cfg,Path.of("build/verification/upgrade-books.csv"));
            catalogue.attachBooks(books);
            var sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(12);sword.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("My sword"));
            var targets=UpgradeEngine.targets(catalogue,books,sword,true,"");
            var sharp=targets.stream().filter(t->t.key().equals("minecraft:sharpness@5")).findFirst().orElseThrow();
            check(sharp.value()==UpgradeEngine.value(catalogue,books,sharp.result()),"Target value includes actual resulting tool durability and enchantments");
            check(sharp.result().getDamageValue()==12&&sharp.result().getHoverName().getString().equals("My sword"),"Enchantment upgrade preserves damage/name");
            var sharpHolder=server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,net.minecraft.resources.Identifier.parse("minecraft:sharpness")));
            check(EnchantmentHelper.getEnchantmentsForCrafting(sharp.result()).getLevel(sharpHolder)==5,"Upgrade applies the exact enchantment level");
            check(targets.stream().noneMatch(t->t.key().startsWith("minecraft:efficiency@")),"Unsupported tool enchantments excluded");
            check(targets.stream().anyMatch(t->t.key().equals("shopcompat_test:compatibility@2")),"Mod/datapack enchantments supported");
            var already=sharp.result();var compatible=UpgradeEngine.targets(catalogue,books,already,true,"");
            check(compatible.stream().noneMatch(t->t.key().startsWith("minecraft:smite@")||t.key().startsWith("minecraft:sharpness@")),"Conflicting and lower/equal enchantments excluded");
            check(compatible.stream().anyMatch(t->t.key().equals("minecraft:mending@1")),"Mending can coexist with Sharpness");
            var sellable=already.copy();sellable.remove(DataComponents.CUSTOM_NAME);long toolSell=catalogue.sellValue(sellable);
            check(toolSell>catalogue.entry("minecraft:diamond_sword").sell(),"Enchantments add resale value to tools");
            var saleInventory=new net.minecraft.world.entity.player.Inventory(null,new net.minecraft.world.entity.EntityEquipment());saleInventory.setItem(0,sellable);
            var saleWallet=new WalletStore(Path.of("build/verification/tool-sale-wallets.json"),0);UUID saleId=UUID.randomUUID();
            check(TradeEngine.sell(catalogue,saleWallet,saleInventory,saleId,"minecraft:diamond_sword",1).equals("sold")&&saleWallet.balance(saleId)==toolSell,"Enchanted damaged tools sell at their exact calculated value");
            var bookTarget=UpgradeEngine.targets(catalogue,books,new ItemStack(Items.BOOK),true,"mending").getFirst();
            check(bookTarget.result().is(Items.ENCHANTED_BOOK)&&bookTarget.result().get(DataComponents.STORED_ENCHANTMENTS).size()==1,"Plain books turn into real enchanted books");
            check(UpgradeEngine.targets(catalogue,books,new ItemStack(Items.DIRT),false,"create:shaft").stream().anyMatch(t->t.key().equals("create:shaft")),"Create upgrade targets dynamically discovered");
            var inv=new net.minecraft.world.entity.player.Inventory(null,new net.minecraft.world.entity.EntityEquipment());inv.setItem(0,sword.copy());
            var wallet=new WalletStore(Path.of("build/verification/upgrade-wallets.json"),0);UUID id=UUID.randomUUID();wallet.set(id,100000000);
            var quote=UpgradeEngine.quote(sword,sharp,UpgradeEngine.value(catalogue,books,sword),true);long before=wallet.balance(id);
            check(UpgradeEngine.execute(quote,wallet,inv,id,0,true,()->0).equals("success"),"Deterministic enchantment success");
            check(ItemStack.isSameItemSameComponents(inv.getItem(0),sharp.result())&&wallet.balance(id)==before-quote.fee(),"Actual enchanted item delivered with exact fee");
        }catch(Exception e){throw new AssertionError(e);}});
        world.getServer().runCommand("clear @a");world.getServer().runCommand("give @a minecraft:diamond 16");world.getServer().runCommand("shopadmin set Player0 1000000");
        context.waitTicks(5);context.runOnClient(client->ShopClient.openUpgrade());
        context.waitFor(client->client.screen instanceof UpgradeScreen&&field(client.screen,"data")!=null);
        context.runOnClient(client->((EditBox)field(client.screen,"search")).setValue("minecraft:netherite_ingot"));
        context.waitFor(client->{UpgradePackets.Page p=field(client.screen,"data");return p.targets().stream().anyMatch(t->t.key().equals("minecraft:netherite_ingot"));});
        context.runOnClient(client->((List<Button>)field(client.screen,"rows")).getFirst().onPress(null));
        context.waitFor(client->{UpgradePackets.Page p=field(client.screen,"data");return p.chance()>0&&!p.selected().isEmpty();});
        long[] before={0},fee={0},token={0};int[] serial={0};context.runOnClient(client->{UpgradePackets.Page p=field(client.screen,"data");before[0]=p.balance();fee[0]=p.fee();token[0]=p.token();serial[0]=p.serial();check(p.destroy(),"Failure must destroy source as requested");});
        context.runOnClient(client->client.getToastManager().clear());context.waitTicks(3);context.takeScreenshot("lucky-upgrade-preview");
        context.clickScreenButton("upgrade.balancedshop.roll");
        context.waitFor(client->{UpgradePackets.Page p=field(client.screen,"data");return p.message().equals("success")||p.message().equals("failed");});
        context.runOnClient(client->{UpgradePackets.Page p=field(client.screen,"data");check(p.balance()==before[0]-fee[0],"Actual network roll charges exact fee");check(p.inventory().get(0).getCount()==15,"Actual network roll consumes exactly one diamond");});
        context.waitTicks(38);context.takeScreenshot("lucky-upgrade-result");
        context.runOnClient(client->net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new UpgradePackets.Roll(serial[0],token[0])));
        context.waitFor(client->{UpgradePackets.Page p=field(client.screen,"data");return p.message().equals("changed");});
        context.runOnClient(client->{UpgradePackets.Page p=field(client.screen,"data");check(p.balance()==before[0]-fee[0]&&p.inventory().get(0).getCount()==15,"Replay packet must not charge again or consume another item");});
        bulkChecks(context,world);
        world.getServer().runCommand("clear @a");world.getServer().runCommand("give @a minecraft:diamond_sword");context.waitTicks(5);
        context.runOnClient(client->ShopClient.openUpgrade());context.waitFor(client->field(client.screen,"data")!=null);
        context.runOnClient(client->((Button)field(client.screen,"mode")).onPress(null));
        context.waitFor(client->{UpgradePackets.Page p=field(client.screen,"data");return p.targets().stream().anyMatch(t->t.key().contains("@"));});
        context.runOnClient(client->((EditBox)field(client.screen,"search")).setValue("mending"));
        context.waitFor(client->{UpgradePackets.Page p=field(client.screen,"data");return p.targets().stream().anyMatch(t->t.key().equals("minecraft:mending@1"));});
        context.runOnClient(client->((List<Button>)field(client.screen,"rows")).getFirst().onPress(null));
        context.waitFor(client->{UpgradePackets.Page p=field(client.screen,"data");return p.selected().equals("minecraft:mending@1")&&p.chance()>0;});
        context.takeScreenshot("lucky-enchant-preview");
        context.getInput().resizeWindow(1280,800);context.runOnClient(client->{client.options.guiScale().set(2);client.resizeDisplay();});context.waitTicks(5);context.takeScreenshot("lucky-enchant-large");
        context.runOnClient(client->check(client.screen.height>=300,"Large GUI must exercise the chance wheel layout"));
        context.runOnClient(client->{
            var lines=new ItemStack(Items.DIAMOND,16).getTooltipLines(Item.TooltipContext.of(client.level),client.player,TooltipFlag.NORMAL);
            check(lines.stream().anyMatch(c->c.getString().contains("250.00")),"Native inventory tooltip has per-item sell value");
            check(lines.stream().anyMatch(c->c.getString().contains(Money.format(400000))),"Native inventory tooltip has stack sell value");
            var named=new ItemStack(Items.DIAMOND);named.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Protected"));
            var protectedLines=named.getTooltipLines(Item.TooltipContext.of(client.level),client.player,TooltipFlag.NORMAL);
            check(protectedLines.stream().anyMatch(c->c.getString().contains("Not sellable")),"Named items do not show a false sale payout");
            var tool=new ItemStack(Items.DIAMOND_SWORD);tool.setDamageValue(tool.getMaxDamage()/2);
            var toolLines=tool.getTooltipLines(Item.TooltipContext.of(client.level),client.player,TooltipFlag.NORMAL);
            check(toolLines.stream().noneMatch(c->c.getString().contains("Not sellable"))&&toolLines.stream().anyMatch(c->c.getString().contains("Sell value:")),"Damaged tool has a real native sell tooltip");
        });
        context.runOnClient(client->client.setScreen(null));
        world.getServer().runCommand("clear @a");world.getServer().runCommand("give @a minecraft:diamond 16");context.waitTicks(5);
        context.runOnClient(client->{client.getToastManager().clear();client.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player));});
        double[] cursor=context.computeOnClient(client->{var menu=client.player.inventoryMenu;var hotbar=menu.slots.get(36);int gx=(client.screen.width-176)/2+hotbar.x+8,gy=(client.screen.height-166)/2+hotbar.y+8;return new double[]{gx*client.getWindow().getGuiScale(),gy*client.getWindow().getGuiScale()};});
        context.getInput().setCursorPos(cursor[0],cursor[1]);context.waitTicks(3);context.takeScreenshot("inventory-sell-tooltip");
        context.runOnClient(client->client.setScreen(null));
        toolSaleChecks(context,world);
    }
    private static void toolSaleChecks(ClientGameTestContext context,TestSingleplayerContext world){
        world.getServer().runCommand("clear @a");long[] expected={0},plainValue={0};
        world.getServer().runOnServer(server->{
            var registry=server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
            var tool=new ItemStack(Items.DIAMOND_SWORD);tool.setDamageValue(tool.getMaxDamage()/2);
            var enchantments=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            for(String id:List.of("mending","unbreaking")) enchantments.set(registry.getOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT,net.minecraft.resources.Identifier.withDefaultNamespace(id))),id.equals("mending")?1:3);
            EnchantmentHelper.setEnchantments(tool,enchantments.toImmutable());
            expected[0]=(long)Math.floor((400400L+4000000L+1600000L)*.125*(1.0-(double)tool.getDamageValue()/tool.getMaxDamage()));
            var plain=new ItemStack(Items.DIAMOND_SWORD);plain.setDamageValue(plain.getMaxDamage()/2);
            plainValue[0]=(long)Math.floor(50050*(1.0-(double)plain.getDamageValue()/plain.getMaxDamage()));
            var named=plain.copy();named.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Keep this sword"));
            var player=server.getPlayerList().getPlayers().getFirst();player.getInventory().setItem(0,tool);player.getInventory().setItem(1,plain);player.getInventory().setItem(2,named);player.inventoryMenu.broadcastChanges();
        });context.waitTicks(5);context.runOnClient(client->ShopClient.openSell());
        context.waitFor(client->{SellPackets.Page p=field(client.screen,"data");return p!=null&&p.inventory().get(0).price()==expected[0];});
        long[] before={0};context.runOnClient(client->{
            SellPackets.Page p=field(client.screen,"data");before[0]=p.balance();var slot=p.inventory().get(0);
            check(slot.plain()&&slot.sellable()&&slot.stack().getDamageValue()>0&&slot.stack().get(DataComponents.ENCHANTMENTS).size()==2,"Sell GUI receives actual damaged enchanted tool components and price");
            var lines=slot.stack().getTooltipLines(Item.TooltipContext.of(client.level),client.player,TooltipFlag.NORMAL);
            check(lines.stream().anyMatch(c->c.getString().contains(Money.format(expected[0]))),"Tooltip and server sale valuation agree for enchanted tools");
            ((List<Button>)field(client.screen,"slots")).get(27).onPress(null);
        });context.getInput().setCursorPos(0,0);context.takeScreenshot("enchanted-tool-sale-value");
        context.runOnClient(client->((List<Button>)field(client.screen,"options")).get(1).onPress(null));
        context.waitFor(client->{SellPackets.Page p=field(client.screen,"data");return p.message().equals("sold")&&p.balance()==before[0]+expected[0];});
        context.waitTicks(5);context.runOnClient(client->((List<Button>)field(client.screen,"options")).get(0).onPress(null));
        context.waitFor(client->{SellPackets.Page p=field(client.screen,"data");return p.message().equals("sold")&&p.balance()==before[0]+expected[0]+plainValue[0];});
        context.runOnClient(client->{SellPackets.Page p=field(client.screen,"data");check(p.inventory().get(2).stack().has(DataComponents.CUSTOM_NAME)&&p.inventory().get(2).count()==1,"Bulk sale preserves the named tool");client.setScreen(null);});
    }
    private static void bulkChecks(ClientGameTestContext context,TestSingleplayerContext world){
        world.getServer().runCommand("clear @a");world.getServer().runCommand("give @a minecraft:crafting_table 256");context.waitTicks(5);
        context.runOnClient(client->ShopClient.openUpgrade());context.waitFor(client->{UpgradePackets.Page p=field(client.screen,"data");return p!=null&&p.available()==256;});
        context.runOnClient(client->((EditBox)field(client.screen,"search")).setValue("minecraft:diamond"));
        context.waitFor(client->{UpgradePackets.Page p=field(client.screen,"data");return p.targets().stream().anyMatch(t->t.key().equals("minecraft:diamond"));});
        context.runOnClient(client->{UpgradePackets.Page p=field(client.screen,"data");int index=0;while(!p.targets().get(index).key().equals("minecraft:diamond")) index++;((List<Button>)field(client.screen,"rows")).get(index).onPress(null);});
        context.waitFor(client->{UpgradePackets.Page p=field(client.screen,"data");return p.selected().equals("minecraft:diamond")&&p.chance()>0;});
        for(int i=0;i<4;i++){
            int button=i,amount=new int[]{16,32,64,256}[i];
            context.runOnClient(client->((List<Button>)field(client.screen,"givePresets")).get(button).onPress(null));
            context.waitFor(client->{UpgradePackets.Page p=field(client.screen,"data");return p.give()==amount&&p.selected().equals("minecraft:diamond");});
            context.runOnClient(client->{UpgradePackets.Page p=field(client.screen,"data");check(Math.abs(p.chance()-UpgradeEngine.chance(amount*400L,200000))<1e-9,"Input preset must recalculate exact chance");});
        }
        context.runOnClient(client->((Button)field(client.screen,"giveMinus")).onPress(null));
        context.waitFor(client->((UpgradePackets.Page)field(client.screen,"data")).give()==255);
        context.runOnClient(client->((Button)field(client.screen,"givePlus")).onPress(null));
        context.waitFor(client->((UpgradePackets.Page)field(client.screen,"data")).give()==256);
        context.runOnClient(client->((List<Button>)field(client.screen,"receivePresets")).get(1).onPress(null));
        context.waitFor(client->{UpgradePackets.Page p=field(client.screen,"data");return p.receive()==32&&Math.abs(p.chance()-.0104)<1e-9;});
        context.runOnClient(client->((Button)field(client.screen,"receiveMinus")).onPress(null));
        context.waitFor(client->((UpgradePackets.Page)field(client.screen,"data")).receive()==31);
        context.runOnClient(client->((Button)field(client.screen,"receivePlus")).onPress(null));
        context.waitFor(client->((UpgradePackets.Page)field(client.screen,"data")).receive()==32);
        // Return to a single diamond through a new screen while keeping the 256-item preset.
        context.runOnClient(client->ShopClient.openUpgrade());context.waitFor(client->field(client.screen,"data")!=null);
        context.runOnClient(client->((EditBox)field(client.screen,"search")).setValue("minecraft:diamond"));
        context.waitFor(client->{UpgradePackets.Page p=field(client.screen,"data");return p.targets().stream().anyMatch(t->t.key().equals("minecraft:diamond"));});
        context.runOnClient(client->{UpgradePackets.Page p=field(client.screen,"data");int index=0;while(!p.targets().get(index).key().equals("minecraft:diamond")) index++;((List<Button>)field(client.screen,"rows")).get(index).onPress(null);});
        context.waitFor(client->!((UpgradePackets.Page)field(client.screen,"data")).selected().isEmpty());
        context.runOnClient(client->((List<Button>)field(client.screen,"givePresets")).get(3).onPress(null));
        context.waitFor(client->{UpgradePackets.Page p=field(client.screen,"data");return p.give()==256&&p.receive()==1&&Math.abs(p.chance()-.3328)<1e-9;});
        context.runOnClient(client->client.getToastManager().clear());context.waitTicks(2);context.takeScreenshot("bulk-256-crafting-to-diamond");
        long[] before={0},fee={0};context.runOnClient(client->{UpgradePackets.Page p=field(client.screen,"data");before[0]=p.balance();fee[0]=p.fee();});
        context.clickScreenButton("upgrade.balancedshop.roll");
        context.waitFor(client->{UpgradePackets.Page p=field(client.screen,"data");return p.message().equals("success")||p.message().equals("failed");});
        context.runOnClient(client->{UpgradePackets.Page p=field(client.screen,"data");check(p.inventory().stream().noneMatch(s->s.is(Items.CRAFTING_TABLE)),"Whole 256-item stake consumed across four stacks");check(p.balance()==before[0]-fee[0],"Bulk network roll charges exact fee");int diamonds=p.inventory().stream().filter(s->s.is(Items.DIAMOND)).mapToInt(ItemStack::getCount).sum();check(diamonds==(p.message().equals("success")?1:0),"Bulk random outcome must deliver exactly the chosen reward");});
        context.waitTicks(38);
    }
}
