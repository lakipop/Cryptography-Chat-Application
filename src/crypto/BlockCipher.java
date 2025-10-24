package crypto;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;

public class BlockCipher {
    private String key128Bit;
    
    private static final int ROUNDS = 10;
    private static final int IV_SIZE = 16; // 128-bit IV
    
    // Set to true to enable educational logging (optimized for performance)
    public static final boolean VERBOSE_LOGGING = true;  // Public so PerRoundLogic can access
    
    // Helper method for conditional logging
    private static void log(String message) {
        if (VERBOSE_LOGGING) {
            System.out.println(message);
        }
    }

    public BlockCipher(String key128Bit) {
        if (key128Bit == null || key128Bit.length() != 32) { // 32 hex chars = 128 bits
            throw new IllegalArgumentException("Key must be a 32-character hex string (128-bit).");
        }
        this.key128Bit = key128Bit;
    }

    /**
     * Encrypt plaintext using 10-round block cipher with IV and enhanced diffusion
     * 
     * Process:
     * 1. Generate random 128-bit IV
     * 2. XOR plaintext with IV (pre-whitening)
     * 3. Apply 10 rounds of: transform → split & mix
     * 4. Hide IV at multiple positions using XOR (multi-position embedding)
     * 5. Base64 encode for transmission
     */
    public String encrypt(String plaintext) {
        // Concise educational logging
        log("\n[ENCRYPTION START] " + plaintext.length() + " bytes");
        
        byte[] plaintextBytes = plaintext.getBytes();
        
        // Generate IV and apply pre-whitening
        byte[] iv = generateIV();
        log("  Step 1: Generated IV (full 128-bit):");
        log("    IV: " + bytesToHex(iv));
        
        byte[] xored = xorWithIV(plaintextBytes, iv);
        log("  Step 2: Pre-whitening (XOR with IV)");
        log("    After whitening: " + bytesToHex(xored));
        
        byte[] block = xored;

        // 10-Round transformation with smart logging
        log("  Step 3: 10-Round Transformation (Transform + Shuffle):");
        
        for (int round = 1; round <= ROUNDS; round++) {
            // Apply key-dependent transform FIRST
            block = PerRoundLogic.transform(block, round, this.key128Bit);
            
            // Then apply chunk shuffle
            block = PerRoundLogic.splitAndMix(block, round, this.key128Bit);
            
            // Log first 2 rounds, last 2 rounds with full ciphertext
            if (round <= 2 || round >= ROUNDS - 1) {
                log("    [Round " + round + "] After transform+shuffle: " + bytesToHex(block));
            } else if (round == 3) {
                log("    ... (Rounds 3-" + (ROUNDS-2) + " processing) ...");
            }
        }
        
        log("  Step 4: IV Embedding (multi-position XOR)");
        byte[] finalCiphertext = embedIVMultiPosition(iv, block);
        log("    After IV embedding: " + bytesToHex(finalCiphertext));
        
        String encrypted = Base64.getEncoder().encodeToString(finalCiphertext);
        log("  Result: " + finalCiphertext.length + " bytes -> " + encrypted.length() + " chars (Base64)");
        log("    Base64: " + encrypted);
        log("[ENCRYPTION COMPLETE]\n");
        return encrypted;
    }

    /**
     * Decrypt ciphertext - reverses all encryption operations
     */
    public String decrypt(String ciphertext) {
        log("\n[DECRYPTION START] " + ciphertext.length() + " chars (Base64)");
        
        // Decode from Base64
        byte[] data = Base64.getDecoder().decode(ciphertext);
        log("  Step 1: Decoded to " + data.length + " bytes");
        log("    Base64 input: " + ciphertext);
        
        // Extract IV
        byte[][] extracted = extractIVMultiPosition(data);
        byte[] iv = extracted[0];
        byte[] actualCiphertext = extracted[1];
        log("  Step 2: Extracted IV (" + bytesToHex(iv).substring(0, 16) + "...)");
        log("    After IV extraction: " + bytesToHex(actualCiphertext));
        
        byte[] block = actualCiphertext;

        // Reverse 10-Round transformation with smart logging
        log("  Step 3: Reverse 10-Round Transformation:");
        
        for (int round = ROUNDS; round >= 1; round--) {
            block = PerRoundLogic.unsplitAndUnmix(block, round, this.key128Bit);
            block = PerRoundLogic.reverseTransform(block, round, this.key128Bit);
            
            // Log first 2 rounds (in reverse), last 2 rounds with full ciphertext
            if (round >= ROUNDS - 1 || round <= 2) {
                log("    Round " + round + " (reverse): " + bytesToHex(block));
            } else if (round == ROUNDS - 2) {
                log("    ... (Rounds " + (ROUNDS-2) + "-3 processing in reverse) ...");
            }
        }
        
        log("  Step 4: Post-whitening (XOR with IV)");
        byte[] plaintext = xorWithIV(block, iv);
        log("    After whitening: " + bytesToHex(plaintext));
        
        String decrypted = new String(plaintext);
        log("  Result: \"" + (decrypted.length() > 50 ? decrypted.substring(0, 50) + "..." : decrypted) + "\" (" + plaintext.length + " bytes)");
        log("[DECRYPTION COMPLETE]\n");
        return decrypted;
    }
    
