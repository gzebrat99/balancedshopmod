package dev.eymen.balancedshop.legacy;
public interface LegacyCodec<B,T> {
    T decode(B buffer);
    void encode(B buffer,T value);
    static <B,T> LegacyCodec<B,T> unit(T value){return new LegacyCodec<>(){public T decode(B buffer){return value;}public void encode(B buffer,T packet){}};}
}
