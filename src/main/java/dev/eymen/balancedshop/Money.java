package dev.eymen.balancedshop;

import java.util.Locale;

/** Integer hundredths: no floating-point money and no unbounded arithmetic. */
public final class Money {
    public static final long MAX = 9_000_000_000_000_000L;
    public static final long MAX_PRICE = 10_000_000_000L;
    private Money() {}
    public static long total(long price, int quantity) {
        if (price < 0 || price > MAX_PRICE || quantity < 1 || quantity > 64)
            throw new IllegalArgumentException("Invalid transaction");
        return Math.multiplyExact(price, quantity);
    }
    public static long credit(long balance, long amount) {
        if (balance < 0 || amount < 0 || balance > MAX - amount)
            throw new IllegalArgumentException("Balance limit exceeded");
        return balance + amount;
    }
    public static String format(long amount) {
        return String.format(Locale.ROOT, "%,d.%02d", amount / 100, amount % 100);
    }
}
