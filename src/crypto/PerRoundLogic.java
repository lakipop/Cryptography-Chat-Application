package crypto;

public class PerRoundLogic {

    private static final int MIN_BYTE = 0;
    private static final int MAX_BYTE = 255;
    private static final int RANGE = MAX_BYTE - MIN_BYTE + 1; // 256

    /**
     * Transform bytes with key-dependent shift (encryption direction)
     */
    public static byte[] transform(byte[] input, int round, String key) {
        byte[] result = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            int byteValue = input[i] & 0xFF; // Convert to unsigned 0-255
            int keyDigit = key.charAt((i + round) % key.length()) - '0'; // 0-9 from key
            int shift = 5 + (round * 3) + keyDigit; // Controlled shift
            int shiftedValue = (byteValue + shift) % RANGE;
            result[i] = (byte) shiftedValue;
        }
        // Log transform operation showing key-dependent shift
        if (BlockCipher.VERBOSE_LOGGING) {
            System.out.println("    Transform: Key-dependent shift applied (round=" + round + ", keyDigit=" + (key.charAt(round % key.length()) - '0') + ")");
        }
        return result;
    }

    /**
     * Reverse transform bytes (decryption direction)
     */
    public static byte[] reverseTransform(byte[] input, int round, String key) {
        byte[] result = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            int byteValue = input[i] & 0xFF; // Convert to unsigned 0-255
            int keyDigit = key.charAt((i + round) % key.length()) - '0'; // same key digit
            int shift = 5 + (round * 3) + keyDigit; // MUST MATCH encrypt shift
            int shiftedValue = (byteValue - shift + RANGE) % RANGE;
            result[i] = (byte) shiftedValue;
        }
        // Log reverse transform operation
        if (BlockCipher.VERBOSE_LOGGING) {
            System.out.println("    Reverse Transform: Key-dependent shift reversed (round=" + round + ")");
        }
        return result;
    }

    /**
     * ENHANCED: Complex multi-position shuffle with round-dependent pattern
     * Creates 2-5 chunks based on data size, then shuffles them using round-specific pattern
     */
    public static byte[] splitAndMix(byte[] data, int round, String key) {
        if (data.length <= 1) {
            return data;
        }

        // Determine number of chunks (2-5) based on data size and round
        int numChunks = Math.min(5, Math.max(2, (data.length / 2) + (round % 3)));
        
        // Calculate chunk boundaries dynamically
        int[] boundaries = new int[numChunks + 1];
        boundaries[0] = 0;
        boundaries[numChunks] = data.length;
        
        int keySum = calculateKeySum(key);
        for (int i = 1; i < numChunks; i++) {
            // Variable chunk sizes based on round, key, and position
            int basePos = (data.length * i) / numChunks;
            int variation = ((round * 13 + keySum * 7 + i * 5) % (data.length / numChunks + 1));
            boundaries[i] = Math.min(data.length - 1, Math.max(1, basePos + variation - (data.length / (numChunks * 2))));
        }
        
        // Sort boundaries to ensure they're in order
        java.util.Arrays.sort(boundaries);
        
        // Create chunks
        byte[][] chunks = new byte[numChunks][];
        StringBuilder splitInfo = new StringBuilder("[Round " + round + "] Split into " + numChunks + " chunks: ");
        
        for (int i = 0; i < numChunks; i++) {
            int chunkSize = boundaries[i + 1] - boundaries[i];
            if (chunkSize > 0) {
                chunks[i] = new byte[chunkSize];
                System.arraycopy(data, boundaries[i], chunks[i], 0, chunkSize);
                splitInfo.append("[").append(i).append("]=").append(chunkSize).append("bytes ");
            }
        }
        
        // Shuffle chunks using round-dependent permutation
        int[] shufflePattern = generateShufflePattern(numChunks, round, keySum);
        splitInfo.append("-> Shuffle pattern: ").append(java.util.Arrays.toString(shufflePattern));
        
        // Reassemble in shuffled order
        byte[] result = new byte[data.length];
        int destPos = 0;
        for (int i = 0; i < numChunks; i++) {
            int srcChunk = shufflePattern[i];
            if (chunks[srcChunk] != null) {
                System.arraycopy(chunks[srcChunk], 0, result, destPos, chunks[srcChunk].length);
                destPos += chunks[srcChunk].length;
            }
        }
        
        System.out.println(splitInfo.toString());
        return result;
    }
    
    /**
     * Generate round-dependent shuffle pattern for chunk reordering
     */
    private static int[] generateShufflePattern(int numChunks, int round, int keySum) {
        int[] pattern = new int[numChunks];
        for (int i = 0; i < numChunks; i++) {
            pattern[i] = i;
        }
        
        // Fisher-Yates shuffle with round-dependent seed
        int seed = round * 31 + keySum;
        for (int i = numChunks - 1; i > 0; i--) {
            seed = (seed * 1103515245 + 12345) & 0x7fffffff; // Linear congruential generator
            int j = seed % (i + 1);
            int temp = pattern[i];
            pattern[i] = pattern[j];
            pattern[j] = temp;
        }
        
        return pattern;
    }

    /**
     * ENHANCED (Reverse): Unshuffle complex multi-position pattern
     */
    public static byte[] unsplitAndUnmix(byte[] data, int round, String key) {
        if (data.length <= 1) {
            return data;
        }

        // Must use SAME chunk calculation as encryption
        int numChunks = Math.min(5, Math.max(2, (data.length / 2) + (round % 3)));
        
        int[] boundaries = new int[numChunks + 1];
        boundaries[0] = 0;
        boundaries[numChunks] = data.length;
        
        int keySum = calculateKeySum(key);
        for (int i = 1; i < numChunks; i++) {
            int basePos = (data.length * i) / numChunks;
            int variation = ((round * 13 + keySum * 7 + i * 5) % (data.length / numChunks + 1));
            boundaries[i] = Math.min(data.length - 1, Math.max(1, basePos + variation - (data.length / (numChunks * 2))));
        }
        
        java.util.Arrays.sort(boundaries);
        
        // Get the shuffle pattern
        int[] shufflePattern = generateShufflePattern(numChunks, round, keySum);
        
        // Create reverse pattern (unshuffle)
        int[] reversePattern = new int[numChunks];
        for (int i = 0; i < numChunks; i++) {
            reversePattern[shufflePattern[i]] = i;
        }
        
        // Calculate the SHUFFLED chunk sizes (how they appear in the data)
        int[] shuffledSizes = new int[numChunks];
        for (int i = 0; i < numChunks; i++) {
            int originalChunkIndex = shufflePattern[i];
            shuffledSizes[i] = boundaries[originalChunkIndex + 1] - boundaries[originalChunkIndex];
        }
        
        // Extract shuffled chunks from data using SHUFFLED sizes
        byte[][] shuffledChunks = new byte[numChunks][];
        int srcPos = 0;
        for (int i = 0; i < numChunks; i++) {
            int chunkSize = shuffledSizes[i];  // ✅ USE SHUFFLED SIZES!
            if (chunkSize > 0) {
                shuffledChunks[i] = new byte[chunkSize];
                System.arraycopy(data, srcPos, shuffledChunks[i], 0, chunkSize);
                srcPos += chunkSize;
            }
        }
        
        // Unshuffle back to original order
        // shuffledChunks[i] contains the chunk that was at position shufflePattern[i] originally
        byte[][] originalChunks = new byte[numChunks][];
        for (int i = 0; i < numChunks; i++) {
            originalChunks[shufflePattern[i]] = shuffledChunks[i];
        }
        
        // Reassemble
        byte[] result = new byte[data.length];
        int destPos = 0;
        for (int i = 0; i < numChunks; i++) {
            if (originalChunks[i] != null) {
                System.arraycopy(originalChunks[i], 0, result, destPos, originalChunks[i].length);
                destPos += originalChunks[i].length;
            }
        }
        
        System.out.println("[Round " + round + "] Unshuffled " + numChunks + " chunks back to original order");
        return result;
    }

    /**
     * Calculate sum of all key bytes (used for position calculation)
     */
    private static int calculateKeySum(String key) {
        int sum = 0;
        for (int i = 0; i < key.length(); i++) {
            sum += key.charAt(i);
        }
        return sum;
    }
}
