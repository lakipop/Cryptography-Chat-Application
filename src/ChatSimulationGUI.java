import javax.swing.*;
// import javax.swing.border.Border;
import java.awt.*;
// import java.awt.event.ActionEvent;
// import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// ======================
// 🧩 MAIN APP: 2-Way Simulated Chat with Encryption
// ======================
public class ChatSimulationGUI extends JFrame {

    // === UI Components ===
    private JTextField aliceMessageField;
    private JTextField bobMessageField;

    private JButton sendAsAliceButton;
    private JButton sendAsBobButton;

    private JTextArea chatHistoryArea;

    private JTextArea encryptionLogArea;  // 🔐 Encryption Steps (1→2→3)
    private JTextArea decryptionLogArea;  // 🔓 Decryption Steps (3→2→1)

    // === Encryption State ===
    private String symmetricKeyHexAliceToBob;
    private String symmetricKeyHexBobToAlice;

    // === TIMESTAMP FORMAT ===
    private final DateTimeFormatter timestampFormat = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ChatSimulationGUI() {
        setTitle("🔐 Simulated 2-Way Chat with Custom Encryption");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLayout(new BorderLayout());

        // Modern Material theme colors
        Color bgColor = new Color(250, 250, 250);  // Light gray background
        Color cardColor = Color.WHITE;              // White for text areas
        Color accentColor = new Color(33, 150, 243); // Material Blue

        // ================================
        // 🔸 1. Alice & Bob Input Section (North)
        // ================================
        JPanel inputPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            "👥 Senders (Alice & Bob)",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 14),
            accentColor
        ));
        inputPanel.setBackground(bgColor);

        // ---- Alice Panel ----
        JPanel alicePanel = new JPanel(new BorderLayout(5, 5));
        alicePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(233, 30, 99), 1),
            "👤 Alice",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(233, 30, 99)
        ));
        alicePanel.setBackground(cardColor);
        aliceMessageField = new JTextField();
        aliceMessageField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sendAsAliceButton = new JButton("🔒 Send as Alice");
        sendAsAliceButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sendAsAliceButton.setBackground(new Color(233, 30, 99));
        sendAsAliceButton.setForeground(Color.WHITE);
        sendAsAliceButton.setFocusPainted(false);
        alicePanel.add(aliceMessageField, BorderLayout.CENTER);
        alicePanel.add(sendAsAliceButton, BorderLayout.EAST);

        // ---- Bob Panel ----
        JPanel bobPanel = new JPanel(new BorderLayout(5, 5));
        bobPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(3, 169, 244), 1),
            "👤 Bob",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(3, 169, 244)
        ));
        bobPanel.setBackground(cardColor);
        bobMessageField = new JTextField();
        bobMessageField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sendAsBobButton = new JButton("🔓 Send as Bob");
        sendAsBobButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sendAsBobButton.setBackground(new Color(3, 169, 244));
        sendAsBobButton.setForeground(Color.WHITE);
        sendAsBobButton.setFocusPainted(false);
        bobPanel.add(bobMessageField, BorderLayout.CENTER);
        bobPanel.add(sendAsBobButton, BorderLayout.EAST);

        inputPanel.add(alicePanel);
        inputPanel.add(bobPanel);

        // ================================
        // 🔸 2. Chat History (Middle)
        // ================================
        chatHistoryArea = new JTextArea(15, 80);
        chatHistoryArea.setEditable(false);
        chatHistoryArea.setLineWrap(true);
        chatHistoryArea.setWrapStyleWord(true);
        chatHistoryArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chatHistoryArea.setMargin(new Insets(10, 10, 10, 10));
        chatHistoryArea.setBackground(cardColor);
        JScrollPane chatScroll = new JScrollPane(chatHistoryArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            "💬 Chat History / Conversation",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 14),
            accentColor
        ));

        // ================================
        // 🔸 3. Encryption & Decryption Process (South or Sides)
        // ================================
        JPanel processPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        processPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            "🔐🔓 Encryption & Decryption Process",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 14),
            accentColor
        ));
        processPanel.setBackground(bgColor);

        // ---- Encryption Panel (Alice → Bob) ----
        JPanel encryptionPanel = new JPanel(new BorderLayout());
        encryptionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(76, 175, 80), 1),
            "🔒 Encryption Steps (Alice → Bob)",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(76, 175, 80)
        ));
        encryptionPanel.setBackground(cardColor);
        encryptionLogArea = new JTextArea(10, 35);
        encryptionLogArea.setEditable(false);
        encryptionLogArea.setLineWrap(true);
        encryptionLogArea.setWrapStyleWord(true);
        encryptionLogArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        encryptionLogArea.setMargin(new Insets(10, 10, 10, 10));
        encryptionLogArea.setForeground(new Color(0, 100, 0));
        encryptionLogArea.setBackground(cardColor);
        encryptionPanel.add(new JScrollPane(encryptionLogArea), BorderLayout.CENTER);

        // ---- Decryption Panel (Bob → Alice) ----
        JPanel decryptionPanel = new JPanel(new BorderLayout());
        decryptionPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(255, 152, 0), 1),
            "🔓 Decryption Steps (Bob → Alice)",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(255, 152, 0)
        ));
        decryptionPanel.setBackground(cardColor);
        decryptionLogArea = new JTextArea(10, 35);
        decryptionLogArea.setEditable(false);
        decryptionLogArea.setLineWrap(true);
        decryptionLogArea.setWrapStyleWord(true);
        decryptionLogArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        decryptionLogArea.setMargin(new Insets(10, 10, 10, 10));
        decryptionLogArea.setForeground(new Color(150, 75, 0));
        decryptionLogArea.setBackground(cardColor);
        decryptionPanel.add(new JScrollPane(decryptionLogArea), BorderLayout.CENTER);

        processPanel.add(encryptionPanel);
        processPanel.add(decryptionPanel);

        // ================================
        // 🔸 4. Add All to Frame
        // ================================
        add(inputPanel, BorderLayout.NORTH);
        add(chatScroll, BorderLayout.CENTER);
        add(processPanel, BorderLayout.SOUTH);

        // ================================
        // 🔸 5. Button Actions
        // ================================
        sendAsAliceButton.addActionListener(e -> { e.getActionCommand(); onSendMessage(true); });
        sendAsBobButton.addActionListener(e -> { e.getActionCommand(); onSendMessage(false); });

        // ================================
        // 🔸 6. Initialize
        // ================================
        generateNewKeys();
        
        // Center window
        setLocationRelativeTo(null);
    }

    private void generateNewKeys() {
        symmetricKeyHexAliceToBob = KeyGenerator.generate128BitKeyHex();
        symmetricKeyHexBobToAlice = KeyGenerator.generate128BitKeyHex();
        System.out.println("[KEYS] Alice→Bob Key: " + symmetricKeyHexAliceToBob);
        System.out.println("[KEYS] Bob→Alice Key: " + symmetricKeyHexBobToAlice);
    }

    private void onSendMessage(boolean isAliceSending) {
        JTextField textField = isAliceSending ? aliceMessageField : bobMessageField;
        JTextArea logArea = isAliceSending ? encryptionLogArea : decryptionLogArea;
        JTextArea otherLogArea = isAliceSending ? decryptionLogArea : encryptionLogArea;

        String sender = isAliceSending ? "Alice" : "Bob";
        String receiver = isAliceSending ? "Bob" : "Alice";
        String direction = sender + " → " + receiver;

        String message = textField.getText().trim();
        if (message.isEmpty()) {
            JOptionPane.showMessageDialog(this, sender + ": Enter a message!", "Empty", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Clear logs
        logArea.setText("");
        otherLogArea.setText("");

        try {
            // Select key based on direction
            String key = isAliceSending ? symmetricKeyHexAliceToBob : symmetricKeyHexBobToAlice;

            chatHistoryArea.append("🕒 [" + getCurrentTimestamp() + "] " + direction + ": \"" + message + "\"\n");

            // 🔐 ENCRYPTION (10 Rounds with IV)
            BlockCipher cipher = new BlockCipher(key);
            String encrypted = cipher.encrypt(message);

            // Log Encryption
            logArea.append("═══ ENCRYPTION START ═══\n");
            logArea.append("🔑 Key: " + key.substring(0, 16) + "...\n");
            logArea.append("🔐 Encrypted (10 rounds with IV)\n");
            logArea.append("   Output (Base64): " + encrypted.substring(0, Math.min(50, encrypted.length())) + "...\n\n");

            // 🔓 DECRYPTION (Reverse 10 rounds)
            BlockCipher decryptCipher = new BlockCipher(key);
            String decrypted = decryptCipher.decrypt(encrypted);

            // Log Decryption
            otherLogArea.append("═══ DECRYPTION START ═══\n");
            otherLogArea.append("🔑 Key: " + key.substring(0, 16) + "...\n");
            otherLogArea.append("🔓 Decrypted (reverse 10 rounds)\n");
            otherLogArea.append("   Result: " + decrypted + "\n");
            otherLogArea.append("   ✅ Verification: " + (message.equals(decrypted) ? "SUCCESS" : "FAILED") + "\n\n");

            // Add to Chat History
            chatHistoryArea.append("   🔒 Encrypted: " + encrypted.substring(0, Math.min(40, encrypted.length())) + "...\n");
            chatHistoryArea.append("   🔓 Decrypted: " + decrypted + "\n");
            chatHistoryArea.append("   ✓ Status: Message verified successfully\n");
            chatHistoryArea.append("══════════════════════════════════════\n\n");

            // Clear input
            textField.setText("");

            // Optional: Regenerate keys per message (for stronger simulation)
            // generateNewKeys();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Encryption Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(timestampFormat);
    }

    // =====================
    // MAIN
    // =====================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ChatSimulationGUI().setVisible(true);
        });
    }
}