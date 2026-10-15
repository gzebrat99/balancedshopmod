package dev.eymen.balancedshop;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class EconomyTest {
    @TempDir Path dir;
    @Test void compressedPricesCannotBeClippedIntoBuySellArbitrage() throws Exception {
        assertEquals(225000,FarmEconomy.bundlePrice(25000,9));
        assertThrows(IllegalArgumentException.class,()->FarmEconomy.bundlePrice(Money.MAX_PRICE,9));
        ShopConfig config=new ShopConfig();config.prices.put("minecraft:diamond",new ShopConfig.Override(Money.MAX_PRICE,true,true));
        assertThrows(java.io.IOException.class,config::validate);
    }
    @Test void upgradeOddsScaleWithValueAndFeesPreventProfitableRetryLoops(){
        assertTrue(UpgradeEngine.chance(10000,20000)>UpgradeEngine.chance(10000,100000));
        for(long source:new long[]{1,100,10000,1000000000}) for(long target:new long[]{100,10000,1000000,1000000000,10000000000L}) if(target>source){
            double p=UpgradeEngine.chance(source,target);assertTrue(p>=.001&&p<=.90);
            for(boolean destroy:new boolean[]{true,false}){
                long fee=UpgradeEngine.fee(source,target,p,destroy);assertTrue(fee>=2500);
                double expected=destroy?p*target-source:p*(target-source);
                assertTrue(fee>=expected,"No free positive expected value: "+source+" -> "+target);
            }
        }
        assertEquals(.90,UpgradeEngine.chance(1000000,100));
        assertEquals(.3328,UpgradeEngine.chance(256*400L,200000),1e-9);
    }
    @Test void moneyRejectsInvalidAmountsAndOverflow() {
        assertEquals(32000, Money.total(500,64));
        assertThrows(IllegalArgumentException.class, () -> Money.total(-1,1));
        assertThrows(IllegalArgumentException.class, () -> Money.total(100,65));
        assertThrows(IllegalArgumentException.class, () -> Money.total(100,0));
        assertThrows(IllegalArgumentException.class, () -> Money.credit(Money.MAX,1));
        assertEquals("12.34", Money.format(1234));
    }
    @Test void walletsPersistAndDoNotResetOnReconnect() throws Exception {
        Path file=dir.resolve("wallets.json"); UUID id=UUID.randomUUID();
        WalletStore first=new WalletStore(file,100000);
        assertEquals(100000,first.balance(id)); first.set(id,123456);
        WalletStore reopened=new WalletStore(file,999999);
        assertEquals(123456,reopened.balance(id));
        assertEquals(999999,reopened.balance(UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class,()->reopened.set(id,-1));
    }
    @Test void corruptWalletsArePreserved() throws Exception {
        Path file=dir.resolve("wallets.json"); Files.writeString(file,"corrupt");
        assertThrows(java.io.IOException.class,()->new WalletStore(file,100000));
        assertEquals("corrupt",Files.readString(file));
    }
    @Test void transientWindowsRenameLockDoesNotLoseWalletChange() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
        Path file=dir.resolve("wallets.json"); UUID id=UUID.randomUUID();
        WalletStore store=new WalletStore(file,0); store.balance(id);
        var executor=java.util.concurrent.Executors.newSingleThreadExecutor();
        var started=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Future<?> save;
        try {
            try(var lock=java.nio.channels.FileChannel.open(file,java.nio.file.StandardOpenOption.READ,com.sun.nio.file.ExtendedOpenOption.NOSHARE_DELETE)) {
                save=executor.submit(()->{started.countDown();store.set(id,12345);return null;});
                assertTrue(started.await(5,java.util.concurrent.TimeUnit.SECONDS));
                Thread.sleep(80);
                assertFalse(save.isDone(),"The real Windows file lock must prevent the initial rename");
            }
            save.get(5,java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(12345,store.balance(id));
            assertEquals(12345,new WalletStore(file,0).balance(id));
        } finally {executor.shutdownNow();}
    }
    @Test void failedSaveRollsBackMemory() throws Exception {
        Path file=dir.resolve("wallets.json"); UUID id=UUID.randomUUID();
        WalletStore store=new WalletStore(file,100); store.balance(id);
        Files.createDirectory(dir.resolve("wallets.json.tmp"));
        assertThrows(java.io.IOException.class,()->store.set(id,500));
        assertEquals(100,store.balance(id));
    }
    @Test void rarityAndModRulesAreMeaningful() {
        assertTrue(PriceRules.baseline("minecraft","netherite_ingot",0,false)>PriceRules.baseline("minecraft","diamond",0,false));
        assertTrue(PriceRules.baseline("create","steam_engine",0,true)>PriceRules.baseline("create","shaft",0,true));
        assertTrue(PriceRules.baseline("othermod","unknown_item",0,false)>0);
        assertTrue(PriceRules.restricted("command_block"));
        assertTrue(PriceRules.restricted("creative_motor"));
        assertFalse(PriceRules.restricted("diamond"));
    }
    @Test void oldConfigIsBackedUpAndMigratedOnceWithoutLosingOverrides() throws Exception {
        Path file=dir.resolve("balancedshop.json");
        String old="{\"startingBalance\":100000,\"sellRatio\":0.25,\"prices\":{\"minecraft:diamond\":{\"buy\":4321,\"buyable\":true,\"sellable\":false}},\"namespaceMultipliers\":{\"othermod\":2}}";
        Files.writeString(file,old);
        ShopConfig migrated=ShopConfig.load(file);
        assertEquals(0,migrated.startingBalance);assertEquals(4,migrated.priceMultiplier);assertEquals(0.125,migrated.sellRatio);
        assertFalse(migrated.prices.containsKey("minecraft:diamond"));assertEquals(2,migrated.namespaceMultipliers.get("othermod"));
        assertEquals(old,Files.readString(dir.resolve("balancedshop-v1.backup.json")));
        Files.writeString(file,Files.readString(file).replace("\"priceMultiplier\": 4.0","\"priceMultiplier\": 7.0"));
        assertEquals(7,ShopConfig.load(file).priceMultiplier);
        assertEquals(old,Files.readString(dir.resolve("balancedshop-v1.backup.json")));
    }
    @Test void bookPricesCoverLevelsTreasureCursesAndModFallback() {
        assertEquals(4000000,BookCatalogue.price("minecraft","mending",1,2,25,true,false,4,1));
        assertEquals(1600000,BookCatalogue.price("minecraft","unbreaking",3,5,21,false,false,4,1));
        assertTrue(BookCatalogue.price("minecraft","unbreaking",3,5,21,false,false,4,1)>BookCatalogue.price("minecraft","unbreaking",2,5,13,false,false,4,1));
        assertTrue(BookCatalogue.price("mod","custom",2,2,10,true,false,4,1)>BookCatalogue.price("mod","custom",2,2,10,false,false,4,1));
        assertEquals(Money.MAX_PRICE,BookCatalogue.price("mod","custom",255,1,1000,true,false,4,1));
        assertTrue(BookCatalogue.price("minecraft","binding_curse",1,1,25,true,true,4,1)>0);
        assertThrows(IllegalArgumentException.class,()->BookCatalogue.price("mod","custom",0,1,1,false,false,4,1));
    }
    @Test void bookPriceOverridesAreValidatedAndExistingConfigsKeepNewDefaults() throws Exception {
        ShopConfig config=new ShopConfig();config.enchantmentPrices.put("minecraft:mending@1",12300L);config.validate();
        config.enchantmentPrices.put("minecraft:unbreaking@3",0L);assertThrows(java.io.IOException.class,config::validate);
        Path file=dir.resolve("balancedshop.json");Files.writeString(file,"{\"schemaVersion\":2,\"startingBalance\":0}");
        assertTrue(ShopConfig.load(file).enchantmentPrices.isEmpty());
    }
    @Test void farmMigrationRepairsBadBlocksButPreservesModOverridesAndWallets() throws Exception {
        Path file=dir.resolve("balancedshop.json");
        Files.writeString(file,"{\"schemaVersion\":2,\"prices\":{\"minecraft:diamond_block\":{\"buy\":320,\"buyable\":true,\"sellable\":true},\"othermod:machine\":{\"buy\":12000,\"buyable\":true,\"sellable\":true}}}");
        Path wallet=dir.resolve("wallets.json");UUID player=UUID.randomUUID();WalletStore store=new WalletStore(wallet,0);store.set(player,43210);
        ShopConfig config=ShopConfig.load(file);assertEquals(3,config.schemaVersion);assertFalse(config.prices.containsKey("minecraft:diamond_block"));assertEquals(12000,config.prices.get("othermod:machine").buy());
        assertTrue(Files.exists(dir.resolve("balancedshop-v2.backup.json")));assertEquals(43210,new WalletStore(wallet,0).balance(player));
    }
    @Test void veinLimitsAndSpawnerOverridesAreValidated() throws Exception {
        ShopConfig config=new ShopConfig();config.veinMiningMaxBlocks=257;assertThrows(java.io.IOException.class,config::validate);config.veinMiningMaxBlocks=64;
        config.spawnerPrices.put("minecraft:iron_golem",0L);assertThrows(java.io.IOException.class,config::validate);config.spawnerPrices.put("minecraft:iron_golem",250000000L);config.validate();
    }
    @Test void infiniteFarmSpawnersCostMoreThanStarterFarmsAndFitMoneyLimits(){
        assertTrue(SpawnerPrices.buy("minecraft:iron_golem")>SpawnerPrices.buy("minecraft:chicken"));
        assertTrue(SpawnerPrices.buy("minecraft:ender_dragon")<=Money.MAX_PRICE);
        assertTrue(SpawnerPrices.buy("mod:custom_mob")>0);
        assertTrue(SpawnerPrices.sell("minecraft:villager",.125)<SpawnerPrices.buy("minecraft:villager"));
    }
}
