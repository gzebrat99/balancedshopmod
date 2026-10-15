package dev.eymen.balancedshop.verification;

import dev.eymen.balancedshop.*;
import dev.eymen.balancedshop.bridge.ClientBridge;
import dev.eymen.balancedshop.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.core.registries.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.npc.villager.*;
import net.minecraft.network.chat.Component;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiConsumer;

/** Runs real native clients/integrated servers and real loader networking, no mocks. */
@SuppressWarnings("unchecked")
public final class ClientVerification {
    private static int phase,timer;private static long balance,cost,fee,token;private static int serial;
    private static <T>T field(Object o,String name){try{var f=o.getClass().getDeclaredField(name);f.setAccessible(true);return(T)f.get(o);}catch(Exception e){throw new AssertionError(e);}}
    private static void step(){phase++;timer=0;BalancedShop.LOG.info("PORT VERIFICATION CLIENT PHASE {}",phase);}
    private static int count(Minecraft c,Item item){int n=0;for(int i=0;i<36;i++)if(c.player.getInventory().getItem(i).is(item))n+=c.player.getInventory().getItem(i).getCount();return n;}
    private static void check(boolean b,String m){ServerVerification.check(b,m);}
    private static void server(Minecraft c,BiConsumer<net.minecraft.server.MinecraftServer,net.minecraft.server.level.ServerPlayer> action){var s=c.getSingleplayerServer();s.execute(()->action.accept(s,s.getPlayerList().getPlayer(c.player.getUUID())));}
    private static void reset(Minecraft c,Item item,int amount){server(c,(s,p)->{p.getInventory().clearContent();if(item!=null)p.getInventory().setItem(0,new ItemStack(item,amount));p.inventoryMenu.broadcastChanges();s.getCommands().performPrefixedCommand(s.createCommandSourceStack(),"shopadmin set "+p.getGameProfile().name()+" 1000000");});}
    public static void install(){try{Files.createDirectories(Path.of("verification"));for(String name:List.of("server.txt","client.txt","FAILED.txt"))Files.deleteIfExists(Path.of("verification",name));}catch(Exception e){throw new IllegalStateException(e);}ClientBridge.ClientTickEvents.END_CLIENT_TICK.register(ClientVerification::tick);BalancedShop.LOG.info("PORT VERIFICATION CLIENT INSTALLED");}
    private static void tick(Minecraft c){
        try{
            if(timer==0&&phase==0)BalancedShop.LOG.info("PORT VERIFICATION CLIENT FIRST TICK");
            if(++timer>3600)throw new AssertionError("Verification timeout phase "+phase+" screen="+c.screen);
            if(phase==0){if(timer<100||c.getOverlay()!=null)return;c.setScreen(new TitleScreen());
                var settings=new LevelSettings("Port verification",GameType.SURVIVAL,false,Difficulty.PEACEFUL,true,new GameRules(FeatureFlags.DEFAULT_FLAGS),WorldDataConfiguration.DEFAULT);
                step();c.createWorldOpenFlows().createFreshLevel("port-verify-"+System.currentTimeMillis(),settings,new WorldOptions(0,false,false),r->r.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),c.screen);return;
            }
            if(c.player==null||!ServerVerification.ready)return;
            switch(phase){
                case 1->{check(c.getResourceManager().getResource(Identifier.parse("balancedshop:items/gold_coin.json")).isPresent(),"Native coin assets loaded");check(!Component.translatable("item.balancedshop.gold_coin").getString().equals("item.balancedshop.gold_coin"),"Native translation resource loaded");reset(c,Items.DIAMOND,64);step();}
                case 2->{if(count(c,Items.DIAMOND)==64){ShopClient.open();step();}}
                case 3->{if(field(c.screen,"data")!=null){((EditBox)field(c.screen,"search")).setValue("minecraft:diamond");step();}}
                case 4->{ShopPackets.Page p=field(c.screen,"data");if(p!=null){int index=-1;for(int i=0;i<p.entries().size();i++)if(p.entries().get(i).id().equals("minecraft:diamond"))index=i;if(index>=0&&p.entries().stream().allMatch(e->e.id().contains("minecraft:diamond"))){balance=p.balance();cost=p.entries().get(index).buy();((List<Button>)field(c.screen,"slots")).get(index).onPress(null);((Button)field(c.screen,"buy")).onPress(null);step();}}}
                case 5->{ShopPackets.Page p=field(c.screen,"data");if(p!=null&&p.message().equals("bought")){check(p.balance()==balance-cost,"GUI shop exact debit");check(count(c,Items.DIAMOND)==65,"Network shop purchase");ShopClient.openSell();step();}}
                case 6->{SellPackets.Page p=field(c.screen,"data");if(p!=null){balance=p.balance();ClientBridge.ClientPlayNetworking.send(new SellPackets.Action(p.serial(),"minecraft:diamond",16));step();}}
                case 7->{SellPackets.Page p=field(c.screen,"data");if(p!=null&&p.message().equals("sold")){check(p.balance()==balance+400000,"Network bulk sell exact payout");check(count(c,Items.DIAMOND)==49,"Network bulk sell item count");ShopClient.openBooks();step();}}
                case 8->{if(field(c.screen,"data")!=null){((EditBox)field(c.screen,"search")).setValue("mending");step();}}
                case 9->{BookPackets.Page p=field(c.screen,"data");if(p!=null&&p.entries().size()==1&&p.entries().getFirst().id().equals("minecraft:mending")){((List<Button>)field(c.screen,"rows")).getFirst().onPress(null);((Button)field(c.screen,"buy")).onPress(null);step();}}
                case 10->{BookPackets.Page p=field(c.screen,"data");if(p!=null&&p.message().equals("bought")){check(count(c,Items.ENCHANTED_BOOK)==1,"Native registry-aware book packet");reset(c,Items.OAK_PLANKS,4);c.setScreen(null);step();}}
                case 11->{if(count(c,Items.OAK_PLANKS)==4){ShopClient.openUncraft();step();}}
                case 12->{if(field(c.screen,"data")!=null&&((Button)field(c.screen,"uncraft")).active){((Button)field(c.screen,"uncraft")).onPress(null);step();}}
                case 13->{UncraftingPackets.Preview p=field(c.screen,"data");if(p!=null&&p.message().equals("done")){check(count(c,Items.OAK_LOG)==1,"Native uncraft GUI");reset(c,Items.DIAMOND,16);c.setScreen(null);step();}}
                case 14->{if(count(c,Items.DIAMOND)==16){ShopClient.openUpgrade();step();}}
                case 15->{if(field(c.screen,"data")!=null){((EditBox)field(c.screen,"search")).setValue("minecraft:netherite_ingot");step();}}
                case 16->{UpgradePackets.Page p=field(c.screen,"data");if(p!=null&&p.targets().size()==1&&p.targets().getFirst().key().equals("minecraft:netherite_ingot")){((List<Button>)field(c.screen,"rows")).getFirst().onPress(null);step();}}
                case 17->{UpgradePackets.Page p=field(c.screen,"data");if(p!=null&&p.chance()>0&&!p.selected().isEmpty()&&((Button)field(c.screen,"roll")).active){check(p.chance()<=.9,"Network chance cap");balance=p.balance();fee=p.fee();((Button)field(c.screen,"roll")).onPress(null);step();}}
                case 18->{UpgradePackets.Page p=field(c.screen,"data");if(p!=null&&(p.message().equals("success")||p.message().equals("failed"))){check(p.balance()==balance-fee,"Network upgrade exact fee");check(count(c,Items.DIAMOND)==15,"Native upgrade consumes selected stake");reset(c,null,0);c.setScreen(null);step();}}
                case 19->{if(c.player.getInventory().getItem(0).isEmpty()){ShopClient.openCoins(null);step();}}
                case 20->{CoinPackets.Page p=field(c.screen,"data");if(p!=null){balance=p.balance();token=p.token();serial=p.serial();((List<Button>)field(c.screen,"rows")).get(2).onPress(null);((List<Button>)field(c.screen,"presets")).get(3).onPress(null);((Button)field(c.screen,"withdraw")).onPress(null);step();}}
                case 21->{CoinPackets.Page p=field(c.screen,"data");if(p!=null&&p.message().equals("withdrawn")&&timer>8){check(p.physical()==640000&&p.balance()==balance-640000,"Real coin withdrawal");ClientBridge.ClientPlayNetworking.send(new CoinPackets.Action(serial,token,0,2,64));step();}}
                case 22->{CoinPackets.Page p=field(c.screen,"data");if(p!=null&&p.message().equals("changed")){check(p.physical()==640000,"Replay cannot mint currency");server(c,(s,v)->s.getCommands().performPrefixedCommand(s.createCommandSourceStack(),"execute at "+v.getGameProfile().name()+" run summon minecraft:villager ~2 ~ ~ {NoAI:1b,VillagerData:{profession:\"minecraft:weaponsmith\",level:1,type:\"minecraft:plains\"}}"));c.setScreen(null);step();}}
                case 23->{var list=c.level.getEntitiesOfClass(Villager.class,c.player.getBoundingBox().inflate(6));for(var v:list)if(v.getVillagerData().profession().is(VillagerProfession.WEAPONSMITH)){c.gameMode.interact(c.player,v,InteractionHand.MAIN_HAND);step();break;}}
                case 24->{if(c.screen instanceof CreateTraderScreen){TraderPackets.Page p=field(c.screen,"data");if(p!=null&&!p.entries().isEmpty()){check(p.entries().stream().allMatch(e->e.id().startsWith("create:")),"Native villager Create-only GUI");cost=p.entries().getFirst().buy()*16;((List<Button>)field(c.screen,"rows")).getFirst().onPress(null);((List<Button>)field(c.screen,"presets")).get(1).onPress(null);((Button)field(c.screen,"buy")).onPress(null);step();}}}
                case 25->{TraderPackets.Page p=field(c.screen,"data");if(p!=null&&p.message().equals("bought")){check(p.physical()==640000-cost,"Native trader exact change");check(count(c,BuiltInRegistries.ITEM.getValue(Identifier.parse("create:shaft")))==16,"Native trader item delivery");ShopClient.openCoins(null);step();}}
                case 26->{CoinPackets.Page p=field(c.screen,"data");if(p!=null){check(p.balance()==balance-640000,"Villager does not debit bank");((Button)field(c.screen,"all")).onPress(null);step();}}
                case 27->{CoinPackets.Page p=field(c.screen,"data");if(p!=null&&p.message().equals("deposited")){check(p.physical()==0&&p.balance()==balance-cost,"Native deposit all after trader");var tooltip=new ItemStack(Items.DIAMOND).getTooltipLines(Item.TooltipContext.of(c.level),c.player,TooltipFlag.NORMAL);check(tooltip.stream().anyMatch(l->l.getString().contains("250.00")),"Native sell tooltip synced");Files.createDirectories(Path.of("verification"));Files.writeString(Path.of("verification/client.txt"),"PASS: all 7 GUI flows, real registry codecs, native trader interaction, replay guard, persistence invoker and tooltip. Total assertions="+ServerVerification.assertions+"\n");BalancedShop.LOG.info("PORT VERIFICATION CLIENT PASS: {} total assertions",ServerVerification.assertions);step();c.stop();}}
            }
        }catch(Throwable t){try{Files.createDirectories(Path.of("verification"));Files.writeString(Path.of("verification/FAILED.txt"),"phase="+phase+" "+t);}catch(Exception ignored){}BalancedShop.LOG.error("PORT VERIFICATION FAILED phase="+phase,t);c.stop();}
    }
}
