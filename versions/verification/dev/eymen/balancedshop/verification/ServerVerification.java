package dev.eymen.balancedshop.verification;
import dev.eymen.balancedshop.*;
import dev.eymen.balancedshop.bridge.ServerBridge;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import java.nio.file.*;
import java.util.*;
public final class ServerVerification {
    private static int checks;
    private static net.minecraft.core.BlockPos protectedPos;
    public static volatile boolean ready;
    public static void registerItems(){
        for(String name:List.of("shaft","andesite_alloy")){
            var id=ResourceLocation.fromNamespaceAndPath("create",name);
            if(!BuiltInRegistries.ITEM.containsKey(id))Registry.register(BuiltInRegistries.ITEM,id,new Item(new Item.Properties()));
        }
    }
    public static void install(){/* protection_hook */ServerBridge.ServerLifecycleEvents.SERVER_STARTED.register(ServerVerification::verify);}
    private static void check(boolean condition,String label){if(!condition)throw new AssertionError(label);checks++;}
    public static void verify(MinecraftServer server){
        try{
            Path dir=Files.createTempDirectory("balanced-shop-backport-");
            Catalogue catalogue=new Catalogue(server,new ShopConfig(),dir.resolve("prices.csv"));
            BookCatalogue books=new BookCatalogue(server,new ShopConfig(),dir.resolve("books.csv"));catalogue.attachBooks(books);
            SpawnerCatalogue spawners=new SpawnerCatalogue(server,new ShopConfig());catalogue.attachSpawners(spawners);
            check(server.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("balancedshop","uncrafting_table")).isEmpty(),"Reverse crafting recipe removed");
            check(ModContent.TABLE.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE),"Uncrafting Table axe mining tag loaded");
            check(net.minecraft.world.level.block.Block.getDrops(ModContent.TABLE.defaultBlockState(),server.overworld(),net.minecraft.core.BlockPos.ZERO,null).stream().anyMatch(stack->stack.is(ModContent.TABLE_ITEM)),"Uncrafting Table drops itself when broken");
            var inventory=new Inventory(null);var player=UUID.randomUUID();var wallets=new WalletStore(dir.resolve("wallets.json"),0);
            check(wallets.balance(player)==0,"Zero initial balance");
            check(catalogue.all().size()==BuiltInRegistries.ITEM.size()-1,"Every registered item is priced");
            check(catalogue.all().stream().allMatch(e->e.buy()>0),"All prices are positive");
            check(!catalogue.entry("minecraft:spawner").buyable(),"Generic spawner cannot be bought");
            check(!catalogue.entry("minecraft:zombie_spawn_egg").buyable(),"Spawn eggs cannot be bought");
            check(catalogue.search("","").stream().noneMatch(e->e.id().endsWith("_spawn_egg")||e.id().endsWith(":spawner")||e.id().endsWith(":mob_spawner")||e.id().endsWith(":uncrafting_table")),"Hidden legacy and generic items");
            for(var bundle:FarmEconomy.BUNDLES.entrySet())check(catalogue.entry("minecraft:"+bundle.getKey()).sell()==catalogue.entry("minecraft:"+bundle.getValue().material()).sell()*bundle.getValue().count(),"Compressed sale price "+bundle.getKey());
            for(String crop:FarmEconomy.CROPS.keySet())if(catalogue.entry("minecraft:"+crop)!=null)check(catalogue.entry("minecraft:"+crop).sellable(),"Crop can be sold "+crop);
            check(spawners.entry("minecraft:iron_golem")!=null&&spawners.entry("minecraft:villager")!=null,"Farm mobs have spawners");
            check(spawners.all().size()>60,"All summonable mob types catalogued");
            check(spawners.entry("minecraft:iron_golem").buy()>spawners.entry("minecraft:zombie").buy(),"Iron farms cost more than starter farms");
            check(catalogue.sellValue(spawners.entry("minecraft:iron_golem").stack())==spawners.entry("minecraft:iron_golem").sell(),"Typed spawner sale value");
            check(MobSpawnerItem.entityKey(spawners.entry("minecraft:villager").stack()).equals("minecraft:villager"),"Spawner keeps mob metadata");
            check(books.entry("minecraft:mending",1)!=null,"Mending exists");check(books.entry("minecraft:unbreaking",3)!=null,"Unbreaking III exists");
            check(books.all().size()>=100,"All vanilla enchantment levels are priced");
            check(TradeEngine.trade(catalogue,wallets,inventory,player,"minecraft:diamond",1,false).equals("funds"),"No free purchases");
            inventory.setItem(0,new ItemStack(Items.COBBLESTONE,64));
            check(TradeEngine.sell(catalogue,wallets,inventory,player,"minecraft:cobblestone",16).equals("sold"),"Sell 16");check(inventory.getItem(0).getCount()==48,"Exact sale quantity");
            wallets.set(player,1_000_000_000);long before=wallets.balance(player);
            check(TradeEngine.trade(catalogue,wallets,inventory,player,"minecraft:diamond",32,false).equals("bought"),"Buy 32");
            check(wallets.balance(player)==before-catalogue.entry("minecraft:diamond").buy()*32,"Exact purchase charge");
            var sword=new ItemStack(Items.DIAMOND_SWORD);check(catalogue.sellValue(sword)>0,"Tools can be sold");long full=catalogue.sellValue(sword);sword.setDamageValue(sword.getMaxDamage()/2);check(catalogue.sellValue(sword)<full,"Damaged tool value");
            var named=TestItems.named(new ItemStack(Items.DIAMOND),"Protected");check(catalogue.sellValue(named)==catalogue.entry("minecraft:diamond").sell(),"Named items can be sold");
            for(int i=0;i<36;i++)inventory.setItem(i,ItemStack.EMPTY);
            before=wallets.balance(player);for(int i=0;i<4;i++)check(CoinEngine.withdraw(wallets,inventory,player,i,16).equals("withdrawn"),"Withdraw denomination "+i);
            check(before==wallets.balance(player)+CoinItems.total(inventory),"Coin value conserved");
            check(CoinEngine.deposit(wallets,inventory,player,0,0,true).equals("deposited"),"Deposit all");check(wallets.balance(player)==before,"Coin roundtrip");
            inventory.setItem(0,TestItems.named(new ItemStack(CoinItems.entry(1).item(),16),"Fake"));check(CoinEngine.deposit(wallets,inventory,player,0,0,true).equals("items"),"Named coins rejected");inventory.setItem(0,ItemStack.EMPTY);
            check(BookPurchase.buy(books,wallets,inventory,player,"minecraft:mending",1,1).equals("bought"),"Buy a real Mending book");check(inventory.getItem(0).is(Items.ENCHANTED_BOOK),"Book has correct item");
            check(BookCatalogue.name(inventory.getItem(0)).getString().length()>0,"Enchantment tooltip is present");
            for(int i=0;i<36;i++)inventory.setItem(i,ItemStack.EMPTY);
            check(SpawnerPurchase.buy(spawners,wallets,inventory,player,"minecraft:villager",1).equals("bought"),"Purchase typed villager spawner");
            check(MobSpawnerItem.entityKey(inventory.getItem(0)).equals("minecraft:villager"),"Purchased cage contains chosen mob");
            inventory.setItem(1,MobSpawnerItem.create("minecraft:iron_golem"));
            check(TradeEngine.sell(catalogue,wallets,inventory,player,TradeEngine.saleKey(inventory.getItem(0)),0).equals("sold"),"Sell selected typed spawner");
            check(MobSpawnerItem.entityKey(inventory.getItem(1)).equals("minecraft:iron_golem"),"Other mob spawners remain in inventory");
            wallets.set(player,50_000_000_000L);
            for(var spawner:spawners.all())check(spawner.buy()<=Money.MAX_PRICE,"Spawner price fits transaction limits "+spawner.id());
            check(SpawnerPurchase.buy(spawners,wallets,inventory,player,"minecraft:ender_dragon",1).equals("bought"),"Highest-price spawner can be purchased");
            for(int i=0;i<36;i++)inventory.setItem(i,ItemStack.EMPTY);inventory.setItem(0,new ItemStack(Items.PUMPKIN_SEEDS,32));
            check(TradeEngine.sell(catalogue,wallets,inventory,player,"minecraft:pumpkin_seeds",7).equals("sold"),"Sell arbitrary seed quantity");
            check(inventory.getItem(0).getCount()==25,"Exact custom sale quantity");
            for(int i=0;i<36;i++)inventory.setItem(i,ItemStack.EMPTY);inventory.setItem(0,new ItemStack(Items.DIAMOND,32));
            var target=new UpgradeEngine.Target("minecraft:netherite_ingot",new ItemStack(Items.NETHERITE_INGOT),catalogue.entry("minecraft:netherite_ingot").buy());
            var quote=UpgradeEngine.quote(inventory,0,target,catalogue.sellValue(inventory.getItem(0)),16,1,true);check(quote!=null&&quote.chance()<=.9,"Batch chance capped at 90%");
            check(UpgradeEngine.execute(quote,wallets,inventory,player,0,true,()->1).equals("failed"),"Deterministic failed roll");check(inventory.getItem(0).getCount()==16,"Failure consumes entire stake");
            quote=UpgradeEngine.quote(inventory,0,target,catalogue.sellValue(inventory.getItem(0)),16,1,true);check(UpgradeEngine.execute(quote,wallets,inventory,player,0,true,()->0).equals("success"),"Deterministic success");
            var options=UpgradeEngine.targets(catalogue,books,new ItemStack(Items.DIAMOND_PICKAXE),true,"unbreaking");check(options.stream().anyMatch(t->t.key().equals("minecraft:unbreaking@3")),"Tool enchant upgrade III");
            check(CreateTraderService.offers(catalogue,"").stream().allMatch(e->e.id().startsWith("create:")),"Create trader only Create");check(CreateTraderService.offers(catalogue,"").size()>=2,"Create items detected");
            var level=server.overworld();var origin=new net.minecraft.core.BlockPos(100,80,100);
            level.getChunkAt(origin);
            var miner=new net.minecraft.server.level.ServerPlayer(server,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ShopVerifier"),net.minecraft.server.level.ClientInformation.createDefault());
            miner.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),miner,net.minecraft.server.network.CommonListenerCookie.createInitial(miner.getGameProfile(),false));
            miner.setPos(100.5,80,102.5);miner.getInventory().setItem(0,new ItemStack(Items.DIAMOND_PICKAXE));
            for(int i=0;i<5;i++)level.setBlockAndUpdate(origin.offset(i,0,0),net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState());
            level.setBlockAndUpdate(origin.offset(0,1,0),net.minecraft.world.level.block.Blocks.GOLD_ORE.defaultBlockState());
            check(VeinMiningService.plan(miner,origin,64,6).size()==5,"Vein planning only connects identical blocks");
            check(VeinMiningService.plan(miner,origin,2,6).size()==2,"Vein block limit enforced");
            check(VeinMiningService.plan(miner,origin,64,1).size()==2,"Vein radius enforced");
            protectedPos=origin;check(!miner.gameMode.destroyBlock(origin),"Native block protection hook cancels mining");
            check(!level.getBlockState(origin).isAir(),"Protected vein block stays intact");protectedPos=null;
            check(miner.gameMode.destroyBlock(origin),"Native vein destruction succeeds");
            check(level.getBlockState(origin).isAir(),"Native vein removes the block");
            check(miner.getMainHandItem().getDamageValue()==1,"Native vein consumes tool durability");
            level.setBlockAndUpdate(origin,net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState());
            check(!VeinMiningService.eligible(miner,origin),"Unbreakable blocks excluded");
            level.setBlockAndUpdate(origin,net.minecraft.world.level.block.Blocks.SPAWNER.defaultBlockState());
            check(!VeinMiningService.eligible(miner,origin),"Block entities excluded from mining");
            level.setBlockAndUpdate(origin,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(origin.below(),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            miner.getInventory().setItem(0,MobSpawnerItem.create("minecraft:iron_golem"));
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(origin.below()),net.minecraft.core.Direction.UP,origin.below(),false);
            var placement=new net.minecraft.world.item.context.BlockPlaceContext(new net.minecraft.world.item.context.UseOnContext(miner,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
            check(((MobSpawnerItem)ModContent.SPAWNER_ITEM).place(placement).consumesAction(),"Typed cage can be placed in survival");
            var cage=(net.minecraft.world.level.block.entity.SpawnerBlockEntity)level.getBlockEntity(origin);
            check(cage.getSpawner().save(new net.minecraft.nbt.CompoundTag()).getCompound("SpawnData").getCompound("entity").getString("id").equals("minecraft:iron_golem"),"Survival placement sets mob without operator permissions");
            MobSpawnerItem.configure(cage,level,origin,"minecraft:iron_golem");
            var saved=cage.getSpawner().save(new net.minecraft.nbt.CompoundTag());
            check(saved.getCompound("SpawnData").getCompound("entity").getString("id").equals("minecraft:iron_golem"),"Placed spawner persists chosen entity");
            check(saved.getCompound("SpawnData").contains("custom_spawn_rules"),"Spawner light rules cover daylight farms");
            for(int i=0;i<5;i++)level.setBlockAndUpdate(origin.offset(i,0,0),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(origin.offset(0,1,0),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            Files.createDirectories(Path.of("verification"));Files.writeString(Path.of("verification/server.txt"),"PASS "+checks+" assertions\nMinecraft "+net.minecraft.SharedConstants.getCurrentVersion().getName()+"\n");ready=true;BalancedShop.LOG.info("Backport verification PASS: {} assertions",checks);
        }catch(Throwable e){try{Files.createDirectories(Path.of("verification"));Files.writeString(Path.of("verification/FAILED.txt"),e.toString());}catch(Exception ignored){}BalancedShop.LOG.error("Backport verification failed",e);}
        finally{if(server.isDedicatedServer())server.halt(false);}
    }
}
