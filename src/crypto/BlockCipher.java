package crypto;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Arrays;

public class BlockCipher {
    private String key128Bit;
    
    private static final int ROUNDS = 10;
    private static final int IV_SIZE = 16; // 128-bit IV

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
        System.out.println("\n");
        System.out.println("============================================================");
        System.out.println("              ENCRYPTION PROCESS START                      ");
        System.out.println("============================================================");
        System.out.println("Algorithm: 10-Round Block Cipher with IV");
        System.out.println("Key (128-bit): " + this.key128Bit);
        System.out.println("------------------------------------------------------------\n");
        
        // ====== STEP 1: INITIALIZATION ======
        System.out.println("STEP 1: INITIALIZATION");
        System.out.println("------------------------------------------------------------");
        System.out.println("Input plaintext: \"" + plaintext + "\"");
        
        byte[] plaintextBytes = plaintext.getBytes();
        System.out.println("Plaintext size:  " + plaintextBytes.length + " bytes");
        System.out.println("Plaintext (HEX): " + bytesToHex(plaintextBytes));
        System.out.println("Plaintext (DEC): " + bytesToDecimal(plaintextBytes));
        
        // Generate IV
        byte[] iv = generateIV();
        System.out.println("\nGenerated IV (Initialization Vector):");
        System.out.println("  Purpose: Ensures same plaintext produces different ciphertext");
        System.out.println("  IV (Base64): " + Base64.getEncoder().encodeToString(iv));
        System.out.println("  IV (HEX):    " + bytesToHex(iv));
        System.out.println("  IV size:     " + iv.length + " bytes (128-bit)\n");
        
        // ====== STEP 2: PRE-WHITENING ======
        System.out.println("STEP 2: PRE-WHITENING (XOR with IV)");
        System.out.println("------------------------------------------------------------");
        System.out.println("Operation: Plaintext XOR IV");
        System.out.println("Purpose: Hide plaintext patterns before main encryption");
        
        byte[] xored = xorWithIV(plaintextBytes, iv);
        System.out.println("\nBefore XOR (Plaintext): " + bytesToHex(plaintextBytes));
        System.out.println("XOR Key (IV):           " + bytesToHex(iv).substring(0, Math.min(plaintextBytes.length * 2, bytesToHex(iv).length())));
        System.out.println("After XOR (Whitened):   " + bytesToHex(xored));
        System.out.println("Status: Pre-whitening complete - data is now randomized\n");
        
        byte[] block = xored;

        // ====== STEP 3: 10-ROUND TRANSFORMATION ======
        System.out.println("STEP 3: MULTI-ROUND TRANSFORMATION");
        System.out.println("------------------------------------------------------------");
        System.out.println("Total Rounds: " + ROUNDS);
        System.out.println("Each round applies: (1) Transform (2) Split & Mix");
        System.out.println("============================================================\n");
        
        for (int round = 1; round <= ROUNDS; round++) {
            System.out.println("  +-------------------------------------------------------+");
            System.out.println("  |  ROUND " + String.format("%2d", round) + " / " + ROUNDS + "                                         |");
            System.out.println("  +-------------------------------------------------------+");
            System.out.println("  Input Data:  " + formatHex(block, 48));
            System.out.println("  Data Size:   " + block.length + " bytes");
            
            // Sub-step 1: Transform
            System.out.println("\n  >> Sub-Step 1: Byte Transformation");
            System.out.println("     - Substituting bytes using key-dependent S-box");
            System.out.println("     - Adding round-specific key material");
            block = PerRoundLogic.transform(block, round, this.key128Bit);
            System.out.println("     After Transform: " + formatHex(block, 48));
            
            // Sub-step 2: Split and Mix
            System.out.println("\n  >> Sub-Step 2: Split & Mix (Diffusion)");
            System.out.println("     - Splitting block into chunks");
            System.out.println("     - Mixing positions for avalanche effect");
            block = PerRoundLogic.splitAndMix(block, round, this.key128Bit);
            System.out.println("     After Split&Mix: " + formatHex(block, 48));
            
            System.out.println("\n  Output Data: " + formatHex(block, 48));
            System.out.println("  Round " + round + " Status: COMPLETE");
            System.out.println("  +-------------------------------------------------------+\n");
        }
        
