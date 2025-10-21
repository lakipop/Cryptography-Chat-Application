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
        System.out.println("[Round " + round + "] Transformed " + input.length + " bytes");
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
        System.out.println("[Round " + round + "] Reversed " + input.length + " bytes");
        return result;
    }

    /**
     * ENHANCEMENT #1: Split data at round-dependent position and swap halves
     * This increases diffusion and breaks positional patterns
     * 
     * Formula: position = (round × 7 + keySum) % dataLength
     * 
     * @param data Input data to split and mix
     * @param round Current round number
     * @param key Encryption key (used for position calculation)
     * @return Mixed data with left and right halves swapped
     */
    public static byte[] splitAndMix(byte[] data, int round, String key) {
        if (data.length <= 1) {
            return data; // Can't split single byte or empty
        }

        // Calculate split position based on round and key
        int keySum = calculateKeySum(key);
        int position = ((round * 7) + keySum) % data.length;
        
        // Ensure we don't split at edges (minimum 1 byte on each side)
        if (position == 0) position = 1;
        if (position >= data.length) position = data.length - 1;

        // Split into left and right
        byte[] left = new byte[position];
        byte[] right = new byte[data.length - position];
        
        System.arraycopy(data, 0, left, 0, position);
        System.arraycopy(data, position, right, 0, data.length - position);

        // Swap: right comes first, then left
        byte[] result = new byte[data.length];
        System.arraycopy(right, 0, result, 0, right.length);
        System.arraycopy(left, 0, result, right.length, left.length);

        System.out.println("[Round " + round + "] Split at position " + position + 
                         " (left=" + left.length + ", right=" + right.length + ") and swapped");
        
        return result;
    }

    /**
     * ENHANCEMENT #1 (Reverse): Unsplit and unmix - reverses the swap operation
     * 
     * @param data Mixed data to unsplit
     * @param round Current round number (for position calculation)
     * @param key Encryption key
     * @return Original data order before split
     */
    public static byte[] unsplitAndUnmix(byte[] data, int round, String key) {
        if (data.length <= 1) {
            return data;
        }

        // Calculate same position as encryption
        int keySum = calculateKeySum(key);
        int position = ((round * 7) + keySum) % data.length;
        
        if (position == 0) position = 1;
        if (position >= data.length) position = data.length - 1;

        // In mixed data: right portion is at front, left portion is at back
        int rightLength = data.length - position;
        int leftLength = position;

        byte[] right = new byte[rightLength];
        byte[] left = new byte[leftLength];
        
        System.arraycopy(data, 0, right, 0, rightLength);
        System.arraycopy(data, rightLength, left, 0, leftLength);

        // Restore original order: left first, then right
        byte[] result = new byte[data.length];
        System.arraycopy(left, 0, result, 0, left.length);
        System.arraycopy(right, 0, result, left.length, right.length);

        System.out.println("[Round " + round + "] Unsplit at position " + position + " and restored order");
        
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
