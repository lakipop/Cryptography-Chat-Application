import java.security.SecureRandom;
import java.util.Base64;

public class BlockCipher {
    private String key128Bit;
    
    // IMPROVEMENT #3: Increased from 3 to 10 rounds for stronger security
    private static final int ROUNDS = 10;
    
    // IMPROVEMENT #2: IV size in bytes
    private static final int IV_SIZE = 16; // 128-bit IV

    public BlockCipher(String key128Bit) {
        if (key128Bit == null || key128Bit.length() != 32) { // 32 hex chars = 128 bits
            throw new IllegalArgumentException("Key must be a 32-character hex string (128-bit).");
        }
        this.key128Bit = key128Bit;
    }

    public String encrypt(String plaintext) {
        System.out.println("\n🔐 Encrypting with key: " + this.key128Bit);
        
        // IMPROVEMENT #2: Generate random IV for each message
        byte[] iv = generateIV();
        System.out.println("🎲 Generated IV: " + Base64.getEncoder().encodeToString(iv));
        
        // Convert plaintext to bytes
        byte[] plaintextBytes = plaintext.getBytes();
        
        // IMPROVEMENT #2: XOR plaintext with IV before encryption
        byte[] xored = xorWithIV(plaintextBytes, iv);
        
        byte[] block = xored;

        // IMPROVEMENT #3: Encrypt through 10 rounds instead of 3
        for (int round = 1; round <= ROUNDS; round++) {
            System.out.println("---- Round " + round + " ----");
            block = PerRoundLogic.transform(block, round, this.key128Bit);
            System.out.println("Intermediate Ciphertext (Base64): " + Base64.getEncoder().encodeToString(block));
        }

        // IMPROVEMENT #2: Prepend IV to ciphertext (IV||ciphertext)
        byte[] result = new byte[IV_SIZE + block.length];
        System.arraycopy(iv, 0, result, 0, IV_SIZE);
        System.arraycopy(block, 0, result, IV_SIZE, block.length);
        
        String encrypted = Base64.getEncoder().encodeToString(result);
        System.out.println("🔒 Encrypted Output: " + encrypted);
        return encrypted;
    }

    public String decrypt(String ciphertext) {
        System.out.println("\n🔓 Decrypting with key: " + this.key128Bit);
        
        // Decode from Base64
        byte[] data = Base64.getDecoder().decode(ciphertext);
        
        // IMPROVEMENT #2: Extract IV from beginning of ciphertext
        byte[] iv = new byte[IV_SIZE];
        System.arraycopy(data, 0, iv, 0, IV_SIZE);
        System.out.println("🎲 Extracted IV: " + Base64.getEncoder().encodeToString(iv));
        
        // Extract actual ciphertext
        byte[] actualCiphertext = new byte[data.length - IV_SIZE];
        System.arraycopy(data, IV_SIZE, actualCiphertext, 0, actualCiphertext.length);
        
        byte[] block = actualCiphertext;

        // IMPROVEMENT #3: Decrypt through 10 rounds in reverse
        for (int round = ROUNDS; round >= 1; round--) {
            System.out.println("---- Round " + round + " (Reverse) ----");
            block = PerRoundLogic.reverseTransform(block, round, this.key128Bit);
        }

        // IMPROVEMENT #2: XOR with IV to get original plaintext
        byte[] plaintext = xorWithIV(block, iv);
        
        String decrypted = new String(plaintext);
        System.out.println("🔓 Decrypted Output: " + decrypted);
        return decrypted;
    }
    
    // IMPROVEMENT #2: Generate cryptographically secure random IV
    private byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        return iv;
    }
    
    // IMPROVEMENT #2: XOR data with IV (repeating IV if needed)
    private byte[] xorWithIV(byte[] data, byte[] iv) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ iv[i % iv.length]);
        }
        return result;
    }
}