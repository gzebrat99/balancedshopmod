package dev.eymen.balancedshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-world UUID wallets. Invalid files are never silently replaced. */
public final class WalletStore {
    private final Path file;
    private final long startingBalance;
    private final Map<UUID, Long> balances = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public WalletStore(Path file, long startingBalance) throws IOException {
        this.file = file;
        this.startingBalance = startingBalance;
        if (Files.exists(file)) {
            try {
                JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                if (root.get("version").getAsInt() != 1) throw new IllegalArgumentException("Unsupported wallet version");
                for (var entry : root.getAsJsonObject("balances").entrySet()) {
                    long value = entry.getValue().getAsBigDecimal().longValueExact();
                    if (value < 0 || value > Money.MAX) throw new IllegalArgumentException("Invalid wallet value");
                    balances.put(UUID.fromString(entry.getKey()), value);
                }
            } catch (RuntimeException e) { throw new IOException("Invalid wallet file: " + file, e); }
        }
    }
    public synchronized long balance(UUID player) throws IOException {
        if (!balances.containsKey(player)) {
            balances.put(player, startingBalance);
            try { save(); } catch (IOException e) { balances.remove(player); throw e; }
        }
        return balances.get(player);
    }
    public synchronized void set(UUID player, long amount) throws IOException {
        if (amount < 0 || amount > Money.MAX) throw new IllegalArgumentException("Balance out of range");
        Long before = balances.put(player, amount);
        try { save(); } catch (IOException e) {
            if (before == null) balances.remove(player); else balances.put(player, before);
            throw e;
        }
    }
    private void save() throws IOException {
        Files.createDirectories(file.getParent());
        JsonObject root = new JsonObject();
        root.addProperty("version", 1);
        JsonObject values = new JsonObject();
        balances.entrySet().stream().sorted(Map.Entry.comparingByKey())
            .forEach(e -> values.addProperty(e.getKey().toString(), e.getValue()));
        root.add("balances", values);
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, GSON.toJson(root), StandardCharsets.UTF_8);
        // Windows sync/antivirus can briefly lock the destination; keep the atomic write and bounded rollback.
        for (int attempt = 0; ; attempt++) {
            try {
                try { Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException e) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
                return;
            } catch (AccessDeniedException e) {
                if (attempt >= 7) throw e;
                try { Thread.sleep(Math.min(10L << attempt, 100L)); }
                catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Wallet save interrupted", interrupted);
                }
            }
        }
    }
}