        System.out.println("============================================================");
        System.out.println("All 10 rounds complete!");
        System.out.println("Final encrypted block (before IV embedding):");
        System.out.println("  Full HEX: " + bytesToHex(block));
        System.out.println("  Size:     " + block.length + " bytes\n");

        // ====== STEP 4: IV EMBEDDING ======
        System.out.println("STEP 4: IV EMBEDDING (Multi-Position XOR)");
        System.out.println("------------------------------------------------------------");
        System.out.println("Purpose: Hide IV at multiple positions within ciphertext");
        System.out.println("Method:  XOR IV at positions 0%, 25%, 50%, 75% + prepend IV");
        
        byte[] finalCiphertext = embedIVMultiPosition(iv, block);
        
        String encrypted = Base64.getEncoder().encodeToString(finalCiphertext);
        System.out.println("\nFinal encrypted data:");
        System.out.println("  Raw bytes:   " + finalCiphertext.length + " bytes");
        System.out.println("  Base64:      " + encrypted);
        System.out.println("  Base64 len:  " + encrypted.length() + " characters");
        System.out.println("\n============================================================");
        System.out.println("              ENCRYPTION PROCESS COMPLETE                   ");
        System.out.println("============================================================\n");
        return encrypted;
    }

    /**
     * Decrypt ciphertext - reverses all encryption operations
     */
    public String decrypt(String ciphertext) {
        // ============================================================
        //              DECRYPTION ALGORITHM - FULL TRACE
        // ============================================================
        System.out.println("\n");
        System.out.println("============================================================");
        System.out.println("              DECRYPTION PROCESS START                      ");
        System.out.println("============================================================");
        System.out.println("Algorithm: Reverse 10-Round Block Cipher with IV");
        System.out.println("Key (128-bit): " + this.key128Bit);
        System.out.println("------------------------------------------------------------\n");
        
        System.out.println("Input ciphertext (Base64): " + ciphertext);
        System.out.println("Ciphertext length: " + ciphertext.length() + " characters");
        
        // Decode from Base64
        byte[] data = Base64.getDecoder().decode(ciphertext);
        System.out.println("Decoded to binary: " + data.length + " bytes\n");
        
        // ====== STEP 1: IV EXTRACTION ======
        System.out.println("STEP 1: IV EXTRACTION (Multi-Position Reverse XOR)");
        System.out.println("------------------------------------------------------------");
        System.out.println("Extracting IV from embedded positions...");
        
        byte[][] extracted = extractIVMultiPosition(data);
        byte[] iv = extracted[0];
        byte[] actualCiphertext = extracted[1];
        
        System.out.println("\nExtracted IV:");
        System.out.println("  IV (Base64): " + Base64.getEncoder().encodeToString(iv));
        System.out.println("  IV (HEX):    " + bytesToHex(iv));
        System.out.println("  IV size:     " + iv.length + " bytes");
        
        System.out.println("\nCiphertext block (after IV removal):");
        System.out.println("  Full HEX: " + bytesToHex(actualCiphertext));
        System.out.println("  Size:     " + actualCiphertext.length + " bytes\n");
        
        byte[] block = actualCiphertext;

        // ====== STEP 2: REVERSE 10-ROUND TRANSFORMATION ======
        System.out.println("STEP 2: REVERSE MULTI-ROUND TRANSFORMATION");
        System.out.println("------------------------------------------------------------");
        System.out.println("Total Rounds: " + ROUNDS + " (applying in reverse order)");
        System.out.println("Each round reverses: (1) Unsplit & Unmix (2) Reverse Transform");
        System.out.println("============================================================\n");
        
        for (int round = ROUNDS; round >= 1; round--) {
            System.out.println("  +-------------------------------------------------------+");
            System.out.println("  |  ROUND " + String.format("%2d", round) + " REVERSE (Unwinding Round " + round + ")              |");
            System.out.println("  +-------------------------------------------------------+");
            System.out.println("  Input Data:  " + formatHex(block, 48));
            System.out.println("  Data Size:   " + block.length + " bytes");
            
            // Sub-step 1: Unsplit and Unmix
            System.out.println("\n  >> Sub-Step 1: Unsplit & Unmix (Reverse Diffusion)");
            System.out.println("     - Unmixing positions");
            System.out.println("     - Rejoining split chunks");
            block = PerRoundLogic.unsplitAndUnmix(block, round, this.key128Bit);
            System.out.println("     After Unsplit&Unmix: " + formatHex(block, 48));
            
            // Sub-step 2: Reverse Transform
            System.out.println("\n  >> Sub-Step 2: Reverse Byte Transformation");
            System.out.println("     - Removing round-specific key material");
            System.out.println("     - Applying inverse S-box substitution");
            block = PerRoundLogic.reverseTransform(block, round, this.key128Bit);
            System.out.println("     After Reverse Transform: " + formatHex(block, 48));
            
            System.out.println("\n  Output Data: " + formatHex(block, 48));
            System.out.println("  Round " + round + " Reverse Status: COMPLETE");
            System.out.println("  +-------------------------------------------------------+\n");
        }
        
        System.out.println("============================================================");
        System.out.println("All rounds reversed!");
        System.out.println("Data after reversing all transformations:");
        System.out.println("  Full HEX: " + bytesToHex(block));
        System.out.println("  Size:     " + block.length + " bytes\n");

        // ====== STEP 3: POST-WHITENING ======
        System.out.println("STEP 3: POST-WHITENING (Reverse XOR with IV)");
        System.out.println("------------------------------------------------------------");
        System.out.println("Operation: Whitened_Data XOR IV = Original_Plaintext");
        System.out.println("Purpose: Recover original plaintext from whitened data");
        
        byte[] plaintext = xorWithIV(block, iv);
        
        System.out.println("\nBefore XOR (Whitened):     " + bytesToHex(block));
        System.out.println("XOR Key (IV):              " + bytesToHex(iv).substring(0, Math.min(block.length * 2, bytesToHex(iv).length())));
        System.out.println("After XOR (Plaintext HEX): " + bytesToHex(plaintext));
        
        String decrypted = new String(plaintext);
        System.out.println("\nRecovered plaintext: \"" + decrypted + "\"");
        System.out.println("Plaintext size: " + plaintext.length + " bytes");
        
        System.out.println("\n============================================================");
        System.out.println("              DECRYPTION PROCESS COMPLETE                   ");
        System.out.println("============================================================\n");
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
        
        System.out.println("\nEmbedding IV at multiple strategic positions:");
        System.out.println("  Total positions: " + positions.length);
        
        for (int i = 0; i < positions.length; i++) {
            int pos = positions[i];
            double percentage = (pos * 100.0 / ciphertext.length);
            System.out.println("  Position " + (i+1) + ": Byte offset " + pos + " (" + String.format("%.1f%%", percentage) + " of ciphertext)");
            
            // XOR IV bytes at this position (cycle through IV if needed)
            for (int j = 0; j < IV_SIZE && (pos + j) < modifiedCipher.length; j++) {
                byte before = modifiedCipher[pos + j];
                modifiedCipher[pos + j] ^= iv[j];
                byte after = modifiedCipher[pos + j];
                if (i == 0 && j < 4) { // Show first few XOR operations as example
                    System.out.println("      Byte " + (pos + j) + ": 0x" + String.format("%02X", before) + 
                                     " XOR 0x" + String.format("%02X", iv[j]) + 
                                     " = 0x" + String.format("%02X", after));
                }
            }
        }
        
        // Prepend IV for extraction reference
        byte[] result = new byte[IV_SIZE + modifiedCipher.length];
        System.arraycopy(iv, 0, result, 0, IV_SIZE);
        System.arraycopy(modifiedCipher, 0, result, IV_SIZE, modifiedCipher.length);
        
        System.out.println("\nIV embedding complete:");
        System.out.println("  IV prepended at start: " + IV_SIZE + " bytes");
        System.out.println("  IV XORed at " + positions.length + " positions inside ciphertext");
        System.out.println("  Total output size: " + result.length + " bytes");
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
        
        System.out.println("Extracting IV from multiple positions (reverse operation):");
        System.out.println("  Prepended IV extracted: " + IV_SIZE + " bytes");
        System.out.println("  Total XOR positions: " + positions.length);
        
        for (int i = 0; i < positions.length; i++) {
            int pos = positions[i];
            System.out.println("  Position " + (i+1) + ": Byte offset " + pos + " (reversing XOR)");
            
            // Reverse XOR operation (XOR is its own inverse)
            for (int j = 0; j < IV_SIZE && (pos + j) < modifiedCipher.length; j++) {
                modifiedCipher[pos + j] ^= iv[j];
            }
        }
        
        System.out.println("\nIV extraction complete:");
        System.out.println("  Original ciphertext restored: " + modifiedCipher.length + " bytes");
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
