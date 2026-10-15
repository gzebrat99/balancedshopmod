package dev.eymen.balancedshop.legacy;
import net.minecraft.resources.ResourceLocation;
public interface LegacyPayload {
    record Type<T extends LegacyPayload>(ResourceLocation id) {}
    Type<? extends LegacyPayload> type();
}
