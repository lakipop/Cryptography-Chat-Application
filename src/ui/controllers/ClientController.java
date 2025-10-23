package ui.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.Animation;
import javafx.util.Duration;
import javafx.stage.Popup;

import crypto.*;

import java.io.*;
import java.net.*;
import java.security.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

public class ClientController {
    
    // FXML Components
    @FXML private VBox chatContainer;
    @FXML private TextArea encryptionLogArea;
    @FXML private TextField messageInputField;
    @FXML private Button sendButton;
    @FXML private Button emojiButton;
    @FXML private Label connectionStatusLabel;
    @FXML private Label statusBarLabel;
    @FXML private Label statusDetailLabel;
    @FXML private Label serverAddressLabel;
    @FXML private Label encryptionInfoLabel;
    @FXML private Label messagesSentLabel;
    @FXML private Label messagesReceivedLabel;
    @FXML private Label sessionTimeLabel;
    @FXML private Label encryptionModeLabel;
    @FXML private Label networkQualityLabel;
    @FXML private TextArea keyInfoArea;
    @FXML private Region statusIndicator;
    @FXML private ScrollPane chatScrollPane;
    
    // Network
    private final int PORT = 12345;
    private final String SERVER_IP = "127.0.0.1";
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ExecutorService executor = Executors.newFixedThreadPool(3);
    
    // Crypto
    private PublicKey otherPublicKey;
    private PrivateKey myPrivateKey;
    private PublicKey myPublicKey;
    private String symmetricKey128Bit;
    
    // Statistics
    private int messagesSent = 0;
    private int messagesReceived = 0;
    private long sessionStartTime;
    
    // Common emojis for quick access
    private final String[] COMMON_EMOJIS = {
        "😊", "😂", "❤️", "👍", "🎉", "🔒", "✅", "❌", 
        "📁", "💬", "🚀", "⚡", "🌟", "✨", "🔐", "🔑"
    };
    
    // File Transfer
    @FXML private Button attachFileButton;
    private FileTransferHandler fileTransferHandler;
    private java.util.Map<String, java.util.List<EncryptedFileChunk>> incomingFileChunks = new java.util.concurrent.ConcurrentHashMap<>();
    private java.util.Map<String, FileMetadata> incomingFileMetadata = new java.util.concurrent.ConcurrentHashMap<>();
    private java.util.Map<String, byte[]> receivedFilesData = new java.util.concurrent.ConcurrentHashMap<>(); // Store received file data
    private javafx.stage.Stage stage;
    private File selectedFileToSend = null; // File waiting to be sent
    
