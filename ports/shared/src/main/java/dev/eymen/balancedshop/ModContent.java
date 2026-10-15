package dev.eymen.balancedshop;
import dev.eymen.balancedshop.bridge.ServerBridge.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
/** Native RegisterEvent supplies the registry window; keep values available to shared engines. */
public final class ModContent {
    public static final Identifier SPAWNER_ID=Identifier.parse("balancedshop:mob_spawner");
    public static final Identifier ID=Identifier.parse("balancedshop:uncrafting_table");
    public static Block TABLE;
    public static Item TABLE_ITEM;
    public static Item SPAWNER_ITEM;
    public static void registerBlocks(){TABLE=Registry.register(BuiltInRegistries.BLOCK,ID,new CraftingTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).setId(ResourceKey.create(Registries.BLOCK,ID))));}
    public static void registerItems(){TABLE_ITEM=Registry.register(BuiltInRegistries.ITEM,ID,new BlockItem(TABLE,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,ID))));SPAWNER_ITEM=Registry.register(BuiltInRegistries.ITEM,SPAWNER_ID,new MobSpawnerItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,SPAWNER_ID))));CoinItems.initialize();dev.eymen.balancedshop.bridge.VerificationHook.call("ServerVerification","registerItems");}
    public static void initialize(){
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries->CoinItems.ALL.forEach(c->entries.accept(c.item())));
        
    }
}
