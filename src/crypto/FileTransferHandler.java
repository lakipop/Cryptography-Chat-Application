package crypto;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Handles file transfer operations including chunking, encryption, and decryption
 * Supports large files through chunked transfer (1MB chunks)
 */
public class FileTransferHandler {
    
    // Maximum chunk size: 1MB (1,048,576 bytes)
    public static final int CHUNK_SIZE = 1024 * 1024;
    
    // Maximum file size: 100MB
    public static final long MAX_FILE_SIZE = 100 * 1024 * 1024;
    
    private BlockCipher cipher;
    
    public FileTransferHandler(BlockCipher cipher) {
        this.cipher = cipher;
    }
    
    /**
     * Prepare file for transmission by creating metadata and encrypted chunks
     * 
     * @param file File to send
     * @return Array containing [FileMetadata, List<EncryptedFileChunk>]
     * @throws IOException If file reading fails
     */
    public Object[] prepareFileForTransfer(File file) throws IOException {
        System.out.println("\n============================================================");
        System.out.println("           FILE TRANSFER PREPARATION START");
        System.out.println("============================================================");
        System.out.println("File: " + file.getName());
        System.out.println("Size: " + formatFileSize(file.length()));
        System.out.println("Path: " + file.getAbsolutePath());
        
        // Validate file size
        if (file.length() > MAX_FILE_SIZE) {
            throw new IOException("File too large. Maximum size: " + formatFileSize(MAX_FILE_SIZE));
        }
        
        if (!file.exists() || !file.isFile()) {
            throw new IOException("File does not exist or is not a valid file");
        }
        
        // Read entire file into memory
        byte[] fileData = Files.readAllBytes(file.toPath());
        System.out.println("File loaded: " + fileData.length + " bytes");
        
        // Calculate SHA-256 checksum
        String checksum = calculateChecksum(fileData);
        System.out.println("Checksum (SHA-256): " + checksum.substring(0, 16) + "...");
        
        // Determine MIME type
        String mimeType = determineMimeType(file.getName());
        System.out.println("MIME type: " + mimeType);
        
        // Calculate number of chunks
        int totalChunks = (int) Math.ceil((double) fileData.length / CHUNK_SIZE);
        System.out.println("Total chunks: " + totalChunks);
        System.out.println("Chunk size: " + formatFileSize(CHUNK_SIZE));
        System.out.println("------------------------------------------------------------\n");
        
        // Create metadata
        FileMetadata metadata = new FileMetadata(
            file.getName(),
            file.length(),
            mimeType,
            totalChunks,
            checksum
        );
        
        // Encrypt file in chunks
        List<EncryptedFileChunk> encryptedChunks = new ArrayList<>();
        
        for (int i = 0; i < totalChunks; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, fileData.length);
            int chunkSize = end - start;
            
            // Extract chunk
            byte[] chunkData = new byte[chunkSize];
            System.arraycopy(fileData, start, chunkData, 0, chunkSize);
            
            System.out.println("Chunk " + (i + 1) + "/" + totalChunks + ":");
            System.out.println("  Bytes: " + start + " to " + (end - 1) + " (" + chunkSize + " bytes)");
            
            // Encrypt chunk (BlockCipher works on byte arrays converted to strings)
            // For binary data, we Base64 encode first, then encrypt
            String chunkBase64 = Base64.getEncoder().encodeToString(chunkData);
            String encryptedChunk = cipher.encrypt(chunkBase64);
            
            System.out.println("  Original: " + chunkSize + " bytes");
            System.out.println("  Base64: " + chunkBase64.length() + " chars");
            System.out.println("  Encrypted: " + encryptedChunk.length() + " chars");
            System.out.println("  Progress: " + ((i + 1) * 100 / totalChunks) + "%\n");
            
            // Create encrypted chunk object
            EncryptedFileChunk chunk = new EncryptedFileChunk(
                i,
                totalChunks,
                encryptedChunk,
                chunkSize
            );
            
            encryptedChunks.add(chunk);
        }
        
        System.out.println("============================================================");
        System.out.println("        FILE PREPARATION COMPLETE");
        System.out.println("============================================================");
        System.out.println("Metadata created: " + metadata);
        System.out.println("Encrypted chunks: " + encryptedChunks.size());
        System.out.println("Ready for transmission\n");
        
