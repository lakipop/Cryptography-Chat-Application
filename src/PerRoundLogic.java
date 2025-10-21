public class PerRoundLogic {

    // IMPROVEMENT #1: Support full byte range (0-255) instead of ASCII 32-126
    private static final int MIN_BYTE = 0;
    private static final int MAX_BYTE = 255;
    private static final int RANGE = MAX_BYTE - MIN_BYTE + 1; // 256

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
}