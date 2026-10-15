package dev.eymen.balancedshop.menu;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import java.util.UUID;

/** Virtual catalogue, no transferable shop slots. Item creation/removal is exclusively a server transaction. */
public final class ShopMenu extends AbstractContainerMenu {
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,"balancedshop");
    public static final RegistryObject<MenuType<ShopMenu>> TYPE=MENUS.register("market",()->IForgeMenuType.create(ShopMenu::new));
    public final UUID session;
    public long sequence=-1;
    public int lastRequest=-100,lastTrade=-100;
    public Preview preview;
    public record Preview(String item,int quantity,long revision,dev.eymen.balancedshop.data.DynamicMarket.Quote buy,dev.eymen.balancedshop.data.DynamicMarket.Quote sell) {
        public boolean matches(String item,int quantity,long revision,boolean selling,dev.eymen.balancedshop.data.DynamicMarket.Quote fresh){
            return this.item.equals(item)&&this.quantity==quantity&&this.revision==revision&&(selling?sell:buy).equals(fresh);
        }
    }
    public static void register(IEventBus bus){MENUS.register(bus);}
    public ShopMenu(int id,Inventory inventory,FriendlyByteBuf buf){this(id,inventory,buf.readUUID());}
    public ShopMenu(int id,Inventory inventory,UUID session){super(TYPE.get(),id);this.session=session;}
    @Override public boolean stillValid(Player player){return player.isAlive()&&!player.isSpectator();}
    @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
}
