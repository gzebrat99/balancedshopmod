package dev.eymen.balancedshop.client;

import dev.eymen.balancedshop.*;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.InteractionHand;
import java.util.*;

@SuppressWarnings({"unchecked","UnstableApiUsage"})
final class CoinGameChecks {
    private static <T> T field(Object o,String name){try{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return (T)f.get(o);}catch(Exception e){throw new AssertionError(e);}}
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    static void run(ClientGameTestContext context,TestSingleplayerContext world){
        world.getServer().runCommand("clear @a");world.getServer().runCommand("shopadmin set Player0 1000000");
        context.waitTicks(5);context.runOnClient(c->ShopClient.openCoins(null));
        context.waitFor(c->c.screen instanceof CoinScreen&&field(c.screen,"data")!=null);
        long[] balance={0},token={0};int[] serial={0};
        context.runOnClient(c->{CoinPackets.Page p=field(c.screen,"data");balance[0]=p.balance();check(p.physical()==0,"No physical coins initially");((List<Button>)field(c.screen,"rows")).get(2).onPress(null);((List<Button>)field(c.screen,"presets")).get(3).onPress(null);token[0]=p.token();serial[0]=p.serial();});
        context.clickScreenButton("coins.balancedshop.withdraw");
        context.waitFor(c->{CoinPackets.Page p=field(c.screen,"data");return p.message().equals("withdrawn");});
        context.runOnClient(c->{CoinPackets.Page p=field(c.screen,"data");check(p.counts().get(2)==64&&p.physical()==640000,"64 gold coins reach inventory");check(p.balance()==balance[0]-640000,"Withdrawal debits face value exactly");
            check(c.player.getInventory().contains(new net.minecraft.world.item.ItemStack(CoinItems.entry(2).item())),"Client receives real coin item");
            var tooltip=new net.minecraft.world.item.ItemStack(CoinItems.entry(2).item()).getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(c.level),c.player,net.minecraft.world.item.TooltipFlag.NORMAL);
            check(tooltip.stream().anyMatch(l->l.getString().contains("Coin value: 100.00")),"Currency has face-value tooltip");
        });
        context.getInput().setCursorPos(2,2);context.waitTicks(2);context.takeScreenshot("coin-wallet-large");context.waitTicks(5);
        context.runOnClient(c->ClientPlayNetworking.send(new CoinPackets.Action(serial[0],token[0],0,2,64)));
        context.waitFor(c->{CoinPackets.Page p=field(c.screen,"data");return p.message().equals("changed");});
        context.runOnClient(c->{CoinPackets.Page p=field(c.screen,"data");check(p.physical()==640000&&p.balance()==balance[0]-640000,"Replayed withdrawal cannot mint coins");});
        context.waitTicks(5);context.clickScreenButton("coins.balancedshop.deposit");
        context.waitFor(c->{CoinPackets.Page p=field(c.screen,"data");return p.message().equals("deposited");});
        context.runOnClient(c->{CoinPackets.Page p=field(c.screen,"data");check(p.balance()==balance[0]&&p.physical()==0,"Deposit conserves money");});
        context.getInput().resizeWindow(854,480);context.runOnClient(c->{c.options.guiScale().set(2);c.resizeDisplay();});context.waitTicks(5);context.takeScreenshot("coin-wallet-small");
        context.runOnClient(c->c.setScreen(null));
        if(!FabricLoader.getInstance().isModLoaded("create"))return;
        world.getServer().runCommand("time set 2000");
        world.getServer().runCommand("execute at @p run setblock ~2 ~ ~ minecraft:grindstone");
        world.getServer().runCommand("execute at @p run summon minecraft:villager ~3 ~ ~ {PersistenceRequired:1b}");
        // This uses the native profession acquisition, not a hard-coded profession NBT.
        context.waitFor(c->{for(var e:c.level.entitiesForRendering())if(e instanceof Villager v&&v.getVillagerData().profession().is(VillagerProfession.WEAPONSMITH))return true;return false;});
        UUID[] villager={null};
        world.getServer().runOnServer(server->{for(var e:server.overworld().getAllEntities())if(e instanceof Villager v&&v.getVillagerData().profession().is(VillagerProfession.WEAPONSMITH)){
            check(v.getOffers().isEmpty(),"Weaponsmith must not expose old emerald or weapon trades");villager[0]=v.getUUID();
            check(v.getBrain().getMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.JOB_SITE).isPresent(),"Villager actually acquired grindstone job site");
        }check(villager[0]!=null,"Grindstone creates Create trader");});
        context.runOnClient(c->{var v=c.level.getEntitiesOfClass(Villager.class,c.player.getBoundingBox().inflate(6)).stream().filter(e->e.getUUID().equals(villager[0])).findFirst().orElseThrow();c.gameMode.interact(c.player,v,InteractionHand.MAIN_HAND);});
        context.waitFor(c->c.screen instanceof CreateTraderScreen&&field(c.screen,"data")!=null);
        context.runOnClient(c->{TraderPackets.Page p=field(c.screen,"data");check(p.physical()==0,"Wallet funds do not count as villager payment");check(p.total()>100,"Create catalogue discovered dynamically");check(p.entries().getFirst().id().equals("create:shaft"),"Shaft featured first");check(p.entries().stream().allMatch(e->e.id().startsWith("create:")),"Create-only entries");((List<Button>)field(c.screen,"rows")).getFirst().onPress(null);check(!((Button)field(c.screen,"buy")).active,"No purchase without physical coins");});
        context.clickScreenButton("coins.balancedshop.open");context.waitFor(c->c.screen instanceof CoinScreen&&field(c.screen,"data")!=null);
        context.runOnClient(c->{((List<Button>)field(c.screen,"rows")).get(2).onPress(null);((List<Button>)field(c.screen,"presets")).get(3).onPress(null);});
        context.clickScreenButton("coins.balancedshop.withdraw");context.waitFor(c->{CoinPackets.Page p=field(c.screen,"data");return p.message().equals("withdrawn");});
        context.clickScreenButton("coins.balancedshop.back");context.waitFor(c->c.screen instanceof CreateTraderScreen&&!(boolean)field(c.screen,"loading"));
        context.runOnClient(c->{((List<Button>)field(c.screen,"rows")).getFirst().onPress(null);((List<Button>)field(c.screen,"presets")).get(1).onPress(null);});
        long[] cost={0};context.runOnClient(c->{TraderPackets.Page p=field(c.screen,"data");cost[0]=p.entries().getFirst().buy()*16;});
        context.getInput().setCursorPos(2,2);context.waitTicks(2);context.takeScreenshot("create-trader-small");context.clickScreenButton("shop.balancedshop.buy");
        context.waitFor(c->{TraderPackets.Page p=field(c.screen,"data");return p.message().equals("bought");});
        context.runOnClient(c->{TraderPackets.Page p=field(c.screen,"data");check(p.physical()==640000-cost[0],"Create villager returns exact change");check(c.player.getInventory().contains(new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("create:shaft")))),"Create parts reach client inventory");});
        context.getInput().resizeWindow(1280,800);context.runOnClient(c->{c.options.guiScale().set(2);c.resizeDisplay();});context.getInput().setCursorPos(2,2);context.waitTicks(5);context.takeScreenshot("create-trader-large");
        context.runOnClient(c->((EditBox)field(c.screen,"search")).setValue("andesite_alloy"));context.waitFor(c->{TraderPackets.Page p=field(c.screen,"data");return p.entries().stream().anyMatch(e->e.id().equals("create:andesite_alloy"))&&p.entries().stream().allMatch(e->e.id().contains("andesite_alloy"));});
        context.runOnClient(c->{TraderPackets.Page p=field(c.screen,"data");serial[0]=p.serial();token[0]=p.token();});
        world.getServer().runOnServer(server->{var v=server.overworld().getEntity(villager[0]);v.teleportTo(v.getX()+20,v.getY(),v.getZ());});
        context.runOnClient(c->ClientPlayNetworking.send(new TraderPackets.Buy(serial[0],token[0],"create:andesite_alloy",1)));
        context.waitFor(c->{TraderPackets.Page p=field(c.screen,"data");return p.message().equals("trader_closed");});
        context.runOnClient(c->{TraderPackets.Page p=field(c.screen,"data");check(p.physical()==640000-cost[0],"Remote trade cannot consume coins or give items");check(!((Button)field(c.screen,"buy")).active,"Disconnected villager UI disables purchases");c.setScreen(null);});
    }
}
