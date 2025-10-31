package br.com.acdev.revenium.components;

import java.math.BigInteger;

public class Commons {

    public static BigInteger toBigInteger(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value);
        try {
            return new BigInteger(s);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static BigInteger nvl(BigInteger v) {
        return v == null ? BigInteger.ZERO : v;
    }
}
