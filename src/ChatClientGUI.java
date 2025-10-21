// ChatClientGUI.java
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.concurrent.*;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

public class ChatClientGUI extends JFrame {
    private final int PORT = 12345;
    private final String SERVER_IP = "127.0.0.1";

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

    public ChatClientGUI() {
        super("👤 Client");
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
        setLayout(new BorderLayout(10, 10));
        
        // Modern Material colors
        Color bgColor = new Color(250, 250, 250);
        Color cardColor = Color.WHITE;
        Color accentColor = new Color(33, 150, 243); // Material Blue
        Color textColor = new Color(33, 33, 33);
        
        getContentPane().setBackground(bgColor);

        // Chat Area with better text wrapping
        chatArea = new JTextArea(20, 60);
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);  // Enable line wrapping
        chatArea.setWrapStyleWord(true);  // Wrap at word boundaries
        chatArea.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        chatArea.setBackground(cardColor);
        chatArea.setForeground(textColor);
        chatArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1)
        ));

        // Encryption Log with better formatting
        encryptionLogArea = new JTextArea(10, 60);
        encryptionLogArea.setEditable(false);
        encryptionLogArea.setLineWrap(true);
        encryptionLogArea.setWrapStyleWord(true);
        encryptionLogArea.setForeground(new Color(0, 100, 0)); // Dark green
        encryptionLogArea.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 13));
        encryptionLogArea.setBackground(new Color(245, 255, 245)); // Light green bg
        encryptionLogArea.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane logScroll = new JScrollPane(encryptionLogArea);
        logScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1)
        ));

        // Tabs with modern look
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        tabbedPane.setBackground(bgColor);
        tabbedPane.addTab("💬 Chat", chatScroll);
        tabbedPane.addTab("🔒 Encryption Log", logScroll);

        // Input Panel with modern styling
        messageField = new JTextField(45);
        messageField.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        messageField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        sendButton = new JButton("Send 🔒");
        sendButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        sendButton.setBackground(accentColor);
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(bgColor);
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(tabbedPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(this::sendMessage);
        messageField.addActionListener(this::sendMessage);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null); // Center on screen
        setVisible(true);
    }

    private void redirectSystemOutToLog() {
        logStream = new ByteArrayOutputStream();
        customLogStream = new PrintStream(logStream) {
            @Override
            public void println(String x) {
                super.println(x);
                SwingUtilities.invokeLater(() -> {
                    // Format output for better readability
                    String formatted = formatLogLine(x);
                    if (!formatted.isEmpty()) {
                        encryptionLogArea.append(formatted + "\n");
                        encryptionLogArea.setCaretPosition(encryptionLogArea.getDocument().getLength());
                    }
                });
            }
        };
        originalOut = System.out;
        System.setOut(customLogStream);
    }
    
    private String formatLogLine(String line) {
        if (line == null || line.trim().isEmpty()) return "";
        
        // Simplify and format log lines for better understanding
        if (line.contains("Encrypting with key:")) {
            return "═══ ENCRYPTION START ═══\n🔑 Key: " + line.substring(line.indexOf(":") + 1).trim().substring(0, 16) + "...";
        } else if (line.contains("Generated IV:")) {
            return "🎲 Random IV Generated: " + line.substring(line.indexOf(":") + 1).trim().substring(0, 24) + "...";
        } else if (line.contains("Round") && line.contains("Transformed")) {
            String roundNum = line.substring(line.indexOf("[Round") + 7, line.indexOf("]"));
            return "  ↳ Round " + roundNum + " transformation complete";
        } else if (line.contains("Encrypted Output:")) {
            String output = line.substring(line.indexOf(":") + 1).trim();
            return "✅ Encrypted (Base64): " + (output.length() > 50 ? output.substring(0, 50) + "..." : output) + "\n";
        } else if (line.contains("Decrypting with key:")) {
            return "\n═══ DECRYPTION START ═══\n🔑 Key: " + line.substring(line.indexOf(":") + 1).trim().substring(0, 16) + "...";
        } else if (line.contains("Extracted IV:")) {
            return "🎲 IV Extracted: " + line.substring(line.indexOf(":") + 1).trim().substring(0, 24) + "...";
        } else if (line.contains("Reversed")) {
            String roundNum = line.substring(line.indexOf("[Round") + 7, line.indexOf("]"));
            return "  ↳ Round " + roundNum + " reverse complete";
        } else if (line.contains("Decrypted Output:")) {
            return "✅ Decrypted: " + line.substring(line.indexOf(":") + 1).trim() + "\n";
        } else if (line.startsWith("----")) {
            return ""; // Skip separator lines
        }
        return line;
    }

    private void restoreSystemOut() {
        if (System.out != originalOut) {
            System.setOut(originalOut);
        }
    }

    private void setupNetworking() {
        executor.submit(() -> {
            try {
                // 🔐 Redirect System.out HERE — BEFORE any encryption or messages
                redirectSystemOutToLog();

                SwingUtilities.invokeLater(() -> 
                    chatArea.append("👤 Client connecting to Server (" + SERVER_IP + ":" + PORT + ")...\n")
                );
                socket = new Socket(SERVER_IP, PORT);
                SwingUtilities.invokeLater(() -> 
                    chatArea.append("✅ Connected to Server!\n\n")
                );

                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                exchangePublicKeys();
                performSymmetricKeyExchange();
                startChatting();

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                    chatArea.append("❌ Client Error: " + ex.getMessage() + "\n")
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
        String symKey = KeyGenerator.generate128BitKeyHex(); // 128-bit key (32 hex chars)
        symmetricKey128Bit = symKey;

        String encryptedKey = RSAUtil.encryptWithPublicKey(symmetricKey128Bit, otherPublicKey);
        out.println(encryptedKey);

        SwingUtilities.invokeLater(() -> 
            chatArea.append("🔐 Symmetric key sent (encrypted)\n✅ Ready to chat!\n\n" +
                          "═══════════════════════════════\n\n")
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

                        // 2. Verify the signature using the SENDER's PUBLIC KEY (e.g. Server's public key)
                        boolean isAuthentic = RSAUtil.verifySignature(
                                decryptedMsg,
                                receivedSignature,
                                otherPublicKey  // ✅ This is the sender's public key
                        );

                        if (isAuthentic) {
                            // ✅ Message is legit ✅
                            SwingUtilities.invokeLater(() ->
                                chatArea.append("📩 Server: " + decryptedMsg + "\n   ✓ Verified\n\n")
                            );
                        } else {
                            // ❌ Message may be fake or tampered
                            SwingUtilities.invokeLater(() ->
                                chatArea.append("⚠️  WARNING: Message verification FAILED\n   Possible tampering detected!\n\n")
                            );
                        }
                    } else {
                        // ❌ Invalid message format
                        SwingUtilities.invokeLater(() ->
                            chatArea.append("🚨 Invalid message format\n\n")
                        );
                    }
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() ->
                        chatArea.append("❌ Chat Error: " + e.getMessage() + " ")
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

    private void sendMessage(java.awt.event.ActionEvent e) {
        sendMessage();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClientGUI::new);
    }
}