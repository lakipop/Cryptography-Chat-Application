package crypto;

// File: KeyGenerator.java
import java.security.SecureRandom;

public class KeyGenerator {
    public static String generate128BitKeyHex() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[16]; // 128 bits = 16 bytes
        random.nextBytes(key);

        // Convert to hex (no javax.xml.bind)
        StringBuilder hexString = new StringBuilder();
        for (byte b : key) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) hex = "0" + hex;
            hexString.append(hex);
        }

        return hexString.toString(); // 32-char hex => 128-bit key
    }

    public static void main(String[] args) {
        String key = generate128BitKeyHex();
        System.out.println("🔑 Generated 128-bit Key (Hex): " + key);
    }
}