    @FXML
    public void initialize() {
        // Defer all initialization until FXML components are fully injected
        Platform.runLater(() -> {
            // Redirect System.out to encryption log
            redirectSystemOut();
            
            // Setup RSA keys
            try {
                KeyPair keyPair = RSAUtil.generateRSAKeyPair();
                myPrivateKey = keyPair.getPrivate();
                myPublicKey = keyPair.getPublic();
                
                if (keyInfoArea != null) {
                    keyInfoArea.appendText("RSA Key Pair Generated\n");
                    keyInfoArea.appendText("Public Key: " + 
                        RSAUtil.publicKeyToString(myPublicKey).substring(0, 50) + "...\n");
                }
            } catch (Exception e) {
                showError("RSA Key Generation Failed: " + e.getMessage());
                return;
            }
            
            // Start network connection
            setupNetworking();
            
            // Start session timer
            startSessionTimer();
            
            // Auto-scroll chat to bottom
            if (chatContainer != null && chatScrollPane != null) {
                chatContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
                    chatScrollPane.setVvalue(1.0);
                });
            }
        });
    }
    
    private void setupNetworking() {
        executor.submit(() -> {
            try {
                Platform.runLater(() -> updateStatus("Connecting...", "🟡 Connecting", false));
                
                socket = new Socket(SERVER_IP, PORT);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                Platform.runLater(() -> {
                    updateStatus("Connected", "🟢 Connected", true);
                    addSystemMessage("✅ Connected to server at " + SERVER_IP + ":" + PORT);
                });
                
                exchangePublicKeys();
                performSymmetricKeyExchange();
                startChatting();
                
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    updateStatus("Connection Failed", "🔴 Disconnected", false);
                    showError("Connection Error: " + ex.getMessage());
                });
            }
        });
    }
    
    private void exchangePublicKeys() throws Exception {
        String myPubKeyStr = RSAUtil.publicKeyToString(myPublicKey);
        out.println(myPubKeyStr);
        
        String theirPubKeyStr = in.readLine();
        otherPublicKey = RSAUtil.stringToPublicKey(theirPubKeyStr);
        
        Platform.runLater(() -> {
            addSystemMessage("🔑 Public keys exchanged securely");
            keyInfoArea.appendText("\nServer Public Key: " + 
                theirPubKeyStr.substring(0, 50) + "...\n");
        });
    }
    
    private void performSymmetricKeyExchange() throws Exception {
        String symKey = KeyGenerator.generate128BitKeyHex();
        symmetricKey128Bit = symKey;
        
        // Initialize file transfer handler with cipher
        BlockCipher cipher = new BlockCipher(symmetricKey128Bit);
        fileTransferHandler = new FileTransferHandler(cipher);
        
        String encryptedKey = RSAUtil.encryptWithPublicKey(symmetricKey128Bit, otherPublicKey);
        out.println(encryptedKey);
        
        Platform.runLater(() -> {
            addSystemMessage("🔐 Symmetric key sent (RSA encrypted)");
            addSystemMessage("✅ Secure channel established - Ready to chat!");
            addSystemMessage("═══════════════════════════════════════");
            sendButton.setDisable(false);
            messageInputField.setDisable(false);
            attachFileButton.setDisable(false);
        });
    }
    
    private void startChatting() {
        executor.submit(() -> {
            try {
                String receivedLine;
                while ((receivedLine = in.readLine()) != null) {
                    
                    // Check if this is a file transfer message
                    if (receivedLine.startsWith("FILE_START||")) {
                        handleFileStart(receivedLine);
                        continue;
                    } else if (receivedLine.startsWith("FILE_CHUNK||")) {
                        handleFileChunk(receivedLine);
                        continue;
                    } else if (receivedLine.startsWith("FILE_END||")) {
                        handleFileEnd(receivedLine);
                        continue;
                    }
                    
                    // Regular text message
                    String[] parts = receivedLine.split("\\|\\|SIG\\|\\|");
                    if (parts.length == 2) {
                        String encryptedMsg = parts[0];
                        String receivedSignature = parts[1];
                        
                        // Decrypt message
                        BlockCipher cipher = new BlockCipher(symmetricKey128Bit);
                        String decryptedMsg = cipher.decrypt(encryptedMsg);
                        
                        // Verify signature
                        boolean isAuthentic = RSAUtil.verifySignature(
                            decryptedMsg, receivedSignature, otherPublicKey
                        );
                        
                        messagesReceived++;
                        final String msg = decryptedMsg;
                        final boolean verified = isAuthentic;
                        
                        Platform.runLater(() -> {
                            addReceivedMessage(msg, verified);
                            messagesReceivedLabel.setText(String.valueOf(messagesReceived));
                        });
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateStatus("Disconnected", "🔴 Disconnected", false);
                    addSystemMessage("❌ Connection lost: " + e.getMessage());
                });
            }
        });
    }
    
    @FXML
    private void onSendMessage() {
        // Check if sending a file
        if (selectedFileToSend != null) {
            sendFile(selectedFileToSend);
            // Reset file selection
            selectedFileToSend = null;
            messageInputField.clear();
            messageInputField.setEditable(true);
            sendButton.setText("Send 🚀");
            attachFileButton.setDisable(false);
            return;
        }
        
        // Otherwise send text message
        String msg = messageInputField.getText().trim();
        if (msg.isEmpty() || out == null) return;
        
        messageInputField.clear();
        
        try {
            // Encrypt message
            BlockCipher cipher = new BlockCipher(symmetricKey128Bit);
            String encryptedMsg = cipher.encrypt(msg);
            
            // Sign message
            String signature = RSAUtil.signMessage(msg, myPrivateKey);
            
            // Send
            out.println(encryptedMsg + "||SIG||" + signature);
            
            messagesSent++;
            addSentMessage(msg, true);
            messagesSentLabel.setText(String.valueOf(messagesSent));
            
        } catch (Exception e) {
            showError("Send Error: " + e.getMessage());
        }
    }
    
    @FXML
    private void onEmojiClicked() {
        // Create emoji picker popup with grid layout
        Popup emojiPopup = new Popup();
        emojiPopup.setAutoHide(true);
        
        GridPane grid = new GridPane();
        grid.setHgap(5);
        grid.setVgap(5);
        grid.setPadding(new javafx.geometry.Insets(10));
        grid.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: #D4D4D8; " +
                     "-fx-border-width: 1px; -fx-border-radius: 12px; " +
                     "-fx-background-radius: 12px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2);");
        
        // Arrange emojis in a 4x4 grid
        int col = 0, row = 0;
        for (String emoji : COMMON_EMOJIS) {
            Button emojiBtn = new Button(emoji);
            emojiBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 20px; " +
                            "-fx-padding: 8px; -fx-min-width: 45px; -fx-min-height: 45px; " +
                            "-fx-cursor: hand;");
            emojiBtn.setOnMouseEntered(e -> 
                emojiBtn.setStyle("-fx-background-color: #E8F5E9; -fx-font-size: 20px; " +
                                "-fx-padding: 8px; -fx-min-width: 45px; -fx-min-height: 45px; " +
                                "-fx-border-radius: 8px; -fx-background-radius: 8px; -fx-cursor: hand;"));
            emojiBtn.setOnMouseExited(e -> 
                emojiBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 20px; " +
                                "-fx-padding: 8px; -fx-min-width: 45px; -fx-min-height: 45px; " +
                                "-fx-cursor: hand;"));
            emojiBtn.setOnAction(e -> {
                messageInputField.appendText(emoji);
                messageInputField.requestFocus();
                emojiPopup.hide();
            });
            
            grid.add(emojiBtn, col, row);
            col++;
            if (col >= 4) {  // 4 columns per row
                col = 0;
                row++;
            }
        }
        
        emojiPopup.getContent().add(grid);
        
        // Show popup above the emoji button
        javafx.geometry.Bounds bounds = emojiButton.localToScreen(emojiButton.getBoundsInLocal());
        emojiPopup.show(emojiButton, bounds.getMinX(), bounds.getMinY() - grid.getPrefHeight() - 200);
    }
    
    @FXML
    private void onRestartConnection() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Restart Connection");
        alert.setHeaderText("Restart and regenerate keys?");
        alert.setContentText("This will disconnect, clear chat, and regenerate encryption keys.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                restartConnection();
            }
        });
    }
    
    @FXML
    private void onClearChat() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Chat");
        alert.setHeaderText("Clear all chat messages?");
        alert.setContentText("This will only clear the local display. Messages won't be deleted from the other party.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                chatContainer.getChildren().clear();
                encryptionLogArea.clear();
                addSystemMessage("🗑️ Chat cleared");
            }
        });
    }
    
    private void restartConnection() {
        // Close existing connections
        try {
            if (socket != null) socket.close();
            if (out != null) out.close();
            if (in != null) in.close();
        } catch (IOException e) {
            // Ignore
        }
        
        // Reset state
        chatContainer.getChildren().clear();
        encryptionLogArea.clear();
        messagesSent = 0;
        messagesReceived = 0;
        messagesSentLabel.setText("0");
        messagesReceivedLabel.setText("0");
        sendButton.setDisable(true);
        messageInputField.setDisable(true);
        
        addSystemMessage("🔄 Restarting connection...");
        
        // Regenerate RSA keys
        try {
            KeyPair keyPair = RSAUtil.generateRSAKeyPair();
            myPrivateKey = keyPair.getPrivate();
            myPublicKey = keyPair.getPublic();
        } catch (Exception e) {
            showError("Key regeneration failed: " + e.getMessage());
            return;
        }
        
        // Reconnect
        setupNetworking();
    }
    
    private void addSentMessage(String message, boolean verified) {
        VBox messageBox = new VBox(5);
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setPadding(new Insets(0, 0, 0, 60));
        
        // Message bubble
        TextFlow bubble = new TextFlow(new Text(message));
        bubble.getStyleClass().add("message-sent");
        bubble.setMaxWidth(500);
        
        // Timestamp and status
        HBox meta = new HBox(5);
        meta.setAlignment(Pos.CENTER_RIGHT);
        Label time = new Label(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        time.getStyleClass().add("message-timestamp");
        Label status = new Label(verified ? "✓✓" : "✓");
        status.getStyleClass().add("message-status");
        meta.getChildren().addAll(time, status);
        
        messageBox.getChildren().addAll(bubble, meta);
        chatContainer.getChildren().add(messageBox);
    }
    
    private void addReceivedMessage(String message, boolean verified) {
        VBox messageBox = new VBox(5);
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(0, 60, 0, 0));
        
        // Message bubble
        TextFlow bubble = new TextFlow(new Text(message));
        bubble.getStyleClass().add("message-received");
        bubble.setMaxWidth(500);
        
        // Timestamp and status
        HBox meta = new HBox(5);
        meta.setAlignment(Pos.CENTER_LEFT);
        Label sender = new Label("Server");
        sender.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        Label time = new Label(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        time.getStyleClass().add("message-timestamp");
        Label status = new Label(verified ? "✓" : "⚠️");
        status.getStyleClass().add("message-status");
        meta.getChildren().addAll(sender, time, status);
        
        messageBox.getChildren().addAll(meta, bubble);
        chatContainer.getChildren().add(messageBox);
    }
    
    private void addSystemMessage(String message) {
        if (chatContainer != null) {
            Label sysMsg = new Label(message);
            sysMsg.getStyleClass().add("system-message");
            sysMsg.setMaxWidth(Double.MAX_VALUE);
            sysMsg.setAlignment(Pos.CENTER);
            chatContainer.getChildren().add(sysMsg);
        }
    }
    
    private void updateStatus(String statusBar, String connection, boolean connected) {
        if (statusBarLabel != null) {
            statusBarLabel.setText(statusBar);
        }
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText(connection);
        }
        if (statusDetailLabel != null) {
            statusDetailLabel.setText(statusBar);
        }
        
        if (statusIndicator != null) {
            if (connected) {
                statusIndicator.getStyleClass().removeAll("status-disconnected", "status-connecting");
            } else if (connection.contains("Connecting")) {
                statusIndicator.getStyleClass().remove("status-disconnected");
                statusIndicator.getStyleClass().add("status-connecting");
            } else {
                statusIndicator.getStyleClass().remove("status-connecting");
                statusIndicator.getStyleClass().add("status-disconnected");
            }
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void redirectSystemOut() {
        PrintStream ps = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                if (encryptionLogArea != null) {
                    Platform.runLater(() -> {
                        try {
                            encryptionLogArea.appendText(String.valueOf((char) b));
                        } catch (Exception e) {
                            // Ignore if UI not ready
                        }
                    });
                }
            }
        });
        System.setOut(ps);
    }
    
    private void startSessionTimer() {
        sessionStartTime = System.currentTimeMillis();
        
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> {
                if (sessionTimeLabel != null) {  // Add null check
                    long elapsed = (System.currentTimeMillis() - sessionStartTime) / 1000;
                    long hours = elapsed / 3600;
                    long minutes = (elapsed % 3600) / 60;
                    long seconds = elapsed % 60;
                    sessionTimeLabel.setText(String.format("⏱️ %02d:%02d:%02d", hours, minutes, seconds));
                }
            })
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }
    
    // ========================================
    // FILE TRANSFER METHODS
    // ========================================
    
    /**
     * Set the stage (needed for file dialogs)
     */
    public void setStage(javafx.stage.Stage stage) {
        this.stage = stage;
    }
    
    /**
     * Handle file attach button click
     */
    @FXML
    private void onAttachFileClicked() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select File to Send");
        
        // Add file filters
        fileChooser.getExtensionFilters().addAll(
            new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*"),
            new javafx.stage.FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"),
            new javafx.stage.FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt", "*.xls", "*.xlsx"),
            new javafx.stage.FileChooser.ExtensionFilter("Videos", "*.mp4", "*.avi", "*.mov", "*.wmv", "*.mkv"),
            new javafx.stage.FileChooser.ExtensionFilter("Archives", "*.zip", "*.rar", "*.7z")
        );
        
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            // Check file size (max 100MB)
            long maxSize = 100 * 1024 * 1024; // 100MB
            if (selectedFile.length() > maxSize) {
                showError("File too large! Maximum size is 100MB");
                return;
            }
            
            // Store selected file and show preview in input box
            selectedFileToSend = selectedFile;
            String sizeStr = FileMetadata.formatSize(selectedFile.length());
            messageInputField.setText("📎 " + selectedFile.getName() + " (" + sizeStr + ")");
            messageInputField.setEditable(false);
            sendButton.setText("Send File");
            attachFileButton.setDisable(true);
        }
    }
    
    /**
     * Send a file through the encrypted channel
     */
    private void sendFile(File file) {
        // Disable attach button during transfer
        attachFileButton.setDisable(true);
        
        Platform.runLater(() -> addSystemMessage("📤 Sending file: " + file.getName()));
        
        executor.submit(() -> {
            try {
                // Prepare file for transfer
                Object[] prepared = fileTransferHandler.prepareFileForTransfer(file);
                FileMetadata metadata = (FileMetadata) prepared[0];
                @SuppressWarnings("unchecked")
                java.util.List<EncryptedFileChunk> chunks = (java.util.List<EncryptedFileChunk>) prepared[1];
                
                // Send FILE_START with metadata
                String metadataString = metadata.toProtocolString();
                out.println("FILE_START||" + metadataString);
                
                Platform.runLater(() -> addSystemMessage("📋 Metadata sent: " + metadata.getFormattedSize()));
                
                // Send each chunk
                for (int i = 0; i < chunks.size(); i++) {
                    EncryptedFileChunk chunk = chunks.get(i);
                    String chunkString = chunk.toProtocolString();
                    out.println("FILE_CHUNK||" + chunkString);
                    
                    final int progress = chunk.getProgressPercentage();
                    final int current = i + 1;
                    final int total = chunks.size();
                    
                    Platform.runLater(() -> 
                        updateStatus("Sending file: " + progress + "% (" + current + "/" + total + ")", 
                                    "🟢 Client Connected", true)
                    );
                    
                    // Small delay to avoid overwhelming the network
                    Thread.sleep(10);
                }
                
                // Send FILE_END with signature
                String signature = RSAUtil.signMessage(metadata.getChecksum(), myPrivateKey);
                out.println("FILE_END||" + metadata.getChecksum() + "||SIG||" + signature);
                
                Platform.runLater(() -> {
                    addSentFileMessage(metadata);
                    addSystemMessage("✅ File sent successfully: " + file.getName());
                    updateStatus("Connected", "🟢 Client Connected", true);
                    attachFileButton.setDisable(false);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("File send failed: " + e.getMessage());
                    attachFileButton.setDisable(false);
                });
            }
        });
    }
    
    /**
     * Handle FILE_START message
     */
    private void handleFileStart(String message) {
        try {
            String metadataString = message.substring("FILE_START||".length());
            FileMetadata metadata = FileMetadata.fromProtocolString(metadataString);
            
            // Store metadata
            incomingFileMetadata.put(metadata.getFilename(), metadata);
            incomingFileChunks.put(metadata.getFilename(), new java.util.ArrayList<>());
            
            Platform.runLater(() -> {
                addSystemMessage("📥 Receiving file: " + metadata.getFilename() + 
                               " (" + metadata.getFormattedSize() + ")");
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> showError("File receive error: " + e.getMessage()));
        }
    }
    
    /**
     * Handle FILE_CHUNK message
     */
    private void handleFileChunk(String message) {
        try {
            String chunkString = message.substring("FILE_CHUNK||".length());
            EncryptedFileChunk chunk = EncryptedFileChunk.fromProtocolString(chunkString);
            
            // Find which file this chunk belongs to
            for (FileMetadata metadata : incomingFileMetadata.values()) {
                java.util.List<EncryptedFileChunk> chunks = incomingFileChunks.get(metadata.getFilename());
                if (chunks != null && chunks.size() == chunk.getChunkIndex()) {
                    chunks.add(chunk);
                    
                    final int progress = chunk.getProgressPercentage();
                    Platform.runLater(() -> 
                        updateStatus("Receiving file: " + progress + "%", "🟢 Client Connected", true)
                    );
                    break;
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Handle FILE_END message
     */
    private void handleFileEnd(String message) {
        executor.submit(() -> {
            try {
                String[] parts = message.substring("FILE_END||".length()).split("\\|\\|SIG\\|\\|");
                String checksum = parts[0];
                String signature = parts[1];
                
                // Find the file by checksum
                FileMetadata metadata = null;
                for (FileMetadata meta : incomingFileMetadata.values()) {
                    if (meta.getChecksum().equals(checksum)) {
                        metadata = meta;
                        break;
                    }
                }
                
                if (metadata == null) {
                    Platform.runLater(() -> showError("File metadata not found"));
                    return;
                }
                
                // Verify signature
                boolean isAuthentic = RSAUtil.verifySignature(checksum, signature, otherPublicKey);
                if (!isAuthentic) {
                    Platform.runLater(() -> showError("File signature verification failed!"));
                    return;
                }
                
                // Get chunks
                java.util.List<EncryptedFileChunk> chunks = incomingFileChunks.get(metadata.getFilename());
                
                // Decrypt and reassemble file (this happens in background thread)
                byte[] fileData = fileTransferHandler.receiveAndDecryptFile(metadata, chunks);
                
                // Store file data in memory for later download
                receivedFilesData.put(metadata.getFilename(), fileData);
                
                // Show file message with download button
                final FileMetadata finalMetadata = metadata;
                Platform.runLater(() -> {
                    addReceivedFileMessage(finalMetadata);
                    addSystemMessage("✅ File received: " + finalMetadata.getFilename() + " - Click to download");
                    messagesReceived++;
                    messagesReceivedLabel.setText(String.valueOf(messagesReceived));
                    
                    // Cleanup chunk storage
                    incomingFileMetadata.remove(finalMetadata.getFilename());
                    incomingFileChunks.remove(finalMetadata.getFilename());
                    updateStatus("Connected", "🟢 Client Connected", true);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showError("File receive error: " + e.getMessage());
                    updateStatus("Connected", "🟢 Client Connected", true);
                });
            }
        });
    }
    
    /**
     * Add a sent file message to chat
     */
    private void addSentFileMessage(FileMetadata metadata) {
        VBox messageBox = new VBox(5);
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setPadding(new Insets(0, 0, 0, 60));
        
        // File info box
        VBox fileBox = new VBox(5);
        fileBox.setStyle("-fx-background-color: #E8F5E9; -fx-background-radius: 12px; " +
                        "-fx-padding: 12px; -fx-border-color: #66BB6A; -fx-border-width: 1px; " +
                        "-fx-border-radius: 12px;");
        fileBox.setMaxWidth(350);
        
        Label fileIcon = new Label(getFileIcon(metadata.getMimeType()));
        fileIcon.setStyle("-fx-font-size: 32px;");
        
        Label fileName = new Label(metadata.getFilename());
        fileName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label fileSize = new Label(metadata.getFormattedSize() + " • " + getFileTypeLabel(metadata.getMimeType()));
        fileSize.setStyle("-fx-text-fill: #71717A; -fx-font-size: 12px;");
        
        Label status = new Label("✅ Sent");
        status.setStyle("-fx-text-fill: #2E7D32; -fx-font-size: 11px;");
        
        fileBox.getChildren().addAll(fileIcon, fileName, fileSize, status);
        
        Label timestamp = new Label(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timestamp.setStyle("-fx-text-fill: #71717A; -fx-font-size: 11px;");
        timestamp.setMaxWidth(Double.MAX_VALUE);
        timestamp.setAlignment(Pos.CENTER_RIGHT);
        
        messageBox.getChildren().addAll(fileBox, timestamp);
        chatContainer.getChildren().add(messageBox);
    }
    
    /**
     * Add a received file message to chat
     */
    private void addReceivedFileMessage(FileMetadata metadata) {
        VBox messageBox = new VBox(5);
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(0, 60, 0, 0));
        
        // File info box
        VBox fileBox = new VBox(5);
        fileBox.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 12px; " +
                        "-fx-padding: 12px; -fx-border-color: #D4D4D8; -fx-border-width: 1px; " +
                        "-fx-border-radius: 12px; -fx-cursor: hand;");
        fileBox.setMaxWidth(350);
        
        Label fileIcon = new Label(getFileIcon(metadata.getMimeType()));
        fileIcon.setStyle("-fx-font-size: 32px;");
        
        Label fileName = new Label(metadata.getFilename());
        fileName.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label fileSize = new Label(metadata.getFormattedSize() + " • " + getFileTypeLabel(metadata.getMimeType()));
        fileSize.setStyle("-fx-text-fill: #71717A; -fx-font-size: 12px;");
        
        Label status = new Label("💾 Click to download");
        status.setStyle("-fx-text-fill: #2563EB; -fx-font-size: 11px; -fx-font-weight: bold;");
        
        fileBox.getChildren().addAll(fileIcon, fileName, fileSize, status);
        
        // Make clickable to download
        fileBox.setOnMouseClicked(e -> downloadReceivedFile(metadata));
        fileBox.setOnMouseEntered(e -> 
            fileBox.setStyle("-fx-background-color: #E0F2FE; -fx-background-radius: 12px; " +
                           "-fx-padding: 12px; -fx-border-color: #2563EB; -fx-border-width: 2px; " +
                           "-fx-border-radius: 12px; -fx-cursor: hand;")
        );
        fileBox.setOnMouseExited(e -> 
            fileBox.setStyle("-fx-background-color: #FAFAFA; -fx-background-radius: 12px; " +
                           "-fx-padding: 12px; -fx-border-color: #D4D4D8; -fx-border-width: 1px; " +
                           "-fx-border-radius: 12px; -fx-cursor: hand;")
        );
        
        Label timestamp = new Label(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timestamp.setStyle("-fx-text-fill: #71717A; -fx-font-size: 11px;");
        
        messageBox.getChildren().addAll(fileBox, timestamp);
        chatContainer.getChildren().add(messageBox);
    }
    
    /**
     * Download a received file when user clicks on it
     */
    private void downloadReceivedFile(FileMetadata metadata) {
        byte[] fileData = receivedFilesData.get(metadata.getFilename());
        if (fileData == null) {
            showError("File data not found. It may have been already downloaded or cleared.");
            return;
        }
        
        javafx.stage.FileChooser saveDialog = new javafx.stage.FileChooser();
        saveDialog.setTitle("Save File");
        saveDialog.setInitialFileName(metadata.getFilename());
        
        File saveLocation = saveDialog.showSaveDialog(stage);
        if (saveLocation != null) {
            try {
                fileTransferHandler.saveFile(fileData, saveLocation);
                addSystemMessage("💾 File saved: " + saveLocation.getName());
                
                // Optionally remove from memory after download
                // receivedFilesData.remove(metadata.getFilename());
                
            } catch (IOException e) {
                showError("Failed to save file: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get icon emoji for file type
     */
    private String getFileIcon(String mimeType) {
        if (mimeType.startsWith("image/")) return "🖼️";
        if (mimeType.startsWith("video/")) return "🎥";
        if (mimeType.startsWith("audio/")) return "🎵";
        if (mimeType.contains("pdf")) return "📄";
        if (mimeType.contains("word") || mimeType.contains("document")) return "📝";
        if (mimeType.contains("excel") || mimeType.contains("spreadsheet")) return "📊";
        if (mimeType.contains("powerpoint") || mimeType.contains("presentation")) return "📽️";
        if (mimeType.contains("zip") || mimeType.contains("rar") || mimeType.contains("7z")) return "📦";
        if (mimeType.contains("text")) return "📃";
        return "📁";
    }
    
    /**
     * Get human-readable file type label
     */
    private String getFileTypeLabel(String mimeType) {
        if (mimeType.startsWith("image/")) return "Image";
        if (mimeType.startsWith("video/")) return "Video";
        if (mimeType.startsWith("audio/")) return "Audio";
        if (mimeType.contains("pdf")) return "PDF Document";
        if (mimeType.contains("word")) return "Word Document";
        if (mimeType.contains("excel")) return "Excel Spreadsheet";
        if (mimeType.contains("powerpoint")) return "PowerPoint";
        if (mimeType.contains("zip") || mimeType.contains("rar")) return "Archive";
        if (mimeType.contains("text")) return "Text File";
        return "File";
    }
}
