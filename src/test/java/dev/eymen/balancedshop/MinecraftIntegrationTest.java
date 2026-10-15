package dev.eymen.balancedshop;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class MinecraftIntegrationTest {
    private static MultiPackResourceManager resources;
    @TempDir Path dir;
    private Catalogue catalogue;
    private WalletStore wallets;
    private Inventory inventory;
    private final UUID player=UUID.randomUUID();
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
        ModContent.initialize();
        Identifier customId=Identifier.parse("shop_test:machine");
        Registry.register(BuiltInRegistries.ITEM,customId,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,customId))));
        for(String name:List.of("shaft","andesite_alloy","command_block")){
            var id=Identifier.parse("create:"+name);Registry.register(BuiltInRegistries.ITEM,id,new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id))));
        }
        var pack=new VanillaPackResourcesBuilder().exposeNamespace("minecraft").pushJarResources()
            .build(new PackLocationInfo("vanilla",Component.literal("Vanilla"),PackSource.BUILT_IN,Optional.empty()));
        resources=new MultiPackResourceManager(PackType.SERVER_DATA,List.of(pack));
        TagLoader<Holder<Item>> loader=new TagLoader<>((id,required)->BuiltInRegistries.ITEM.get(id),"tags/item");
        Map<TagKey<Item>,List<Holder<Item>>> tags=loader.build(loader.load(resources)).entrySet().stream()
            .collect(Collectors.toMap(e->TagKey.create(Registries.ITEM,e.getKey()),Map.Entry::getValue));
        tags.forEach(((WritableRegistry<Item>)BuiltInRegistries.ITEM)::bindTag);
        BuiltInRegistries.ITEM.freeze();
    }
    @AfterAll static void close() { if(resources!=null) resources.close(); }
    @BeforeEach void setup() throws Exception {
        catalogue=new Catalogue(resources,new ShopConfig(),dir.resolve("prices.csv"));
        wallets=new WalletStore(dir.resolve("wallets.json"),100000000);
        inventory=new Inventory(null,new EntityEquipment());
    }
    private String trade(String id,int qty,boolean sell) throws Exception {
        return TradeEngine.trade(catalogue,wallets,inventory,player,id,qty,sell);
    }
    @Test void physicalCoinsConserveMoneyAcrossDenominations() throws Exception {
        long before=wallets.balance(player);
        for(int i=0;i<4;i++){assertEquals("withdrawn",CoinEngine.withdraw(wallets,inventory,player,i,32));assertEquals(32,CoinItems.count(inventory,i));}
        assertEquals(before,wallets.balance(player)+CoinItems.total(inventory));
        assertEquals("deposited",CoinEngine.deposit(wallets,inventory,player,1,16,false));assertEquals(16,CoinItems.count(inventory,1));
        assertEquals("deposited",CoinEngine.deposit(wallets,inventory,player,0,0,true));assertEquals(0,CoinItems.total(inventory));assertEquals(before,wallets.balance(player));
    }
    @Test void coinsCannotBeMintedByShopPriceOverridesOrNamedDeposits() throws Exception {
        ShopConfig cfg=new ShopConfig();cfg.allowRestrictedItems=true;cfg.prices.put("balancedshop:silver_coin",new ShopConfig.Override(1,true,true));
        var c=new Catalogue(resources,cfg,dir.resolve("override.csv"));var e=c.entry("balancedshop:silver_coin");assertFalse(e.buyable());assertFalse(e.sellable());assertEquals(100,e.buy());
        assertEquals("restricted",TradeEngine.trade(c,wallets,inventory,player,e.id(),64,false));
        var named=new ItemStack(CoinItems.entry(1).item(),64);named.set(DataComponents.CUSTOM_NAME,Component.literal("Fake cash"));inventory.setItem(0,named);
        long before=wallets.balance(player);assertEquals("items",CoinEngine.deposit(wallets,inventory,player,0,0,true));assertEquals(64,inventory.getItem(0).getCount());assertEquals(before,wallets.balance(player));
        assertTrue(UpgradeEngine.targets(c,null,new ItemStack(Items.DIRT),false,"_coin").isEmpty());
    }
    @Test void coinTransfersRejectSpaceFundsMissingCoinsAndInvalidAmounts() throws Exception {
        for(int i=0;i<36;i++)inventory.setItem(i,new ItemStack(Items.COBBLESTONE,64));long before=wallets.balance(player);
        assertEquals("space",CoinEngine.withdraw(wallets,inventory,player,1,64));assertEquals(before,wallets.balance(player));
        assertEquals("invalid",CoinEngine.withdraw(wallets,inventory,player,1,2305));assertEquals("invalid",CoinEngine.withdraw(wallets,inventory,player,4,1));
        assertEquals("invalid",CoinEngine.deposit(wallets,inventory,player,1,0,false));
        inventory.setItem(0,new ItemStack(CoinItems.entry(1).item(),16));assertEquals("items",CoinEngine.deposit(wallets,inventory,player,1,32,false));assertEquals(16,inventory.getItem(0).getCount());
        wallets.set(player,0);inventory.setItem(1,ItemStack.EMPTY);assertEquals("funds",CoinEngine.withdraw(wallets,inventory,player,1,1));
        wallets.set(player,Money.MAX);assertEquals("limit",CoinEngine.deposit(wallets,inventory,player,0,0,true));assertEquals(16,inventory.getItem(0).getCount());
    }
    @Test void failedCoinPersistenceNeverMutatesInventory() throws Exception {
        long before=wallets.balance(player);Files.createDirectory(dir.resolve("wallets.json.tmp"));
        assertThrows(java.io.IOException.class,()->CoinEngine.withdraw(wallets,inventory,player,1,32));assertEquals(0,CoinItems.total(inventory));assertEquals(before,wallets.balance(player));
        inventory.setItem(0,new ItemStack(CoinItems.entry(1).item(),32));assertThrows(java.io.IOException.class,()->CoinEngine.deposit(wallets,inventory,player,0,0,true));assertEquals(32,CoinItems.count(inventory,1));assertEquals(before,wallets.balance(player));
    }
    @Test void createVillagerOffersOnlyBuyableCreateItemsAndFeaturesShaftFirst(){
        var offers=CreateTraderService.offers(catalogue,"");assertFalse(offers.isEmpty());assertEquals("create:shaft",offers.getFirst().id());assertTrue(offers.stream().allMatch(e->e.id().startsWith("create:")&&e.buyable()));
        assertEquals(1,CreateTraderService.offers(catalogue,"andesite_alloy").size());assertTrue(CreateTraderService.offers(catalogue,"command_block").isEmpty());assertTrue(CreateTraderService.offers(catalogue,"minecraft:diamond").isEmpty());
    }
    @Test void createPaymentsUsePhysicalCashReturnExactChangeAndNeverUseWallet() throws Exception {
        assertEquals("funds",CoinEngine.buyCreate(catalogue,inventory,"create:shaft",16));
        inventory.setItem(0,new ItemStack(CoinItems.entry(3).item(),2));long physical=CoinItems.total(inventory),before=wallets.balance(player),cost=Money.total(catalogue.entry("create:shaft").buy(),16);
        assertEquals("bought",CoinEngine.buyCreate(catalogue,inventory,"create:shaft",16));assertEquals(physical-cost,CoinItems.total(inventory));assertEquals(before,wallets.balance(player));assertEquals(16,inventory.getItem(0).getCount());assertTrue(inventory.getItem(0).is(catalogue.item("create:shaft")));
        assertEquals("restricted",CoinEngine.buyCreate(catalogue,inventory,"minecraft:diamond",1));assertEquals("invalid",CoinEngine.buyCreate(catalogue,inventory,"create:shaft",65));assertEquals("restricted",CoinEngine.buyCreate(catalogue,inventory,"create:command_block",1));
    }
    @Test void createTradeRejectsInsufficientChangeCapacityWithoutConsumingCoins(){
        for(int i=0;i<36;i++)inventory.setItem(i,new ItemStack(Items.COBBLESTONE,64));inventory.setItem(0,new ItemStack(CoinItems.entry(3).item(),2));long before=CoinItems.total(inventory);
        assertEquals("space",CoinEngine.buyCreate(catalogue,inventory,"create:shaft",1));assertEquals(before,CoinItems.total(inventory));assertEquals(2,inventory.getItem(0).getCount());
    }
    @Test void upgradeSuccessReplacesOneItemAndFailureDestroysOneItem() throws Exception {
        ItemStack source=new ItemStack(Items.DIAMOND,16);inventory.setItem(0,source);
        var target=new UpgradeEngine.Target("minecraft:netherite_ingot",new ItemStack(Items.NETHERITE_INGOT),catalogue.entry("minecraft:netherite_ingot").buy());
        var quote=UpgradeEngine.quote(source,target,catalogue.entry("minecraft:diamond").buy(),true);long before=wallets.balance(player);
        assertEquals("success",UpgradeEngine.execute(quote,wallets,inventory,player,0,true,()->0));
        assertEquals(15,inventory.getItem(0).getCount());assertTrue(inventory.getItem(1).is(Items.NETHERITE_INGOT));assertEquals(before-quote.fee(),wallets.balance(player));
        assertEquals("changed",UpgradeEngine.execute(quote,wallets,inventory,player,0,true,()->0));
        var failed=UpgradeEngine.quote(inventory.getItem(0),target,quote.sourceValue(),true);
        assertEquals("failed",UpgradeEngine.execute(failed,wallets,inventory,player,0,true,()->1));
        assertEquals(14,inventory.getItem(0).getCount());assertEquals(1,inventory.getItem(1).getCount());assertEquals(before-quote.fee()-failed.fee(),wallets.balance(player));
    }
    @Test void upgradeRejectionsAndSaveFailurePreserveItemsAndBalance() throws Exception {
        var target=new UpgradeEngine.Target("minecraft:netherite_ingot",new ItemStack(Items.NETHERITE_INGOT),catalogue.entry("minecraft:netherite_ingot").buy());
        for(int i=0;i<36;i++) inventory.setItem(i,new ItemStack(Items.COBBLESTONE,64));inventory.setItem(0,new ItemStack(Items.DIAMOND,2));
        var quote=UpgradeEngine.quote(inventory.getItem(0),target,catalogue.entry("minecraft:diamond").buy(),true);long before=wallets.balance(player);
        assertEquals("space",UpgradeEngine.execute(quote,wallets,inventory,player,0,true,()->{fail("Must not roll before capacity check");return 0;}));
        assertEquals(2,inventory.getItem(0).getCount());assertEquals(before,wallets.balance(player));
        inventory.setItem(1,ItemStack.EMPTY);wallets.set(player,0);
        assertEquals("funds",UpgradeEngine.execute(quote,wallets,inventory,player,0,true,()->0));assertEquals(2,inventory.getItem(0).getCount());
        wallets.set(player,before);Files.createDirectory(dir.resolve("wallets.json.tmp"));
        assertThrows(java.io.IOException.class,()->UpgradeEngine.execute(quote,wallets,inventory,player,0,true,()->0));
        assertEquals(2,inventory.getItem(0).getCount());assertTrue(inventory.getItem(1).isEmpty());assertEquals(before,wallets.balance(player));
        Files.delete(dir.resolve("wallets.json.tmp"));
        assertEquals("failed",UpgradeEngine.execute(quote,wallets,inventory,player,0,false,()->1));assertEquals(2,inventory.getItem(0).getCount());
    }
    @Test void upgradePreservesContainersAndRecognizesModdedItems(){
        var plain=new ItemStack(Items.DIAMOND_SWORD);plain.setDamageValue(20);plain.set(DataComponents.CUSTOM_NAME,Component.literal("Sword"));
        assertFalse(UpgradeEngine.eligible(plain,false));assertTrue(UpgradeEngine.eligible(plain,true));
        var filled=new ItemStack(Items.SHULKER_BOX);filled.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND))));
        assertFalse(UpgradeEngine.eligible(filled,false));assertFalse(UpgradeEngine.eligible(filled,true));
        assertTrue(UpgradeEngine.targets(catalogue,null,new ItemStack(Items.DIRT),false,"shop_test:machine").stream().anyMatch(t->t.key().equals("shop_test:machine")));
        assertTrue(UpgradeEngine.targets(catalogue,null,new ItemStack(Items.DIRT),false,"command_block").isEmpty());
    }
    @Test void bulkUpgradeConsumesAcrossStacksAndPricesBothSides() throws Exception {
        for(int i=0;i<4;i++) inventory.setItem(i,new ItemStack(Items.CRAFTING_TABLE,64));
        inventory.setItem(4,new ItemStack(Items.CRAFTING_TABLE,17));
        var target=new UpgradeEngine.Target("minecraft:diamond",new ItemStack(Items.DIAMOND),catalogue.entry("minecraft:diamond").buy());
        long unit=catalogue.sellValue(inventory.getItem(0));
        var quote=UpgradeEngine.quote(inventory,0,target,unit,256,1,true);assertNotNull(quote);
        assertEquals(102400,quote.sourceValue());assertEquals(.3328,quote.chance(),1e-9);long before=wallets.balance(player);
        assertEquals("success",UpgradeEngine.execute(quote,wallets,inventory,player,0,true,()->0));
        assertEquals(1,inventory.getItem(0).getCount());assertTrue(inventory.getItem(0).is(Items.DIAMOND));
        for(int i=1;i<4;i++) assertTrue(inventory.getItem(i).isEmpty());assertEquals(17,inventory.getItem(4).getCount());
        assertEquals(before-quote.fee(),wallets.balance(player));
        inventory.clearContent();for(int i=0;i<4;i++) inventory.setItem(i,new ItemStack(Items.CRAFTING_TABLE,64));
        var failed=UpgradeEngine.quote(inventory,0,target,unit,256,16,true);assertNotNull(failed);
        assertEquals(quote.chance()/16,failed.chance(),1e-9);
        assertEquals("failed",UpgradeEngine.execute(failed,wallets,inventory,player,0,true,()->1));
        for(int i=0;i<36;i++) assertTrue(inventory.getItem(i).isEmpty());
    }
    @Test void bulkUpgradeDeliversWholeRewardAndRejectsStaleStacksAndInvalidCounts() throws Exception {
        for(int i=0;i<4;i++) inventory.setItem(i,new ItemStack(Items.CRAFTING_TABLE,64));
        var target=new UpgradeEngine.Target("minecraft:diamond",new ItemStack(Items.DIAMOND),catalogue.entry("minecraft:diamond").buy());long unit=catalogue.sellValue(inventory.getItem(0));
        for(int bad:new int[]{-1,0,2305,Integer.MAX_VALUE}){
            assertNull(UpgradeEngine.quote(inventory,0,target,unit,bad,1,true));assertNull(UpgradeEngine.quote(inventory,0,target,unit,1,bad,true));
        }
        assertNull(UpgradeEngine.quote(inventory,0,target,unit,257,1,true));
        var stale=UpgradeEngine.quote(inventory,0,target,unit,256,128,true);long before=wallets.balance(player);
        inventory.getItem(1).shrink(1);assertEquals("changed",UpgradeEngine.execute(stale,wallets,inventory,player,0,true,()->0));
        assertEquals(before,wallets.balance(player));assertEquals(64,inventory.getItem(0).getCount());
        inventory.getItem(1).grow(1);var fresh=UpgradeEngine.quote(inventory,0,target,unit,256,128,true);
        assertEquals("success",UpgradeEngine.execute(fresh,wallets,inventory,player,0,true,()->0));
        int diamonds=0;for(int i=0;i<36;i++) if(inventory.getItem(i).is(Items.DIAMOND)){assertTrue(inventory.getItem(i).getCount()<=64);diamonds+=inventory.getItem(i).getCount();}
        assertEquals(128,diamonds);
    }
    @Test void bulkNonstackableRewardCapacityAndSaveFailurePreserveAllStakes() throws Exception {
        for(int i=0;i<4;i++) inventory.setItem(i,new ItemStack(Items.CRAFTING_TABLE,64));
        var sword=new UpgradeEngine.Target("minecraft:diamond_sword",new ItemStack(Items.DIAMOND_SWORD),catalogue.entry("minecraft:diamond_sword").buy());
        long unit=catalogue.sellValue(inventory.getItem(0)),before=wallets.balance(player);
        var tooMany=UpgradeEngine.quote(inventory,0,sword,unit,256,64,true);
        assertEquals("space",UpgradeEngine.execute(tooMany,wallets,inventory,player,0,true,()->{fail("No roll on insufficient capacity");return 0;}));
        assertEquals(before,wallets.balance(player));for(int i=0;i<4;i++) assertEquals(64,inventory.getItem(i).getCount());
        var fit=UpgradeEngine.quote(inventory,0,sword,unit,256,16,true);Files.createDirectory(dir.resolve("wallets.json.tmp"));
        assertThrows(java.io.IOException.class,()->UpgradeEngine.execute(fit,wallets,inventory,player,0,true,()->0));
        assertEquals(before,wallets.balance(player));for(int i=0;i<4;i++) assertEquals(64,inventory.getItem(i).getCount());
        Files.delete(dir.resolve("wallets.json.tmp"));assertEquals("success",UpgradeEngine.execute(fit,wallets,inventory,player,0,true,()->0));
        int swords=0;for(int i=0;i<36;i++) if(inventory.getItem(i).is(Items.DIAMOND_SWORD)){assertEquals(1,inventory.getItem(i).getCount());swords++;}assertEquals(16,swords);
    }
    @Test void swordsAndPickaxesCanBeBoughtSoldAndUsedAsUpgradeStakes() throws Exception {
        int tools=0;
        for(var e:catalogue.all()) if(e.id().startsWith("minecraft:")&&(e.id().endsWith("_sword")||e.id().endsWith("_pickaxe"))){
            assertTrue(e.buyable()&&e.sellable(),e.id());inventory.clearContent();long before=wallets.balance(player);
            assertEquals("bought",trade(e.id(),1,false));var tool=inventory.getItem(0);
            assertTrue(UpgradeEngine.eligible(tool,false));assertTrue(catalogue.sellValue(tool)>0);
            tool.setDamageValue(tool.getMaxDamage()/2);long damaged=catalogue.sellValue(tool);
            assertTrue(damaged>0&&damaged<e.sell());assertEquals("sold",trade(e.id(),1,true));
            assertEquals(before-e.buy()+damaged,wallets.balance(player));tools++;
        }
        assertTrue(tools>=12);
    }
    @Test void allRegisteredItemsAndVanillaRecipesArePriced() throws Exception {
        assertEquals(BuiltInRegistries.ITEM.size()-1,catalogue.all().size());
        assertTrue(catalogue.recipeCount()>1000,"Vanilla recipes must be read");
        assertNotNull(catalogue.entry("shop_test:machine"));
        for(var entry:catalogue.all()) {
            assertTrue(entry.buy()>0 && entry.buy()<=Money.MAX_PRICE,entry.id());
            assertTrue(entry.sell()<=entry.buy()/2,entry.id());
        }
        assertEquals(catalogue.entry("minecraft:iron_ingot").buy()*9,catalogue.entry("minecraft:iron_block").buy());
        assertTrue(catalogue.entry("minecraft:oak_planks").buy()*4<=catalogue.entry("minecraft:oak_log").buy());
        assertTrue(catalogue.entry("minecraft:shulker_box").buy()>=catalogue.entry("minecraft:shulker_shell").buy()*2);
        assertTrue(catalogue.entry("minecraft:ender_chest").buy()>=catalogue.entry("minecraft:obsidian").buy()*8);
        assertTrue(catalogue.entry("minecraft:lapis_ore").buy()>catalogue.entry("minecraft:lapis_lazuli").sell()*36);
        assertTrue(catalogue.entry("minecraft:copper_ore").buy()>catalogue.entry("minecraft:raw_copper").sell()*20);
        assertTrue(catalogue.entry("minecraft:glowstone").buy()>=catalogue.entry("minecraft:glowstone_dust").buy()*4);
        assertTrue(catalogue.entry("minecraft:bookshelf").buy()>catalogue.entry("minecraft:book").sell()*3);
        Path report=Path.of("build/verification/vanilla-prices.csv");Files.createDirectories(report.getParent());
        Files.copy(dir.resolve("prices.csv"),report,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Catalogue coverage: "+catalogue.all().size()+" items, "+catalogue.recipeCount()+" recipes");
    }
    @Test void purchaseAndSaleDebitAndCreditExactly() throws Exception {
        long before=wallets.balance(player); var entry=catalogue.entry("minecraft:diamond");
        assertEquals("bought",trade(entry.id(),16,false));
        assertEquals(before-entry.buy()*16,wallets.balance(player));
        assertEquals(16,inventory.getItem(0).getCount());
        assertEquals("sold",trade(entry.id(),16,true));
        assertTrue(inventory.getItem(0).isEmpty());
        assertEquals(before-entry.buy()*16+entry.sell()*16,wallets.balance(player));
        assertEquals(wallets.balance(player),new WalletStore(dir.resolve("wallets.json"),0).balance(player));
    }
    @Test void fullInventoryAndInsufficientFundsLeaveEverythingUntouched() throws Exception {
        long before=wallets.balance(player);
        for(int i=0;i<36;i++) inventory.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        assertEquals("space",trade("minecraft:diamond",1,false));assertEquals(before,wallets.balance(player));
        inventory.clearContent();wallets.set(player,0);
        assertEquals("funds",trade("minecraft:diamond",1,false));assertTrue(inventory.getItem(0).isEmpty());
    }
    @Test void nonStackablesRespectCapacityAndPurchasesMergeStacks() throws Exception {
        inventory.setItem(4,new ItemStack(Items.DIAMOND,60));
        assertEquals("bought",trade("minecraft:diamond",16,false));
        assertEquals(64,inventory.getItem(4).getCount());assertEquals(12,inventory.getItem(0).getCount());
        inventory.clearContent();
        assertEquals("space",trade("minecraft:diamond_sword",64,false));
        assertEquals("bought",trade("minecraft:diamond_sword",16,false));
        for(int i=0;i<16;i++) assertEquals(1,inventory.getItem(i).getCount());
    }
    @Test void damagedToolsAreValuedAndNamedAndFilledItemsAreProtected() throws Exception {
        ItemStack named=new ItemStack(Items.DIAMOND,16);named.set(DataComponents.CUSTOM_NAME,Component.literal("Keep me"));
        inventory.setItem(0,named);
        assertEquals("items",trade("minecraft:diamond",1,true));assertEquals(16,inventory.getItem(0).getCount());
        ItemStack damaged=new ItemStack(Items.DIAMOND_SWORD);damaged.setDamageValue(1);inventory.setItem(1,damaged);
        long value=catalogue.sellValue(damaged),before=wallets.balance(player);
        assertTrue(value>0&&value<catalogue.entry("minecraft:diamond_sword").sell());
        assertEquals("sold",trade("minecraft:diamond_sword",1,true));assertEquals(before+value,wallets.balance(player));
        ItemStack box=new ItemStack(Items.SHULKER_BOX);box.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND,64))));
        inventory.setItem(2,box);assertEquals("items",trade("minecraft:shulker_box",1,true));
    }
    @Test void invalidRestrictedAndMissingItemsCannotChangeBalance() throws Exception {
        long before=wallets.balance(player);
        assertEquals("invalid",trade("minecraft:diamond",-1,false));
        assertEquals("invalid",trade("minecraft:diamond",65,true));
        assertEquals("restricted",trade("minecraft:command_block",1,false));
        assertEquals("restricted",trade("missing:thing",1,false));
        assertEquals("items",trade("minecraft:diamond",1,true));
        assertEquals(before,wallets.balance(player));
    }
    @Test void saveFailureRestoresInventoryAndWallet() throws Exception {
        long before=wallets.balance(player);Files.createDirectory(dir.resolve("wallets.json.tmp"));
        assertThrows(java.io.IOException.class,()->trade("minecraft:diamond",16,false));
        assertEquals(before,wallets.balance(player));assertTrue(inventory.getItem(0).isEmpty());
        inventory.setItem(0,new ItemStack(Items.DIAMOND,16));
        assertThrows(java.io.IOException.class,()->trade("minecraft:diamond",16,true));
        assertEquals(16,inventory.getItem(0).getCount());assertEquals(before,wallets.balance(player));
    }
    @Test void zeroStartAndMoreExpensivePrices() throws Exception {
        WalletStore zero=new WalletStore(dir.resolve("zero.json"),new ShopConfig().startingBalance);
        assertEquals(0,zero.balance(player));
        assertEquals(200000,catalogue.entry("minecraft:diamond").buy());
        assertEquals(25000,catalogue.entry("minecraft:diamond").sell());
        inventory.setItem(0,new ItemStack(Items.DIAMOND));
        assertEquals("sold",TradeEngine.trade(catalogue,zero,inventory,player,"minecraft:diamond",1,true));
        assertEquals(25000,zero.balance(player));
    }
    @Test void uncraftConsumesWholeBatchAndReturnsMaterials() throws Exception {
        var reverse=new UncraftingCatalogue(resources,catalogue);
        assertTrue(reverse.size()>500);
        var plan=reverse.plans("minecraft:oak_planks").stream().filter(p->p.recipe().equals("minecraft:oak_planks")).findFirst().orElseThrow();
        assertEquals(4,plan.consume());assertEquals(0,plan.fee());
        inventory.setItem(0,new ItemStack(Items.OAK_PLANKS,3));
        assertEquals("amount",UncraftingEngine.uncraft(reverse,catalogue,wallets,inventory,player,0,plan.recipe(),plan.input(),1));
        assertEquals(3,inventory.getItem(0).getCount());
        inventory.setItem(0,new ItemStack(Items.OAK_PLANKS,8));
        assertEquals("done",UncraftingEngine.uncraft(reverse,catalogue,wallets,inventory,player,0,plan.recipe(),plan.input(),2));
        assertTrue(inventory.contains(new ItemStack(Items.OAK_LOG)));
        assertEquals(2,inventory.getItem(0).getCount());
    }
    @Test void unsafeRecipesAndModifiedItemsCannotBeUncrafted() throws Exception {
        var reverse=new UncraftingCatalogue(resources,catalogue);
        assertTrue(reverse.plans("minecraft:cake").isEmpty(),"Milk buckets leave crafting remainders");
        var plan=reverse.plans("minecraft:diamond_sword").getFirst();
        ItemStack sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(5);inventory.setItem(0,sword);
        assertEquals("special",UncraftingEngine.uncraft(reverse,catalogue,wallets,inventory,player,0,plan.recipe(),plan.input(),1));
        assertEquals("recipe",UncraftingEngine.uncraft(reverse,catalogue,wallets,inventory,player,0,"fake:recipe",plan.input(),1));
        assertEquals(5,inventory.getItem(0).getDamageValue());
    }
    @Test void uncraftFullInventoryDoesNotConsumeSource() throws Exception {
        var reverse=new UncraftingCatalogue(resources,catalogue);var plan=reverse.plans("minecraft:diamond_sword").getFirst();
        for(int i=0;i<36;i++) inventory.setItem(i,new ItemStack(Items.COBBLESTONE,64));
        inventory.setItem(0,new ItemStack(Items.DIAMOND_SWORD));
        assertEquals("space",UncraftingEngine.uncraft(reverse,catalogue,wallets,inventory,player,0,plan.recipe(),plan.input(),1));
        assertTrue(inventory.getItem(0).is(Items.DIAMOND_SWORD));
    }
    @Test void uncraftValueDifferenceRequiresFundsAndSaveFailurePreservesSource() throws Exception {
        ShopConfig config=new ShopConfig();
        config.prices.put("minecraft:diamond_sword",new ShopConfig.Override(100,true,true));
        Catalogue cheap=new Catalogue(resources,config,dir.resolve("cheap.csv"));
        var reverse=new UncraftingCatalogue(resources,cheap);var plan=reverse.plans("minecraft:diamond_sword").getFirst();
        long returned=plan.returns().stream().mapToLong(m->cheap.entry(m.id()).buy()*m.count()).sum();
        assertEquals(returned-100,plan.fee());assertTrue(plan.fee()>0);
        wallets.set(player,0);inventory.setItem(0,new ItemStack(Items.DIAMOND_SWORD));
        assertEquals("funds",UncraftingEngine.uncraft(reverse,cheap,wallets,inventory,player,0,plan.recipe(),plan.input(),1));
        assertTrue(inventory.getItem(0).is(Items.DIAMOND_SWORD));
        wallets.set(player,plan.fee());Files.createDirectory(dir.resolve("wallets.json.tmp"));
        assertThrows(java.io.IOException.class,()->UncraftingEngine.uncraft(reverse,cheap,wallets,inventory,player,0,plan.recipe(),plan.input(),1));
        assertEquals(plan.fee(),wallets.balance(player));assertTrue(inventory.getItem(0).is(Items.DIAMOND_SWORD));
        Files.delete(dir.resolve("wallets.json.tmp"));
        assertEquals("done",UncraftingEngine.uncraft(reverse,cheap,wallets,inventory,player,0,plan.recipe(),plan.input(),1));
        assertEquals(0,wallets.balance(player));assertTrue(inventory.contains(new ItemStack(Items.DIAMOND)));
    }
    @Test void sellAllSpansStacksAndProtectsSpecialItemsAndEquipment() throws Exception {
        inventory.setItem(0,new ItemStack(Items.DIAMOND,64));inventory.setItem(1,new ItemStack(Items.DIAMOND,64));
        ItemStack named=new ItemStack(Items.DIAMOND,5);named.set(DataComponents.CUSTOM_NAME,Component.literal("Keep me"));inventory.setItem(2,named);
        inventory.setItem(40,new ItemStack(Items.DIAMOND,10));
        long before=wallets.balance(player);
        assertEquals("sold",TradeEngine.sell(catalogue,wallets,inventory,player,"minecraft:diamond",0));
        assertEquals(before+128*catalogue.entry("minecraft:diamond").sell(),wallets.balance(player));
        assertTrue(inventory.getItem(0).isEmpty());assertTrue(inventory.getItem(1).isEmpty());
        assertEquals(5,inventory.getItem(2).getCount());assertEquals(10,inventory.getItem(40).getCount());
        assertEquals("items",TradeEngine.sell(catalogue,wallets,inventory,player,"minecraft:diamond",0));
    }
    @Test void sellMenuQuantitiesAndBulkSaveFailureAreAtomic() throws Exception {
        for(int amount:new int[]{1,16,32,64}) {
            inventory.clearContent();inventory.setItem(0,new ItemStack(Items.DIAMOND,64));
            long before=wallets.balance(player);
            assertEquals("sold",TradeEngine.sell(catalogue,wallets,inventory,player,"minecraft:diamond",amount));
            assertEquals(64-amount,inventory.getItem(0).getCount());
            assertEquals(before+amount*catalogue.entry("minecraft:diamond").sell(),wallets.balance(player));
        }
        inventory.setItem(0,new ItemStack(Items.DIAMOND,64));inventory.setItem(1,new ItemStack(Items.DIAMOND,64));
        long before=wallets.balance(player);
        assertEquals("invalid",TradeEngine.sell(catalogue,wallets,inventory,player,"minecraft:diamond",2));
        assertEquals("invalid",TradeEngine.sell(catalogue,wallets,inventory,player,"minecraft:diamond",-1));
        Files.createDirectory(dir.resolve("wallets.json.tmp"));
        assertThrows(java.io.IOException.class,()->TradeEngine.sell(catalogue,wallets,inventory,player,"minecraft:diamond",0));
        assertEquals(64,inventory.getItem(0).getCount());assertEquals(64,inventory.getItem(1).getCount());assertEquals(before,wallets.balance(player));
    }
    @Test void spawnersAndEveryVanillaSpawnEggCanBePurchased() throws Exception {
        wallets.set(player,Money.MAX/2);
        assertEquals(10000000,catalogue.entry("minecraft:spawner").buy());
        assertEquals(50000000,catalogue.entry("minecraft:trial_spawner").buy());
        int eggs=0;
        for(var entry:catalogue.all()) if(entry.id().equals("minecraft:spawner")||entry.id().equals("minecraft:trial_spawner")
            ||entry.id().startsWith("minecraft:")&&entry.id().endsWith("_spawn_egg")) {
            inventory.clearContent();long before=wallets.balance(player);
            assertTrue(entry.buyable(),entry.id());assertEquals("bought",trade(entry.id(),1,false),entry.id());
            assertEquals(before-entry.buy(),wallets.balance(player));
            assertTrue(ItemStack.isSameItemSameComponents(new ItemStack(catalogue.item(entry.id())),inventory.getItem(0)),entry.id());
            if(entry.id().endsWith("_spawn_egg")) eggs++;
        }
        assertTrue(eggs>70);
        assertTrue(catalogue.entry("minecraft:blaze_spawn_egg").buy()>catalogue.entry("minecraft:cow_spawn_egg").buy());
        assertFalse(catalogue.entry("minecraft:command_block").buyable());
    }
}
