package dev.eymen.balancedshop.bridge;
import net.minecraft.server.level.ServerPlayer;
import dev.eymen.balancedshop.bridge.mixin.PlayerListAccess;
public final class PlayerPersistence {
    public static void save(ServerPlayer player){((PlayerListAccess)player.level().getServer().getPlayerList()).balancedshop$save(player);}
}
