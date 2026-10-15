package dev.eymen.balancedshop.data;

import com.google.gson.JsonParser;
import dev.eymen.balancedshop.Money;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Tek Overworld kaydı: boyut değiştirmek bakiye veya stok değiştirmez. Yalnızca sunucu iş parçacığı kullanır. */
public final class EconomyData extends SavedData {
    public static final String NAME="balancedshop_economy";
    public static final long INITIAL_STOCK=4096, MAX_STOCK=1_000_000_000L;
    private final Map<UUID,Long> balances=new HashMap<>();
    private final Map<String,Long> stocks=new HashMap<>();
    private boolean migrated;
    private long revision;

    public static EconomyData get(MinecraftServer server,Path legacy) throws IOException {
        Path file=server.getWorldPath(LevelResource.ROOT).resolve("data/"+NAME+".dat");
        // Vanilla computeIfAbsent can recover from a read error by creating empty data.
        // For money, fail closed instead of silently resetting wallets.
        if(Files.exists(file)) try { load(NbtIo.readCompressed(file.toFile()).getCompound("data")); }
        catch(RuntimeException e){throw new IOException("Invalid economy data; restore a world backup",e);}
        var data=server.overworld().getDataStorage().computeIfAbsent(EconomyData::load,EconomyData::new,NAME);
        data.migrate(legacy);
        return data;
    }
    public void migrate(Path legacy) throws IOException {
        if(migrated)return;
        Map<UUID,Long> imported=new HashMap<>();
        if(Files.exists(legacy)) {
            try {
                var json=JsonParser.parseString(Files.readString(legacy)).getAsJsonObject();
                if(json.get("version").getAsInt()!=1)throw new IllegalArgumentException("Wallet version");
                for(var e:json.getAsJsonObject("balances").entrySet()) {
                    long value=e.getValue().getAsBigDecimal().longValueExact();validate(value);
                    imported.put(UUID.fromString(e.getKey()),value);
                }
            }catch(RuntimeException e){throw new IOException("Invalid legacy wallets",e);}
            Path backup=legacy.resolveSibling("wallets.pre-2.1.0.json");
            if(!Files.exists(backup))Files.copy(legacy,backup);
        }
        imported.forEach(balances::putIfAbsent);
        migrated=true;setDirty();
    }
    public static EconomyData load(CompoundTag tag) {
        if(tag.getInt("Version")!=1)throw new IllegalArgumentException("Economy data version");
        EconomyData data=new EconomyData();
        var money=tag.getCompound("Balances");
        for(String key:money.getAllKeys()){if(!money.contains(key,4))throw new IllegalArgumentException("Balance type");long n=money.getLong(key);validate(n);data.balances.put(UUID.fromString(key),n);}
        var stock=tag.getCompound("Stocks");
        for(String key:stock.getAllKeys()){if(!stock.contains(key,4))throw new IllegalArgumentException("Stock type");long n=stock.getLong(key);if(n<0||n>MAX_STOCK)throw new IllegalArgumentException("Stock bounds");data.stocks.put(key,n);}
        data.migrated=tag.getBoolean("Migrated");data.revision=Math.max(0,tag.getLong("Revision"));return data;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        tag.putInt("Version",1);tag.putBoolean("Migrated",migrated);tag.putLong("Revision",revision);
        CompoundTag money=new CompoundTag(),stock=new CompoundTag();
        balances.forEach((id,n)->money.putLong(id.toString(),n));stocks.forEach(stock::putLong);
        tag.put("Balances",money);tag.put("Stocks",stock);return tag;
    }
    public long balance(UUID id){return balances.getOrDefault(id,0L);}
    public void setBalance(UUID id,long value){validate(value);balances.put(id,value);setDirty();}
    public long stock(String id){return stocks.getOrDefault(id,INITIAL_STOCK);}
    public long revision(){return revision;}
    public void stock(String id,long value){if(value<0||value>MAX_STOCK)throw new IllegalArgumentException("Stock bounds");stocks.put(id,value);revision++;setDirty();}
    /** Both accounts are validated before either changes; /pay cannot mint or lose money. */
    public boolean transfer(UUID from,UUID to,long amount) {
        if(from.equals(to)||amount<=0||amount>Money.MAX||balance(from)<amount||balance(to)>Money.MAX-amount)return false;
        long sender=balance(from)-amount,receiver=balance(to)+amount;
        balances.put(from,sender);balances.put(to,receiver);setDirty();return true;
    }
    private static void validate(long value){if(value<0||value>Money.MAX)throw new IllegalArgumentException("Balance bounds");}
}
