package dev.eymen.balancedshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ShopConfig {
    public int schemaVersion = 3;
    public long startingBalance = 0;
    public double priceMultiplier = 4.0;
    public double sellRatio = 0.125;
    public boolean allowRestrictedItems = false;
    public boolean sellingEnabled = true;
    public boolean createVillagersEnabled = true;
    public boolean upgradeDestroyOnFailure = true;
    public int transactionCooldownTicks = 4;
    public boolean veinMiningEnabled = true;
    public int veinMiningMaxBlocks = 64;
    public int veinMiningRadius = 6;
    public Map<String, Override> prices = new LinkedHashMap<>();
    public Map<String, Double> namespaceMultipliers = new LinkedHashMap<>();
    public Map<String, Long> spawnerPrices = new LinkedHashMap<>();
    public Map<String, Long> enchantmentPrices = new LinkedHashMap<>();

    public record Override(long buy, boolean buyable, boolean sellable) {}
    public ShopConfig() {
        namespaceMultipliers.put("create", 1.25);
    }
    public static ShopConfig load() throws IOException {
        return load(FabricLoader.getInstance().getConfigDir().resolve("balancedshop.json"));
    }
    static ShopConfig load(Path path) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            ShopConfig config = new ShopConfig();
            Files.writeString(path, gson.toJson(config), StandardCharsets.UTF_8);
            return config;
        }
        String json = Files.readString(path, StandardCharsets.UTF_8);
        ShopConfig config = gson.fromJson(json, ShopConfig.class);
        if (config == null) throw new IOException("Empty shop configuration");
        if (!com.google.gson.JsonParser.parseString(json).getAsJsonObject().has("schemaVersion")) {
            Files.copy(path,path.resolveSibling("balancedshop-v1.backup.json"),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            config.schemaVersion=2;config.startingBalance=0;config.priceMultiplier=4;config.sellRatio=0.125;
            config.validate();Files.writeString(path,gson.toJson(config),StandardCharsets.UTF_8);
        }
        if(config.schemaVersion<3) {
            Files.copy(path,path.resolveSibling("balancedshop-v2.backup.json"),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            config.schemaVersion=3;config.startingBalance=0;
            config.prices.keySet().removeIf(FarmEconomy::managed);
            config.validate();Files.writeString(path,gson.toJson(config),StandardCharsets.UTF_8);
        }
        config.validate();
        return config;
    }
    public void validate() throws IOException {
        if (startingBalance < 0 || startingBalance > Money.MAX || !Double.isFinite(priceMultiplier)
                || priceMultiplier < 1 || priceMultiplier > 100 || !Double.isFinite(sellRatio)
                || sellRatio < 0 || sellRatio > 0.5 || transactionCooldownTicks < 1
                || transactionCooldownTicks > 200 || veinMiningMaxBlocks<1||veinMiningMaxBlocks>256||veinMiningRadius<1||veinMiningRadius>12||prices == null || namespaceMultipliers == null || enchantmentPrices == null || spawnerPrices == null)
            throw new IOException("Invalid balancedshop.json economy settings");
        for (var entry : prices.entrySet()) {
            Override value = entry.getValue();
            if (entry.getKey() == null || value == null || value.buy < 1 || value.buy > Money.MAX_PRICE)
                throw new IOException("Invalid price override: " + entry.getKey());
        }
        for (var entry : namespaceMultipliers.entrySet())
            if (entry.getValue() == null || !Double.isFinite(entry.getValue())
                    || entry.getValue() < 0.1 || entry.getValue() > 100)
                throw new IOException("Invalid namespace multiplier: " + entry.getKey());
        if(schemaVersion>=3)for(var bundle:FarmEconomy.BUNDLES.values()){
            Override unit=prices.get("minecraft:"+bundle.material());
            if(unit!=null)try{FarmEconomy.bundlePrice(unit.buy(),bundle.count());}
            catch(IllegalArgumentException e){throw new IOException("Material price too large for its storage block: "+bundle.material(),e);}
        }
        for(var entry:spawnerPrices.entrySet())
            if(entry.getKey()==null||entry.getValue()==null||entry.getValue()<1||entry.getValue()>Money.MAX_PRICE)
                throw new IOException("Invalid spawner price: "+entry.getKey());
        for(var entry:enchantmentPrices.entrySet())
            if(entry.getKey()==null||entry.getValue()==null||entry.getValue()<1||entry.getValue()>Money.MAX_PRICE)
                throw new IOException("Invalid enchanted book price: "+entry.getKey());
    }
}