        return new Object[] { metadata, encryptedChunks };
    }
    
    /**
     * Decrypt and reassemble file from encrypted chunks
     * 
     * @param metadata File metadata
     * @param encryptedChunks List of encrypted chunks
     * @return Decrypted file data as byte array
     * @throws Exception If decryption or reassembly fails
     */
    public byte[] receiveAndDecryptFile(FileMetadata metadata, List<EncryptedFileChunk> encryptedChunks) 
            throws Exception {
        System.out.println("\n============================================================");
        System.out.println("           FILE RECEPTION & DECRYPTION START");
        System.out.println("============================================================");
        System.out.println("File: " + metadata.getFilename());
        System.out.println("Expected size: " + metadata.getFormattedSize());
        System.out.println("Expected chunks: " + metadata.getTotalChunks());
        System.out.println("------------------------------------------------------------\n");
        
        // Validate chunk count
        if (encryptedChunks.size() != metadata.getTotalChunks()) {
            throw new Exception("Chunk count mismatch! Expected " + metadata.getTotalChunks() + 
                              ", got " + encryptedChunks.size());
        }
        
        // Decrypt all chunks
        ByteArrayOutputStream fileOutputStream = new ByteArrayOutputStream();
        
        for (int i = 0; i < encryptedChunks.size(); i++) {
            EncryptedFileChunk chunk = encryptedChunks.get(i);
            
            System.out.println("Decrypting chunk " + (i + 1) + "/" + metadata.getTotalChunks() + ":");
            System.out.println("  Chunk index: " + chunk.getChunkIndex());
            System.out.println("  Encrypted size: " + chunk.getEncryptedData().length() + " chars");
            
            // Decrypt chunk
            String decryptedBase64 = cipher.decrypt(chunk.getEncryptedData());
            byte[] chunkData = Base64.getDecoder().decode(decryptedBase64);
            
            System.out.println("  Decrypted: " + chunkData.length + " bytes");
            System.out.println("  Expected: " + chunk.getOriginalSize() + " bytes");
            
            // Verify size matches
            if (chunkData.length != chunk.getOriginalSize()) {
                throw new Exception("Chunk " + i + " size mismatch after decryption!");
            }
            
            // Write to output stream
            fileOutputStream.write(chunkData);
            System.out.println("  Status: OK - Added to file buffer");
            System.out.println("  Progress: " + chunk.getProgressPercentage() + "%\n");
        }
        
        byte[] completeFile = fileOutputStream.toByteArray();
        
        System.out.println("------------------------------------------------------------");
        System.out.println("All chunks decrypted and reassembled");
        System.out.println("Total bytes: " + completeFile.length);
        System.out.println("Expected bytes: " + metadata.getFileSize());
        
        // Verify file size
        if (completeFile.length != metadata.getFileSize()) {
            throw new Exception("File size mismatch! Expected " + metadata.getFileSize() + 
                              ", got " + completeFile.length);
        }
        
        // Verify checksum
        String receivedChecksum = calculateChecksum(completeFile);
        System.out.println("\nChecksum verification:");
        System.out.println("  Expected: " + metadata.getChecksum());
        System.out.println("  Received: " + receivedChecksum);
        
        if (!receivedChecksum.equals(metadata.getChecksum())) {
            throw new Exception("Checksum mismatch! File may be corrupted.");
        }
        
        System.out.println("  Status: VERIFIED - Checksums match!");
        System.out.println("\n============================================================");
        System.out.println("        FILE RECEPTION COMPLETE - FILE INTACT");
        System.out.println("============================================================\n");
        
        return completeFile;
    }
    
    /**
     * Calculate SHA-256 checksum of data
     */
    private String calculateChecksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    /**
     * Determine MIME type from filename extension
     */
    private String determineMimeType(String filename) {
        String lower = filename.toLowerCase();
        
        // Images
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".webp")) return "image/webp";
        
        // Documents
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
        if (lower.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        
        // Text
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".csv")) return "text/csv";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".xml")) return "application/xml";
        
        // Video
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".avi")) return "video/x-msvideo";
        if (lower.endsWith(".mov")) return "video/quicktime";
        if (lower.endsWith(".wmv")) return "video/x-ms-wmv";
        if (lower.endsWith(".mkv")) return "video/x-matroska";
        
        // Audio
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".wav")) return "audio/wav";
        if (lower.endsWith(".ogg")) return "audio/ogg";
        
        // Archives
        if (lower.endsWith(".zip")) return "application/zip";
        if (lower.endsWith(".rar")) return "application/x-rar-compressed";
        if (lower.endsWith(".7z")) return "application/x-7z-compressed";
        
        // Default
        return "application/octet-stream";
    }
    
    /**
     * Format file size in human-readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Save decrypted file to disk
     */
    public void saveFile(byte[] fileData, File destinationFile) throws IOException {
        System.out.println("Saving file to: " + destinationFile.getAbsolutePath());
        Files.write(destinationFile.toPath(), fileData);
        System.out.println("File saved successfully: " + formatFileSize(fileData.length));
    }
}
