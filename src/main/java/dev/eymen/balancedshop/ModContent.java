package dev.eymen.balancedshop;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModContent {
    public static final Identifier SPAWNER_ID=Identifier.parse("balancedshop:mob_spawner");
    public static final Identifier ID=Identifier.parse("balancedshop:uncrafting_table");
    public static final Block TABLE=Registry.register(BuiltInRegistries.BLOCK,ID,
        new CraftingTableBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).setId(ResourceKey.create(Registries.BLOCK,ID))));
    public static final Item TABLE_ITEM=Registry.register(BuiltInRegistries.ITEM,ID,
        new BlockItem(TABLE,new Item.Properties().setId(ResourceKey.create(Registries.ITEM,ID))));
    public static final Item SPAWNER_ITEM=Registry.register(BuiltInRegistries.ITEM,SPAWNER_ID,new MobSpawnerItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,SPAWNER_ID))));
    private ModContent() {}
    public static void initialize() {
        CoinItems.initialize();
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries->CoinItems.ALL.forEach(coin->entries.accept(coin.item())));
        
    }
}
