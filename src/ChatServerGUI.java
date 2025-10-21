// ChatServerGUI.java
import javax.swing.*;
import java.awt.*;
// import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.security.*;
// import java.util.Base64;
import java.util.concurrent.*;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

public class ChatServerGUI extends JFrame {
    private final int PORT = 12345;

    // UI
    private JTextArea chatArea;
    private JTextArea encryptionLogArea;
    private JTextField messageField;
    private JButton sendButton;
    private JTabbedPane tabbedPane;

    // Sockets
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ExecutorService executor = Executors.newFixedThreadPool(3);

    // Crypto
    private PublicKey otherPublicKey;
    private PrivateKey myPrivateKey;
    private PublicKey myPublicKey;
    private String symmetricKey128Bit;

    // Logging
    private ByteArrayOutputStream logStream;
    private PrintStream originalOut;
    private PrintStream customLogStream;

    public ChatServerGUI() {
        super("🔐 Server");
        try {
            KeyPair keyPair = RSAUtil.generateRSAKeyPair();
            myPrivateKey = keyPair.getPrivate();
            myPublicKey = keyPair.getPublic();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "RSA Key Generation Failed", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setupGUI();
        redirectSystemOutToLog();
        setupNetworking();
    }

    private void setupGUI() {
        setLayout(new BorderLayout());

        // Modern Material theme colors
        Color cardColor = Color.WHITE;              // White for text areas
        Color accentColor = new Color(33, 150, 243); // Material Blue

        // Chat
        chatArea = new JTextArea(20, 60);
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        chatArea.setMargin(new Insets(10, 10, 10, 10));
        chatArea.setBackground(cardColor);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            "💬 Chat",
            0,
            0,
            new Font("Segoe UI", Font.BOLD, 14),
            accentColor
        ));

