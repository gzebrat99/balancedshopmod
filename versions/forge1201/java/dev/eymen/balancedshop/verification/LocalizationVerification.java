package dev.eymen.balancedshop.verification;

import dev.eymen.balancedshop.*;
import dev.eymen.balancedshop.localization.TurkishText;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.nio.file.*;
import java.util.*;

/** Uses actual catalogues under the default English server language. */
public final class LocalizationVerification {
    private static int checks;
    private static void check(boolean pass,String label){if(!pass)throw new AssertionError(label);checks++;}
    public static int run(MinecraftServer server)throws Exception{
        checks=0;Path dir=Files.createTempDirectory("shop-turkish-");var config=new ShopConfig();
        var catalogue=new Catalogue(server,config,dir.resolve("prices.csv"));var books=new BookCatalogue(server,config,dir.resolve("books.csv"));var spawners=new SpawnerCatalogue(server,config);
        check(TurkishText.name(new ItemStack(Items.DIAMOND)).getString().equals("Elmas"),"Turkish item name independent of global locale");
        check(net.minecraft.locale.Language.getInstance().getOrDefault("item.minecraft.diamond").equals("Diamond"),"Global Minecraft/server language is unchanged");
        for(String query:List.of("elmas","ELMAS","Elmas","minecraft:diamond","diamond"))check(catalogue.search(query,"").stream().anyMatch(e->e.id().equals("minecraft:diamond")),"Diamond query "+query);
        for(String query:List.of("balkabağı tohumu","bal kabağı tohumu","balkabagi tohumu","BAL KABAĞI TOHUMU"))check(catalogue.search(query,"").stream().anyMatch(e->e.id().equals("minecraft:pumpkin_seeds")),"Pumpkin query "+query);
        for(String query:List.of("elmas bloğu","elmas blogu","elmas blok"))check(catalogue.search(query,"").stream().anyMatch(e->e.id().equals("minecraft:diamond_block")),"Diamond block query "+query);
        check(catalogue.search("demir kazma","@gear").stream().anyMatch(e->e.id().equals("minecraft:iron_pickaxe")),"Turkish search composes with server categories");
        check(catalogue.search("bu eşya kesinlikle yok 98765","").isEmpty(),"Turkish search does not match unrelated items");
        for(String query:List.of("kırılmazlık","kirilmazlik","KIRILMAZLIK","unbreaking","Kırılmazlık 3","kırılmazlık III"))check(books.search(query).stream().anyMatch(e->e.id().equals("minecraft:unbreaking")&&e.level()==3),"Unbreaking query "+query);
        for(String query:List.of("onarım","onarim","tamir","mending"))check(books.search(query).stream().anyMatch(e->e.id().equals("minecraft:mending")),"Mending query "+query);
        check(BookCatalogue.name(books.entry("minecraft:unbreaking",3).book()).getString().equals("Kırılmazlık III"),"Book title includes Turkish enchantment and Roman level");
        for(String query:List.of("köylü","koylu"))check(spawners.search(query).stream().anyMatch(e->e.id().equals("minecraft:villager")),"Villager spawner query "+query);
        check(spawners.search("demir golem").stream().anyMatch(e->e.id().equals("minecraft:iron_golem")),"Iron golem spawner Turkish search");
        check(TurkishText.name(MobSpawnerItem.create("minecraft:iron_golem")).getString().equals("Demir Golem Üreteci"),"Nested entity and spawner translations");
        check(catalogue.search("mil","create").stream().anyMatch(e->e.id().equals("create:shaft")),"Common Create synonym");
        check(catalogue.search("andezit alaşım","create").stream().anyMatch(e->e.id().equals("create:andesite_alloy")),"Create Turkish material alias");
        var source=new ItemStack(Items.DIAMOND_PICKAXE);
        check(UpgradeEngine.targets(catalogue,books,source,true,"kirilmazlik").stream().anyMatch(t->t.key().equals("minecraft:unbreaking@3")),"Enchant upgrader accepts Turkish target query");
        check(UpgradeEngine.targets(catalogue,books,source,false,"elmas").stream().anyMatch(t->t.key().equals("minecraft:diamond")),"Item upgrader accepts Turkish target query");
        var named=new ItemStack(Items.DIAMOND);named.setHoverName(Component.literal("Oyuncunun özel adı"));var before=named.copy();
        check(TurkishText.name(named).getString().equals("Oyuncunun özel adı")&&ItemStack.matches(named,before),"Display localization preserves custom names and NBT");
        check(TurkishText.component(Component.translatable("shop.balancedshop.balance","12.50")).getString().equals("Bakiye: 12.50 akçe"),"Parameterized Turkish UI text");
        check(TurkishText.fold("IŞIK İĞNE ŞAFT").equals("isik igne saft"),"Dotted/dotless uppercase Turkish letters normalize correctly");
        return checks;
    }
}
