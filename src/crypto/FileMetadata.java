package crypto;

import java.io.Serializable;

/**
 * File metadata for encrypted file transfers
 * Contains information about the file being transmitted
 */
public class FileMetadata implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String filename;
    private long fileSize;        // Total file size in bytes
    private String mimeType;      // e.g., "image/jpeg", "application/pdf"
    private int totalChunks;      // Number of chunks
    private String checksum;      // SHA-256 checksum for integrity verification
    
    public FileMetadata() {}
    
    public FileMetadata(String filename, long fileSize, String mimeType, int totalChunks, String checksum) {
        this.filename = filename;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.totalChunks = totalChunks;
        this.checksum = checksum;
    }
    
    // Getters and setters
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public int getTotalChunks() {
        return totalChunks;
    }
    
    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }
    
    public String getChecksum() {
        return checksum;
    }
    
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
    
    /**
     * Get human-readable file size
     */
    public String getFormattedSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Convert to protocol string format
     * Format: filename|fileSize|mimeType|totalChunks|checksum
     */
    public String toProtocolString() {
        return String.join("|", 
            filename, 
            String.valueOf(fileSize), 
            mimeType, 
            String.valueOf(totalChunks), 
            checksum
        );
    }
    
    /**
     * Parse from protocol string format
     */
    public static FileMetadata fromProtocolString(String protocolString) {
        String[] parts = protocolString.split("\\|");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid protocol string format");
        }
        
        return new FileMetadata(
            parts[0],                    // filename
            Long.parseLong(parts[1]),    // fileSize
            parts[2],                    // mimeType
            Integer.parseInt(parts[3]),  // totalChunks
            parts[4]                     // checksum
        );
    }
    
    @Override
    public String toString() {
        return String.format("FileMetadata{filename='%s', size=%s, type='%s', chunks=%d}", 
            filename, getFormattedSize(), mimeType, totalChunks);
    }
}