        // Encryption Log
        encryptionLogArea = new JTextArea(10, 60);
        encryptionLogArea.setEditable(false);
        encryptionLogArea.setLineWrap(true);
        encryptionLogArea.setWrapStyleWord(true);
        encryptionLogArea.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        encryptionLogArea.setMargin(new Insets(10, 10, 10, 10));
        encryptionLogArea.setForeground(new Color(0, 100, 0));
        encryptionLogArea.setBackground(cardColor);
        JScrollPane logScroll = new JScrollPane(encryptionLogArea);
        logScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            "🔒 Encryption / Decryption Steps",
            0,
            0,
            new Font("Segoe UI", Font.BOLD, 14),
            accentColor
        ));

        // Tabs
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Chat", chatScroll);
        tabbedPane.addTab("Encryption Log", logScroll);

        // Input
        messageField = new JTextField(45);
        messageField.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        sendButton = new JButton("Send 🔒");
        sendButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        sendButton.setBackground(accentColor);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(tabbedPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(_ -> sendMessage());
        messageField.addActionListener(_ -> sendMessage());

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void redirectSystemOutToLog() {
        logStream = new ByteArrayOutputStream();
        customLogStream = new PrintStream(logStream) {
            @Override
            public void println(String x) {
                super.println(x);
                String formatted = formatLogLine(x);
                SwingUtilities.invokeLater(() -> {
                    encryptionLogArea.append(formatted + "\n");
                    encryptionLogArea.setCaretPosition(encryptionLogArea.getDocument().getLength());
                });
            }
        };
        originalOut = System.out;
        System.setOut(customLogStream);
    }

    private String formatLogLine(String line) {
        if (line.contains("Encrypting with key")) {
            return "═══ ENCRYPTION START ═══\n🔑 Key: " + extractShortKey(line);
        } else if (line.contains("Generated IV")) {
            return "🎲 Random IV Generated: " + extractIV(line);
        } else if (line.contains("Round") && line.contains("Transformed")) {
            return "↳ " + line;
        } else if (line.contains("Encrypted (Base64)")) {
            return "✅ " + line + "\n";
        } else if (line.contains("Decrypting with key")) {
            return "═══ DECRYPTION START ═══\n🔑 Key: " + extractShortKey(line);
        } else if (line.contains("Extracted IV")) {
            return "🎲 Extracted IV: " + extractIV(line);
        } else if (line.contains("Decrypted plaintext")) {
            return "✅ " + line + "\n";
        }
        return line;
    }

    private String extractShortKey(String line) {
        int keyStart = line.indexOf("key:");
        if (keyStart > 0) {
            String key = line.substring(keyStart + 4).trim();
            return key.length() > 16 ? key.substring(0, 16) + "..." : key;
        }
        return "";
    }

    private String extractIV(String line) {
        int ivStart = line.indexOf(":");
        if (ivStart > 0) {
            String iv = line.substring(ivStart + 1).trim();
            return iv.length() > 24 ? iv.substring(0, 24) + "..." : iv;
        }
        return "";
    }

    private void restoreSystemOut() {
        if (System.out != originalOut) {
            System.setOut(originalOut);
        }
    }

    private void setupNetworking() {
        executor.submit(() -> {
            try (
                    ServerSocket serverSocket = new ServerSocket(PORT)
            ) {
                // 🔒 Redirect System.out HERE — BEFORE any encryption or messages
                redirectSystemOutToLog();

                SwingUtilities.invokeLater(() ->
                    chatArea.append("🔐 Server waiting for client on port " + PORT + "...\n")
                );
                socket = serverSocket.accept();
                SwingUtilities.invokeLater(() ->
                    chatArea.append("✅ Client connected!\n\n")
                );

                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                exchangePublicKeys();
                performSymmetricKeyExchange();
                startChatting();

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                        chatArea.append("❌ Server Error: " + ex.getMessage() + "\n\n")
                        );
            } finally {
                restoreSystemOut();
            }
        });
    }

    private void exchangePublicKeys() throws Exception {
        String myPubKeyStr = RSAUtil.publicKeyToString(myPublicKey);
        out.println(myPubKeyStr);

        String theirPubKeyStr = in.readLine();
        otherPublicKey = RSAUtil.stringToPublicKey(theirPubKeyStr);

        SwingUtilities.invokeLater(() ->
            chatArea.append("🔑 Public keys exchanged\n")
        );
    }

    private void performSymmetricKeyExchange() throws Exception {
        String encryptedSymmetricKey = in.readLine();
        symmetricKey128Bit = RSAUtil.decryptWithPrivateKey(encryptedSymmetricKey, myPrivateKey);

        SwingUtilities.invokeLater(() ->
            chatArea.append("🔐 Symmetric key received (decrypted). Ready to chat.\n═══════════════\n\n")
        );
    }

    private void startChatting() {
        executor.submit(() -> {
            try {
                String receivedLine;
                while ((receivedLine = in.readLine()) != null) {
                    // Expect format: ENCRYPTED_MSG||SIG||SIGNATURE_BASE64
                    String[] parts = receivedLine.split("\\|\\|SIG\\|\\|");
                    if (parts.length == 2) {
                        String encryptedMsg = parts[0];
                        String receivedSignature = parts[1];

                        // 1. Decrypt the message
                        BlockCipher cipher = new BlockCipher(symmetricKey128Bit);
                        String decryptedMsg = cipher.decrypt(encryptedMsg);

                        // 2. Verify the signature using the SENDER's PUBLIC KEY (e.g. Server public key)
                        boolean isAuthentic = RSAUtil.verifySignature(
                                decryptedMsg,
                                receivedSignature,
                                otherPublicKey  // ✅ This is the sender's public key
                        );

                        if (isAuthentic) {
                            // ✅ Message is legit ✅
                            SwingUtilities.invokeLater(() ->
                                    chatArea.append("📩 Client: " + decryptedMsg + "\n   ✓ Verified\n\n")
                                    );
                        } else {
                            // ❌ Message may be fake or tampered
                            SwingUtilities.invokeLater(() ->
                                    chatArea.append("⚠️ ⚠️ WARNING: Message verification FAILED. Possible tampering.\n\n")
                                    );
                        }
                    } else {
                        // ❌ Invalid message format
                        SwingUtilities.invokeLater(() ->
                                chatArea.append("🚨 Invalid message format.\n\n")
                                );
                    }
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        chatArea.append("❌ Chat Error: " + e.getMessage() + "\n\n")
                        );
            }
        });
    }

    private void sendMessage() {
        String msg = messageField.getText().trim();
        if (msg.isEmpty() || out == null) return;
        messageField.setText("");

        try {
            SwingUtilities.invokeLater(() ->
                chatArea.append("📤 You: " + msg + "\n   ✓ Sent\n\n")
            );

            // 1. Encrypt the message (confidentiality)
            BlockCipher cipher = new BlockCipher(symmetricKey128Bit);
            String encryptedMsg = cipher.encrypt(msg);

            // 2. Sign the ORIGINAL (plaintext) message (authenticity + integrity)
            String signature = RSAUtil.signMessage(msg, myPrivateKey);

            // 3. Send both: encrypted message + signature
            out.println(encryptedMsg + "||SIG||" + signature);

        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() ->
                    chatArea.append("❌ Error: " + e.getMessage() + "\n\n")
                    );
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatServerGUI::new);
    }
}