    /**
     * Generate cryptographically secure random IV
     */
    private byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        return iv;
    }
    
    /**
     * XOR data with IV (repeating IV if needed)
     */
    private byte[] xorWithIV(byte[] data, byte[] iv) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ iv[i % iv.length]);
        }
        return result;
    }

    /**
     * ENHANCEMENT #2: Multi-position IV embedding using XOR
     * 
     * Strategy: XOR the IV at multiple strategic positions within the ciphertext
     * This makes it harder to identify where the IV is located
     * 
     * Positions: Start, 25%, 50%, 75% of ciphertext length
     * We also prepend the IV at the beginning for extraction reference
     * 
     * Format: [IV_16bytes][modified_ciphertext_with_IV_XORed_at_positions]
     */
    private byte[] embedIVMultiPosition(byte[] iv, byte[] ciphertext) {
        byte[] modifiedCipher = Arrays.copyOf(ciphertext, ciphertext.length);
        
        log("\nIV Embedding Process:");
        log("  Ciphertext BEFORE IV embedding: " + bytesToHex(ciphertext));
        
        // Calculate strategic positions (4 positions spread across the ciphertext)
        int[] positions = calculateIVPositions(ciphertext.length);
        
        log("  Embedding IV at " + positions.length + " strategic positions:");
        
        for (int i = 0; i < positions.length; i++) {
            int pos = positions[i];
            double percentage = (pos * 100.0 / ciphertext.length);
            log("    Position " + (i+1) + ": Byte offset " + pos + " (" + String.format("%.1f%%", percentage) + ")");
            
            // XOR IV bytes at this position (cycle through IV if needed)
            for (int j = 0; j < IV_SIZE && (pos + j) < modifiedCipher.length; j++) {
                modifiedCipher[pos + j] ^= iv[j];
            }
        }
        
        log("  Ciphertext AFTER IV XOR: " + bytesToHex(modifiedCipher));
        
        // Prepend IV for extraction reference
        byte[] result = new byte[IV_SIZE + modifiedCipher.length];
        System.arraycopy(iv, 0, result, 0, IV_SIZE);
        System.arraycopy(modifiedCipher, 0, result, IV_SIZE, modifiedCipher.length);
        
        log("  Final result (IV prepended): " + bytesToHex(result));
        log("  Total output size: " + result.length + " bytes");
        return result;
    }

    /**
     * ENHANCEMENT #2 (Reverse): Extract IV from multiple positions
     */
    private byte[][] extractIVMultiPosition(byte[] data) {
        // Extract prepended IV
        byte[] iv = new byte[IV_SIZE];
        System.arraycopy(data, 0, iv, 0, IV_SIZE);
        
        // Extract modified ciphertext
        byte[] modifiedCipher = new byte[data.length - IV_SIZE];
        System.arraycopy(data, IV_SIZE, modifiedCipher, 0, modifiedCipher.length);
        
        // Calculate same positions as encryption
        int[] positions = calculateIVPositions(modifiedCipher.length);
        
        log("Extracting IV from multiple positions (reverse operation):");
        log("  Prepended IV extracted: " + IV_SIZE + " bytes");
        log("  Total XOR positions: " + positions.length);
        
        for (int i = 0; i < positions.length; i++) {
            int pos = positions[i];
            log("  Position " + (i+1) + ": Byte offset " + pos + " (reversing XOR)");
            
            // Reverse XOR operation (XOR is its own inverse)
            for (int j = 0; j < IV_SIZE && (pos + j) < modifiedCipher.length; j++) {
                modifiedCipher[pos + j] ^= iv[j];
            }
        }
        
        log("\nIV extraction complete:");
        log("  Original ciphertext restored: " + modifiedCipher.length + " bytes");
        return new byte[][] { iv, modifiedCipher };
    }

    /**
     * Calculate strategic positions for IV embedding using key-dependent unpredictable positions
     * Uses key to generate seemingly random but reproducible positions
     */
    private int[] calculateIVPositions(int dataLength) {
        if (dataLength <= 1) {
            return new int[] { 0 };
        }
        
        // Calculate key sum for seeding position generation
        int keySeed = 0;
        for (int i = 0; i < this.key128Bit.length(); i++) {
            keySeed += this.key128Bit.charAt(i) * (i + 1);
        }
        
        // Determine number of positions based on data size
        int numPositions;
        if (dataLength < 8) {
            numPositions = 2;  // Very small: 2 positions
        } else if (dataLength < 16) {
            numPositions = 3;  // Small: 3 positions
        } else if (dataLength < 64) {
            numPositions = 4;  // Medium: 4 positions
        } else {
            numPositions = 5;  // Large: 5 positions
        }
        
        int[] positions = new int[numPositions];
        
        // Start with key-based seed
        long hash = keySeed;
        
        // Use XOR-based hash mixing for better distribution
        // This ensures positions are spread across the entire data range
        for (int i = 0; i < numPositions; i++) {
            // Hash mixing using XOR and bit shifts (Knuth multiplicative hash)
            hash ^= (hash << 13);
            hash ^= (hash >>> 17);
            hash ^= (hash << 5);
            hash += i * 2654435761L;  // Large prime for position variation
            
            // Map to position: take absolute value, modulo dataLength
            int position = (int)(Math.abs(hash) % dataLength);
            
            // Ensure position is within bounds
            position = Math.max(0, Math.min(dataLength - 1, position));
            
            positions[i] = position;
        }
        
        // Sort positions to ensure they're in order (for consistent XOR)
        java.util.Arrays.sort(positions);
        
        // Remove duplicates (in case positions collide on small data)
        int uniqueCount = 0;
        for (int i = 0; i < positions.length; i++) {
            if (i == 0 || positions[i] != positions[i-1]) {
                positions[uniqueCount++] = positions[i];
            }
        }
        
        // Return only unique positions
        return java.util.Arrays.copyOf(positions, uniqueCount);
    }
    
    /**
     * Helper method to convert byte array to hex string for logging
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02X", b));
        }
        return hex.toString();
    }
}
