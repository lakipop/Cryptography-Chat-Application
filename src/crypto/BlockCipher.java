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
    
    // File transfer mode: minimal logging for performance
    private static boolean fileTransferMode = false;
    
    // Helper method for conditional logging
    private static void log(String message) {
        if (VERBOSE_LOGGING && !fileTransferMode) {
            System.out.println(message);
        }
    }
    
    // Minimal log that works even in file transfer mode
    private static void minimalLog(String message) {
        if (VERBOSE_LOGGING) {
            System.out.println(message);
        }
    }
    
    // Enable/disable file transfer mode
    public static void setFileTransferMode(boolean enabled) {
        fileTransferMode = enabled;
        if (enabled) {
            minimalLog("[PERFORMANCE MODE] Detailed encryption logs minimized for file transfer");
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
        // Minimal logging for file transfer mode
        if (fileTransferMode) {
            minimalLog("[ENCRYPT] " + plaintext.length() + " bytes → IV gen → Pre-whiten → 10 rounds → IV embed → Base64");
            byte[] plaintextBytes = plaintext.getBytes();
            byte[] iv = generateIV();
            byte[] xored = xorWithIV(plaintextBytes, iv);
            byte[] block = xored;
            
            for (int round = 1; round <= ROUNDS; round++) {
                block = PerRoundLogic.transform(block, round, this.key128Bit);
                block = PerRoundLogic.splitAndMix(block, round, this.key128Bit);
            }
            
            byte[] finalCiphertext = embedIVMultiPosition(iv, block);
            String encrypted = Base64.getEncoder().encodeToString(finalCiphertext);
            return encrypted;
        }
        
        // Full educational logging for text messages
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
        
        log("  Step 4: IV Embedding (dual strategy)");
        byte[] finalCiphertext = embedIVMultiPosition(iv, block);
        
        String encrypted = Base64.getEncoder().encodeToString(finalCiphertext);
        log("\n  Final Result: " + finalCiphertext.length + " bytes -> " + encrypted.length() + " chars (Base64)");
        log("    Base64: " + encrypted);
        log("[ENCRYPTION COMPLETE]\n");
        return encrypted;
    }

    /**
     * Decrypt ciphertext - reverses all encryption operations
     */
    public String decrypt(String ciphertext) {
        // Minimal logging for file transfer mode
        if (fileTransferMode) {
            minimalLog("[DECRYPT] " + ciphertext.length() + " chars → Base64 decode → IV extract → 10 rounds reverse → Post-whiten");
            byte[] data = Base64.getDecoder().decode(ciphertext);
            byte[][] extracted = extractIVMultiPosition(data);
            byte[] iv = extracted[0];
            byte[] actualCiphertext = extracted[1];
            byte[] block = actualCiphertext;
            
            for (int round = ROUNDS; round >= 1; round--) {
                block = PerRoundLogic.unsplitAndUnmix(block, round, this.key128Bit);
                block = PerRoundLogic.reverseTransform(block, round, this.key128Bit);
            }
            
            byte[] plaintext = xorWithIV(block, iv);
            String decrypted = new String(plaintext);
            return decrypted;
        }
        
        // Full educational logging for text messages
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
     * ENHANCED: Multi-position IV embedding using BOTH XOR and INSERT strategies
     * 
     * Strategy 1: XOR IV at strategic positions (obfuscation)
     * Strategy 2: Break IV into pieces and INSERT at different positions (diffusion)
     * 
     * This dual-layer approach makes the IV extremely difficult to locate
     */
    private byte[] embedIVMultiPosition(byte[] iv, byte[] ciphertext) {
        log("\nIV Embedding Process (Dual Strategy):");
        log("  Original IV (16 bytes): " + bytesToHex(iv));
        log("  Ciphertext BEFORE embedding: " + bytesToHex(ciphertext));
        
        // STRATEGY 1: XOR IV at positions for obfuscation
        byte[] xoredCipher = Arrays.copyOf(ciphertext, ciphertext.length);
        int[] xorPositions = calculateIVPositions(ciphertext.length);
        
        log("\n  Strategy 1: XOR IV at " + xorPositions.length + " positions:");
        for (int i = 0; i < xorPositions.length; i++) {
            int pos = xorPositions[i];
            double percentage = (pos * 100.0 / ciphertext.length);
            log("    XOR Position " + (i+1) + ": Byte offset " + pos + " (" + String.format("%.1f%%", percentage) + ")");
            
            // XOR IV bytes at this position
            for (int j = 0; j < IV_SIZE && (pos + j) < xoredCipher.length; j++) {
                xoredCipher[pos + j] ^= iv[j];
            }
        }
        log("  After XOR: " + bytesToHex(xoredCipher));
        
        // STRATEGY 2: Break IV into pieces and INSERT at different positions
        int numInsertPositions = Math.min(4, Math.max(2, xoredCipher.length / 4));
        int ivChunkSize = IV_SIZE / numInsertPositions;
        
        int[] insertPositions = new int[numInsertPositions];
        int keySeed = 0;
        for (int i = 0; i < this.key128Bit.length(); i++) {
            keySeed += this.key128Bit.charAt(i) * (i + 1);
        }
        
        long hash = keySeed + 12345; // Different seed than XOR positions
        for (int i = 0; i < numInsertPositions; i++) {
            hash ^= (hash << 13);
            hash ^= (hash >>> 17);
            hash ^= (hash << 5);
            hash += i * 2654435761L;
            int position = (int)(Math.abs(hash) % (xoredCipher.length + 1));
            insertPositions[i] = position;
        }
        java.util.Arrays.sort(insertPositions);
        
        log("\n  Strategy 2: Break IV into " + numInsertPositions + " chunks and INSERT:");
        
        // Show how IV is broken into pieces with positions
        StringBuilder ivBreakdown = new StringBuilder("    IV breakdown: ");
        int ivPos = 0;
        for (int i = 0; i < numInsertPositions; i++) {
            int chunkSize = (i == numInsertPositions - 1) ? (IV_SIZE - ivPos) : ivChunkSize;
            double percentage = (insertPositions[i] * 100.0 / xoredCipher.length);
            ivBreakdown.append("Chunk").append(i+1).append("[").append(bytesToHex(Arrays.copyOfRange(iv, ivPos, ivPos + chunkSize)))
                       .append("] at ").append(String.format("%.1f%%", percentage)).append(" ");
            ivPos += chunkSize;
        }
        log(ivBreakdown.toString());
        
        // Build result by inserting IV chunks at calculated positions into XORed ciphertext
        byte[] result = new byte[xoredCipher.length + IV_SIZE];
        int srcPos = 0;  // Position in XORed ciphertext
        int destPos = 0; // Position in result
        ivPos = 0;       // Position in IV (reset)
        
        log("    Combining ciphertext + IV chunks:");
        
        for (int i = 0; i < numInsertPositions; i++) {
            // Copy ciphertext up to insertion point
            int copyLen = insertPositions[i] - srcPos;
            if (copyLen > 0) {
                double cipherStartPct = (srcPos * 100.0 / xoredCipher.length);
                double cipherEndPct = ((srcPos + copyLen) * 100.0 / xoredCipher.length);
                log("      [Cipher:" + bytesToHex(Arrays.copyOfRange(xoredCipher, srcPos, srcPos + copyLen)) + 
                    "] (" + String.format("%.1f%% - %.1f%%", cipherStartPct, cipherEndPct) + ")");
                System.arraycopy(xoredCipher, srcPos, result, destPos, copyLen);
                srcPos += copyLen;
                destPos += copyLen;
            }
            
            // Insert IV chunk
            int chunkSize = (i == numInsertPositions - 1) ? (IV_SIZE - ivPos) : ivChunkSize;
            double insertPct = (insertPositions[i] * 100.0 / xoredCipher.length);
            log("      [IV-Chunk" + (i+1) + ":" + bytesToHex(Arrays.copyOfRange(iv, ivPos, ivPos + chunkSize)) + 
                "] <- inserted at position " + insertPositions[i] + " (" + String.format("%.1f%%", insertPct) + ")");
            System.arraycopy(iv, ivPos, result, destPos, chunkSize);
            
            ivPos += chunkSize;
            destPos += chunkSize;
        }
        
        // Copy remaining ciphertext
        if (srcPos < xoredCipher.length) {
            double cipherStartPct = (srcPos * 100.0 / xoredCipher.length);
            log("      [Cipher:" + bytesToHex(Arrays.copyOfRange(xoredCipher, srcPos, xoredCipher.length)) + 
                "] (" + String.format("%.1f%% - 100.0%%", cipherStartPct) + ")");
            System.arraycopy(xoredCipher, srcPos, result, destPos, xoredCipher.length - srcPos);
        }
        
        log("\n  Final embedded result: " + bytesToHex(result));
        log("  Total size: " + result.length + " bytes (cipher " + ciphertext.length + " + IV " + IV_SIZE + ")");
        return result;
    }

    /**
     * ENHANCED (Reverse): Extract IV from multiple positions - reverses BOTH strategies
     */
    private byte[][] extractIVMultiPosition(byte[] data) {
        log("\nIV Extraction Process (Reverse Dual Strategy):");
        log("  Input data: " + bytesToHex(data));
        
        // STRATEGY 2 (Reverse): First, extract inserted IV chunks
        // Calculate how many insert positions were used based on original cipher length
        int originalCiphertextLength = data.length - IV_SIZE;
        int numInsertPositions = Math.min(4, Math.max(2, originalCiphertextLength / 4));
        int ivChunkSize = IV_SIZE / numInsertPositions;
        
        // Recalculate INSERT positions (same algorithm as encryption)
        int keySeed = 0;
        for (int i = 0; i < this.key128Bit.length(); i++) {
            keySeed += this.key128Bit.charAt(i) * (i + 1);
        }
        
        long hash = keySeed + 12345; // Same different seed as encryption
        int[] insertPositions = new int[numInsertPositions];
        for (int i = 0; i < numInsertPositions; i++) {
            hash ^= (hash << 13);
            hash ^= (hash >>> 17);
            hash ^= (hash << 5);
            hash += i * 2654435761L;
            int position = (int)(Math.abs(hash) % (originalCiphertextLength + 1));
            insertPositions[i] = position;
        }
        java.util.Arrays.sort(insertPositions);
        
        log("\n  Strategy 2 (Reverse): Extracting IV from " + numInsertPositions + " INSERT positions:");
        
        // Extract IV chunks and rebuild XORed ciphertext
        byte[] iv = new byte[IV_SIZE];
        byte[] xoredCipher = new byte[originalCiphertextLength];
        
        int srcPos = 0;     // Position in embedded data
        int cipherPos = 0;  // Position in reconstructed ciphertext
        int ivPos = 0;      // Position in reconstructed IV
        
        for (int i = 0; i < numInsertPositions; i++) {
            // Copy ciphertext up to IV chunk position
            int copyLen = insertPositions[i] - cipherPos;
            if (copyLen > 0) {
                System.arraycopy(data, srcPos, xoredCipher, cipherPos, copyLen);
                srcPos += copyLen;
                cipherPos += copyLen;
            }
            
            // Extract IV chunk
            int chunkSize = (i == numInsertPositions - 1) ? (IV_SIZE - ivPos) : ivChunkSize;
            System.arraycopy(data, srcPos, iv, ivPos, chunkSize);
            
            log("    Extract IV[" + ivPos + ".." + (ivPos+chunkSize-1) + "] from position " + insertPositions[i]);
            
            ivPos += chunkSize;
            srcPos += chunkSize;
        }
        
        // Copy remaining ciphertext
        if (srcPos < data.length) {
            System.arraycopy(data, srcPos, xoredCipher, cipherPos, data.length - srcPos);
        }
        
        log("  Extracted IV: " + bytesToHex(iv));
        log("  XORed ciphertext (after extraction): " + bytesToHex(xoredCipher));
        
        // STRATEGY 1 (Reverse): Remove XOR from the ciphertext
        int[] xorPositions = calculateIVPositions(xoredCipher.length);
        
        log("\n  Strategy 1 (Reverse): Removing XOR from " + xorPositions.length + " positions:");
        for (int i = 0; i < xorPositions.length; i++) {
            int pos = xorPositions[i];
            log("    Reverse XOR at position " + pos);
            
            // XOR again to reverse (XOR is its own inverse)
            for (int j = 0; j < IV_SIZE && (pos + j) < xoredCipher.length; j++) {
                xoredCipher[pos + j] ^= iv[j];
            }
        }
        
        log("  Final ciphertext (after XOR removal): " + bytesToHex(xoredCipher));
        log("  Extraction complete!\n");
        
        return new byte[][] { iv, xoredCipher };
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
