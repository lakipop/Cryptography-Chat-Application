package crypto;

/**
 * Represents an encrypted chunk of a file
 * Each chunk is encrypted independently with its own IV
 */
public class EncryptedFileChunk {
    private int chunkIndex;           // Chunk number (0-based)
    private int totalChunks;          // Total number of chunks
    private String encryptedData;     // Base64-encoded encrypted chunk
    private int originalSize;         // Original chunk size before encryption
    
    public EncryptedFileChunk() {}
    
    public EncryptedFileChunk(int chunkIndex, int totalChunks, String encryptedData, int originalSize) {
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.encryptedData = encryptedData;
        this.originalSize = originalSize;
    }
    
    // Getters and setters
    public int getChunkIndex() {
        return chunkIndex;
    }
    
    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }
    
    public int getTotalChunks() {
        return totalChunks;
    }
    
    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }
    
    public String getEncryptedData() {
        return encryptedData;
    }
    
    public void setEncryptedData(String encryptedData) {
        this.encryptedData = encryptedData;
    }
    
    public int getOriginalSize() {
        return originalSize;
    }
    
    public void setOriginalSize(int originalSize) {
        this.originalSize = originalSize;
    }
    
    /**
     * Check if this is the last chunk
     */
    public boolean isLastChunk() {
        return chunkIndex == totalChunks - 1;
    }
    
    /**
     * Get progress percentage
     */
    public int getProgressPercentage() {
        return (int) ((chunkIndex + 1) * 100.0 / totalChunks);
    }
    
    /**
     * Convert to protocol string format
     * Format: chunkIndex|totalChunks|originalSize|encryptedData
     */
    public String toProtocolString() {
        return String.join("|",
            String.valueOf(chunkIndex),
            String.valueOf(totalChunks),
            String.valueOf(originalSize),
            encryptedData
        );
    }
    
    /**
     * Parse from protocol string format
     */
    public static EncryptedFileChunk fromProtocolString(String protocolString) {
        String[] parts = protocolString.split("\\|", 4); // Limit to 4 parts
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid chunk protocol string format");
        }
        
        return new EncryptedFileChunk(
            Integer.parseInt(parts[0]),  // chunkIndex
            Integer.parseInt(parts[1]),  // totalChunks
            parts[3],                    // encryptedData
            Integer.parseInt(parts[2])   // originalSize
        );
    }
    
    @Override
    public String toString() {
        return String.format("Chunk %d/%d (size=%d bytes, encrypted=%d chars)", 
            chunkIndex + 1, totalChunks, originalSize, encryptedData.length());
    }
}
