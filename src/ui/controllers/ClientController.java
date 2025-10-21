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
    
    @FXML
    public void initialize() {
        // Redirect System.out to encryption log
        redirectSystemOut();
        
        // Setup RSA keys
        try {
            KeyPair keyPair = RSAUtil.generateRSAKeyPair();
            myPrivateKey = keyPair.getPrivate();
            myPublicKey = keyPair.getPublic();
            
            Platform.runLater(() -> {
                keyInfoArea.appendText("RSA Key Pair Generated\n");
                keyInfoArea.appendText("Public Key: " + 
                    RSAUtil.publicKeyToString(myPublicKey).substring(0, 50) + "...\n");
            });
        } catch (Exception e) {
            showError("RSA Key Generation Failed: " + e.getMessage());
            return;
        }
        
        // Start network connection
        setupNetworking();
        
        // Start session timer
        startSessionTimer();
        
        // Auto-scroll chat to bottom
        chatContainer.heightProperty().addListener((obs, oldVal, newVal) -> {
            chatScrollPane.setVvalue(1.0);
        });
    }
    
    private void setupNetworking() {
        executor.submit(() -> {
            try {
                updateStatus("Connecting...", "🟡 Connecting", false);
                
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
        
        String encryptedKey = RSAUtil.encryptWithPublicKey(symmetricKey128Bit, otherPublicKey);
        out.println(encryptedKey);
        
        Platform.runLater(() -> {
            addSystemMessage("🔐 Symmetric key sent (RSA encrypted)");
            addSystemMessage("✅ Secure channel established - Ready to chat!");
            addSystemMessage("═══════════════════════════════════════");
            sendButton.setDisable(false);
            messageInputField.setDisable(false);
        });
    }
    
    private void startChatting() {
        executor.submit(() -> {
            try {
                String receivedLine;
                while ((receivedLine = in.readLine()) != null) {
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
        Label sysMsg = new Label(message);
        sysMsg.getStyleClass().add("system-message");
        sysMsg.setMaxWidth(Double.MAX_VALUE);
        sysMsg.setAlignment(Pos.CENTER);
        chatContainer.getChildren().add(sysMsg);
    }
    
    private void updateStatus(String statusBar, String connection, boolean connected) {
        statusBarLabel.setText(statusBar);
        connectionStatusLabel.setText(connection);
        statusDetailLabel.setText(statusBar);
        
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
                Platform.runLater(() -> encryptionLogArea.appendText(String.valueOf((char) b)));
            }
        });
        System.setOut(ps);
    }
    
    private void startSessionTimer() {
        sessionStartTime = System.currentTimeMillis();
        
        Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                long elapsed = (System.currentTimeMillis() - sessionStartTime) / 1000;
                long hours = elapsed / 3600;
                long minutes = (elapsed % 3600) / 60;
                long seconds = elapsed % 60;
                sessionTimeLabel.setText(String.format("⏱️ %02d:%02d:%02d", hours, minutes, seconds));
            })
        );
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }
}
