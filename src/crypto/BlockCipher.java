package crypto;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;

public class BlockCipher {
    private String key128Bit;
    
    private static final int ROUNDS = 10;
    private static final int IV_SIZE = 16; // 128-bit IV
    
    // Set to false to disable verbose logging for production use
    private static final boolean VERBOSE_LOGGING = false;
    
    // Helper method for conditional logging
    private static void log(String message) {
        if (VERBOSE_LOGGING) {
            log(message);
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
        // ============================================================
        //              ENCRYPTION ALGORITHM - FULL TRACE
        // ============================================================
        log("\n");
        log("============================================================");
        log("              ENCRYPTION PROCESS START                      ");
        log("============================================================");
        log("Algorithm: 10-Round Block Cipher with IV");
        log("Key (128-bit): " + this.key128Bit);
        log("------------------------------------------------------------\n");
        
        // ====== STEP 1: INITIALIZATION ======
        log("STEP 1: INITIALIZATION");
        log("------------------------------------------------------------");
        log("Input plaintext: \"" + plaintext + "\"");
        
        byte[] plaintextBytes = plaintext.getBytes();
        log("Plaintext size:  " + plaintextBytes.length + " bytes");
        log("Plaintext (HEX): " + bytesToHex(plaintextBytes));
        log("Plaintext (DEC): " + bytesToDecimal(plaintextBytes));
        
        // Generate IV
        byte[] iv = generateIV();
        log("\nGenerated IV (Initialization Vector):");
        log("  Purpose: Ensures same plaintext produces different ciphertext");
        log("  IV (Base64): " + Base64.getEncoder().encodeToString(iv));
        log("  IV (HEX):    " + bytesToHex(iv));
        log("  IV size:     " + iv.length + " bytes (128-bit)\n");
        
        // ====== STEP 2: PRE-WHITENING ======
        log("STEP 2: PRE-WHITENING (XOR with IV)");
        log("------------------------------------------------------------");
        log("Operation: Plaintext XOR IV");
        log("Purpose: Hide plaintext patterns before main encryption");
        
        byte[] xored = xorWithIV(plaintextBytes, iv);
        log("\nBefore XOR (Plaintext): " + bytesToHex(plaintextBytes));
        log("XOR Key (IV):           " + bytesToHex(iv).substring(0, Math.min(plaintextBytes.length * 2, bytesToHex(iv).length())));
        log("After XOR (Whitened):   " + bytesToHex(xored));
        log("Status: Pre-whitening complete - data is now randomized\n");
        
        byte[] block = xored;

        // ====== STEP 3: 10-ROUND TRANSFORMATION ======
        log("STEP 3: MULTI-ROUND TRANSFORMATION");
        log("------------------------------------------------------------");
        log("Total Rounds: " + ROUNDS);
        log("Each round applies: (1) Transform (2) Split & Mix");
        log("============================================================\n");
        
        for (int round = 1; round <= ROUNDS; round++) {
            log("  +-------------------------------------------------------+");
            log("  |  ROUND " + String.format("%2d", round) + " / " + ROUNDS + "                                         |");
            log("  +-------------------------------------------------------+");
            log("  Input Data:  " + formatHex(block, 48));
            log("  Data Size:   " + block.length + " bytes");
            
            // Sub-step 1: Transform
            log("\n  >> Sub-Step 1: Byte Transformation");
            log("     - Substituting bytes using key-dependent S-box");
            log("     - Adding round-specific key material");
            block = PerRoundLogic.transform(block, round, this.key128Bit);
            log("     After Transform: " + formatHex(block, 48));
            
            // Sub-step 2: Split and Mix
            log("\n  >> Sub-Step 2: Split & Mix (Diffusion)");
            log("     - Splitting block into chunks");
            log("     - Mixing positions for avalanche effect");
            block = PerRoundLogic.splitAndMix(block, round, this.key128Bit);
            log("     After Split&Mix: " + formatHex(block, 48));
            
            log("\n  Output Data: " + formatHex(block, 48));
            log("  Round " + round + " Status: COMPLETE");
            log("  +-------------------------------------------------------+\n");
        }
        
        log("============================================================");
        log("All 10 rounds complete!");
        log("Final encrypted block (before IV embedding):");
        log("  Full HEX: " + bytesToHex(block));
        log("  Size:     " + block.length + " bytes\n");

        // ====== STEP 4: IV EMBEDDING ======
        log("STEP 4: IV EMBEDDING (Multi-Position XOR)");
        log("------------------------------------------------------------");
        log("Purpose: Hide IV at multiple positions within ciphertext");
        log("Method:  XOR IV at positions 0%, 25%, 50%, 75% + prepend IV");
        
        byte[] finalCiphertext = embedIVMultiPosition(iv, block);
        
        String encrypted = Base64.getEncoder().encodeToString(finalCiphertext);
        log("\nFinal encrypted data:");
        log("  Raw bytes:   " + finalCiphertext.length + " bytes");
        log("  Base64:      " + encrypted);
        log("  Base64 len:  " + encrypted.length() + " characters");
        log("\n============================================================");
        log("              ENCRYPTION PROCESS COMPLETE                   ");
        log("============================================================\n");
        return encrypted;
    }

    /**
     * Decrypt ciphertext - reverses all encryption operations
     */
    public String decrypt(String ciphertext) {
        // ============================================================
        //              DECRYPTION ALGORITHM - FULL TRACE
        // ============================================================
        log("\n");
        log("============================================================");
        log("              DECRYPTION PROCESS START                      ");
        log("============================================================");
        log("Algorithm: Reverse 10-Round Block Cipher with IV");
        log("Key (128-bit): " + this.key128Bit);
        log("------------------------------------------------------------\n");
        
        log("Input ciphertext (Base64): " + ciphertext);
        log("Ciphertext length: " + ciphertext.length() + " characters");
        
        // Decode from Base64
        byte[] data = Base64.getDecoder().decode(ciphertext);
        log("Decoded to binary: " + data.length + " bytes\n");
        
        // ====== STEP 1: IV EXTRACTION ======
        log("STEP 1: IV EXTRACTION (Multi-Position Reverse XOR)");
        log("------------------------------------------------------------");
        log("Extracting IV from embedded positions...");
        
        byte[][] extracted = extractIVMultiPosition(data);
        byte[] iv = extracted[0];
        byte[] actualCiphertext = extracted[1];
        
        log("\nExtracted IV:");
        log("  IV (Base64): " + Base64.getEncoder().encodeToString(iv));
        log("  IV (HEX):    " + bytesToHex(iv));
        log("  IV size:     " + iv.length + " bytes");
        
        log("\nCiphertext block (after IV removal):");
        log("  Full HEX: " + bytesToHex(actualCiphertext));
        log("  Size:     " + actualCiphertext.length + " bytes\n");
        
        byte[] block = actualCiphertext;

        // ====== STEP 2: REVERSE 10-ROUND TRANSFORMATION ======
        log("STEP 2: REVERSE MULTI-ROUND TRANSFORMATION");
        log("------------------------------------------------------------");
        log("Total Rounds: " + ROUNDS + " (applying in reverse order)");
        log("Each round reverses: (1) Unsplit & Unmix (2) Reverse Transform");
        log("============================================================\n");
        
        for (int round = ROUNDS; round >= 1; round--) {
            log("  +-------------------------------------------------------+");
            log("  |  ROUND " + String.format("%2d", round) + " REVERSE (Unwinding Round " + round + ")              |");
            log("  +-------------------------------------------------------+");
            log("  Input Data:  " + formatHex(block, 48));
            log("  Data Size:   " + block.length + " bytes");
            
            // Sub-step 1: Unsplit and Unmix
            log("\n  >> Sub-Step 1: Unsplit & Unmix (Reverse Diffusion)");
            log("     - Unmixing positions");
            log("     - Rejoining split chunks");
            block = PerRoundLogic.unsplitAndUnmix(block, round, this.key128Bit);
            log("     After Unsplit&Unmix: " + formatHex(block, 48));
            
            // Sub-step 2: Reverse Transform
            log("\n  >> Sub-Step 2: Reverse Byte Transformation");
            log("     - Removing round-specific key material");
            log("     - Applying inverse S-box substitution");
            block = PerRoundLogic.reverseTransform(block, round, this.key128Bit);
            log("     After Reverse Transform: " + formatHex(block, 48));
            
            log("\n  Output Data: " + formatHex(block, 48));
            log("  Round " + round + " Reverse Status: COMPLETE");
            log("  +-------------------------------------------------------+\n");
        }
        
        log("============================================================");
        log("All rounds reversed!");
        log("Data after reversing all transformations:");
        log("  Full HEX: " + bytesToHex(block));
        log("  Size:     " + block.length + " bytes\n");

        // ====== STEP 3: POST-WHITENING ======
        log("STEP 3: POST-WHITENING (Reverse XOR with IV)");
        log("------------------------------------------------------------");
        log("Operation: Whitened_Data XOR IV = Original_Plaintext");
        log("Purpose: Recover original plaintext from whitened data");
        
        byte[] plaintext = xorWithIV(block, iv);
        
        log("\nBefore XOR (Whitened):     " + bytesToHex(block));
        log("XOR Key (IV):              " + bytesToHex(iv).substring(0, Math.min(block.length * 2, bytesToHex(iv).length())));
        log("After XOR (Plaintext HEX): " + bytesToHex(plaintext));
        
        String decrypted = new String(plaintext);
        log("\nRecovered plaintext: \"" + decrypted + "\"");
        log("Plaintext size: " + plaintext.length + " bytes");
        
        log("\n============================================================");
        log("              DECRYPTION PROCESS COMPLETE                   ");
        log("============================================================\n");
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
        
        // Calculate strategic positions (4 positions spread across the ciphertext)
        int[] positions = calculateIVPositions(ciphertext.length);
        
        log("\nEmbedding IV at multiple strategic positions:");
        log("  Total positions: " + positions.length);
        
        for (int i = 0; i < positions.length; i++) {
            int pos = positions[i];
            double percentage = (pos * 100.0 / ciphertext.length);
            log("  Position " + (i+1) + ": Byte offset " + pos + " (" + String.format("%.1f%%", percentage) + " of ciphertext)");
            
            // XOR IV bytes at this position (cycle through IV if needed)
            for (int j = 0; j < IV_SIZE && (pos + j) < modifiedCipher.length; j++) {
                byte before = modifiedCipher[pos + j];
                modifiedCipher[pos + j] ^= iv[j];
                byte after = modifiedCipher[pos + j];
                if (i == 0 && j < 4) { // Show first few XOR operations as example
                    log("      Byte " + (pos + j) + ": 0x" + String.format("%02X", before) + 
                                     " XOR 0x" + String.format("%02X", iv[j]) + 
                                     " = 0x" + String.format("%02X", after));
                }
            }
        }
        
        // Prepend IV for extraction reference
        byte[] result = new byte[IV_SIZE + modifiedCipher.length];
        System.arraycopy(iv, 0, result, 0, IV_SIZE);
        System.arraycopy(modifiedCipher, 0, result, IV_SIZE, modifiedCipher.length);
        
        log("\nIV embedding complete:");
        log("  IV prepended at start: " + IV_SIZE + " bytes");
        log("  IV XORed at " + positions.length + " positions inside ciphertext");
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
     * Calculate strategic positions for IV embedding
     * Returns 4 positions spread across the data: 0%, 25%, 50%, 75%
     */
    private int[] calculateIVPositions(int dataLength) {
        if (dataLength < IV_SIZE * 4) {
            // If data is too short, just use start position
            return new int[] { 0 };
        }
        
        return new int[] {
            0,                          // Start (0%)
            dataLength / 4,             // 25%
            dataLength / 2,             // 50%
            (dataLength * 3) / 4        // 75%
        };
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
    
    /**
     * Helper method to convert byte array to decimal string for logging
     */
    private static String bytesToDecimal(byte[] bytes) {
        StringBuilder dec = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) dec.append(" ");
            dec.append(String.format("%3d", bytes[i] & 0xFF));
        }
        return dec.toString();
    }
    
    /**
     * Helper method to format hex with maximum length (truncate if too long)
     */
    private static String formatHex(byte[] bytes, int maxChars) {
        String hex = bytesToHex(bytes);
        if (hex.length() <= maxChars) {
            return hex;
        } else {
            return hex.substring(0, maxChars) + "... (+" + (hex.length() - maxChars) + " more)";
        }
    }
}
