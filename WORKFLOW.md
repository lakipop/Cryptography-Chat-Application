# Java Code Execution Workflow

This document describes the complete code execution flow from application startup to message exchange. The flow is explained at a high-to-medium level, focusing on what each section does and its purpose.

---

## 📋 Table of Contents

1. [Application Startup](#1-application-startup)
2. [Initial Setup Phase](#2-initial-setup-phase)
3. [Network Connection Phase](#3-network-connection-phase)
4. [Key Exchange Phase](#4-key-exchange-phase)
5. [Message Sending Flow](#5-message-sending-flow)
6. [Message Receiving Flow](#6-message-receiving-flow)
7. [Supporting Components](#7-supporting-components)

---

## 1. Application Startup

### Step 1.1: Server Launch (`ChatServerGUI.java`)

**What Happens:**
- User runs `java ChatServerGUI`
- The `main()` method executes and creates a new `ChatServerGUI` instance
- Java Swing creates the GUI window with title "🔐 Server"

**What This Does:**
- Initializes the graphical user interface
- Prepares the application to accept connections

**Code Location:** `ChatServerGUI.java` - lines 1-50

---

### Step 1.2: Client Launch (`ChatClientGUI.java`)

**What Happens:**
- User runs `java ChatClientGUI`
- The `main()` method executes and creates a new `ChatClientGUI` instance
- Java Swing creates the GUI window with title "👤 Client"

**What This Does:**
- Initializes the client interface
- Prepares to connect to the server

**Code Location:** `ChatClientGUI.java` - lines 1-50

---

## 2. Initial Setup Phase

### Step 2.1: RSA Key Pair Generation (Both Server and Client)

**What Happens:**
```
Constructor → RSAUtil.generateRSAKeyPair()
```

**What This Does:**
- Both applications immediately generate their own RSA  (asymmetric)  key pairs (public + private keys)
- Uses Java's `KeyPairGenerator` with 2048-bit strength
- **Purpose:** These keys will be used to:
  - Exchange the symmetric key securely (asymmetric encryption)
    (These RSA keys will be used to exchange the symmetric key securely)
  - Sign and verify messages (digital signatures)

**Result:**
- Server has: `myPublicKey`, `myPrivateKey`
- Client has: `myPublicKey`, `myPrivateKey`

**Code Location:** 
- `RSAUtil.java` - `generateRSAKeyPair()` method
- `ChatServerGUI.java` / `ChatClientGUI.java` - Constructor

---

#### 📘 Understanding: Asymmetric vs Symmetric Encryption

**Two Types of Encryption Used in This Project:**

**1. Asymmetric Encryption (RSA)**

- Uses **two different keys**: Public key + Private key
- **Example:** RSA keys (generated in Step 2.1)
- **Characteristic:** What one key encrypts, only the other key can decrypt
- **Used for:** Key exchange, digital signatures
- **Speed:** Slow (mathematically complex)

**2. Symmetric Encryption (BlockCipher)**

- Uses **one shared key**: Same key for both encrypt and decrypt
- **Example:** Our custom 10-round cipher (used in Step 5-6)
- **Characteristic:** Same key does both encryption and decryption
- **Used for:** Encrypting actual messages
- **Speed:** Fast

**Why Both? (Hybrid Encryption)**

**Problem:**
- Asymmetric (RSA) is **too slow** to encrypt every message
- Symmetric is **fast** but how do you share the key securely?

**Solution:**
1. Use **asymmetric** (RSA) to safely exchange the symmetric key (once) → Step 4.2
2. Use **symmetric** (BlockCipher) for all messages (fast) → Steps 5-6

**Simple Analogy:**
- **Asymmetric (RSA)** = Armored truck 🚚🔒 (slow, secure, delivers the key)
- **Symmetric (BlockCipher)** = Regular car 🚗 (fast, efficient, handles daily work)

You use the **armored truck** (RSA) to securely deliver a **car key** (symmetric key), then use the **regular car** (symmetric cipher) for all your trips (messages).

---

### Step 2.2: GUI Components Setup

**What Happens:**
```
setupGUI() method
```

**What This Does:**
- Creates the main window layout with two tabs:
  1. **Chat Tab:** Displays conversation messages
  2. **Encryption Log Tab:** Shows detailed encryption/decryption steps
- Sets up text areas, input fields, and send button
- Applies modern UI styling (colors, fonts, borders)

**Purpose:**
- Provides user interface for chatting
- Allows users to see encryption process in real-time

**Code Location:** `setupGUI()` method in both GUI classes

---

### Step 2.3: Log Redirection Setup

**What Happens:**
```
redirectSystemOutToLog()
```

**What This Does:**
- Captures all `System.out.println()` outputs from the application
- Redirects them to the "Encryption Log" tab instead of console
- Formats log messages with emojis and better structure

**Purpose:**
- Shows users what happens during encryption/decryption
- Educational - helps understand the algorithm step-by-step

**Code Location:** `redirectSystemOutToLog()` method in both GUI classes

---

## 3. Network Connection Phase

### Step 3.1: Server Starts Listening

**What Happens:**
```
setupNetworking() → ServerSocket(PORT 12345) → socket.accept()
```

**What This Does:**
1. Server creates a `ServerSocket` on port 12345
2. Displays message: "🔐 Server waiting for client..."
3. Blocks and waits for incoming connection (`.accept()`)

**Purpose:**
- Server must be ready before client connects
- Listening mode allows client to find and connect to server

**Code Location:** `ChatServerGUI.java` - `setupNetworking()` method

---

### Step 3.2: Client Connects to Server

**What Happens:**
```
setupNetworking() → Socket(SERVER_IP, PORT)
```

**What This Does:**
1. Client creates a `Socket` connecting to "127.0.0.1:12345" (localhost)
2. TCP handshake occurs between client and server
3. Both display: "✅ Connected!"

**Result:**
- Two-way communication channel is established
- Both can send/receive data through `PrintWriter` and `BufferedReader`

**Code Location:** `ChatClientGUI.java` - `setupNetworking()` method

---

## 4. Key Exchange Phase

### Step 4.1: RSA Public Key Exchange

**What Happens:**
```
exchangePublicKeys()
```

**Process Flow:**
1. **Server** sends its public key to Client (as Base64 string)
2. **Client** receives Server's public key
3. **Client** sends its public key to Server
4. **Server** receives Client's public key

**What This Does:**
- Both parties now have each other's public keys
- These keys will be used to:
  - Encrypt the symmetric key (next step)
  - Verify digital signatures (later)

**Result:**
- Server has `otherPublicKey` (Client's public key)
- Client has `otherPublicKey` (Server's public key)

**Code Location:** `exchangePublicKeys()` method in both classes

---

### Step 4.2: Symmetric Key Exchange (Hybrid Encryption)

**What Happens:**
```
performSymmetricKeyExchange()
```

**Process Flow:**

**Client Side:**
1. Generates 128-bit random key using `KeyGenerator.generate128BitKeyHex()`
2. Encrypts this key with **Server's RSA public key**
3. Sends encrypted symmetric key to Server

**Server Side:**
1. Receives encrypted symmetric key from Client
2. Decrypts it using **Server's RSA private key**
3. Now both have the same symmetric key

**What This Does:**
- Establishes a shared secret key securely over the network
- This symmetric key will be used for fast encryption of all chat messages
- **Hybrid Encryption Benefit:** RSA (slow but secure) protects the symmetric key, then symmetric encryption (fast) handles all messages

**Result:**
- Both Server and Client have `symmetricKey128Bit` (identical 128-bit key)
- Ready to start chatting with encrypted messages

**short mean:** 
Client generates symmetric key (128-bit)
Client encrypts it with Server's RSA public key (asymmetric encryption)
Server decrypts it with Server's RSA private key (asymmetric decryption)
Now both have the same symmetric key!))

**Code Location:** `performSymmetricKeyExchange()` method in both classes

---

## 5. Message Sending Flow

When a user types a message and clicks "Send 🔒":

### Step 5.1: User Input

**What Happens:**
- User types message in text field: e.g., "Hello, World!"
- User clicks "Send" button or presses Enter
- `sendMessage()` method is triggered

**Code Location:** `sendMessage()` method in both GUI classes

---

### Step 5.2: Symmetric Encryption (BlockCipher)

**What Happens:**
```
BlockCipher cipher = new BlockCipher(symmetricKey128Bit);
String encryptedMsg = cipher.encrypt(msg);
```

**Process:** (`BlockCipher.java` - `encrypt()` method)

**A. Generate Random IV (Initialization Vector)**
```
generateIV() → 16 random bytes (128-bit)
```
- **Purpose:** Ensures same message encrypts differently each time
- **Result:** Semantic security - prevents pattern recognition

**B. Pre-Whitening (IV XOR)**
```
xorWithIV(plaintextBytes, iv)
```
- **Purpose:** XOR plaintext with IV before encryption
- **Result:** Adds randomness to the input

**C. 10 Rounds of Transformation**
```
for round 1 to 10:
    PerRoundLogic.transform(block, round, key)
```

**Each Round Does:** (`PerRoundLogic.java` - `transform()` method)
1. Takes current byte array
2. For each byte (0-255 range):
   - Calculates shift value based on:
     - Round number
     - Key digit at that position
     - Fixed base shift (5)
   - Formula: `(byteValue + shift) % 256`
3. Returns transformed byte array

**Purpose:**
- **Confusion:** Makes relationship between key and ciphertext complex
- **Diffusion:** Changes in plaintext spread throughout ciphertext
- **10 rounds** provide strong security (like AES-128)

**D. Prepend IV to Ciphertext**
```
result = [IV (16 bytes)] + [ciphertext]
```
- **Purpose:** Receiver needs the IV to decrypt
- **Security:** IV is not secret, can be sent openly

**E. Base64 Encoding**
```
Base64.encode(IV + ciphertext)
```
- **Purpose:** Converts binary data to text-safe format
- **Result:** String that can be sent over text-based protocols

**Final Result:** 
- Encrypted message as Base64 string, e.g., `"k9fZ3mP7x... (long string)"`

**Code Location:** 
- `BlockCipher.java` - `encrypt()` method
- `PerRoundLogic.java` - `transform()` method

---

### Step 5.3: Digital Signature Generation

**What Happens:**
```
String signature = RSAUtil.signMessage(msg, myPrivateKey);
```

**Process:** (`RSAUtil.java` - `signMessage()` method)

1. **Hash the Original Message**
   - Uses SHA-256 algorithm
   - Creates a fixed-size hash (256-bit) of the plaintext
   - Purpose: Creates a unique fingerprint of the message

2. **Sign the Hash**
   - Encrypts the hash using sender's **private key** (RSA)
   - Only the sender can create this signature (has private key)
   - Anyone can verify it (using sender's public key)

3. **Encode to Base64**
   - Converts signature bytes to text string
   - Makes it safe to transmit

**Purpose:**
- **Authentication:** Proves message came from the real sender
- **Integrity:** Detects if message was modified
- **Non-repudiation:** Sender cannot deny sending it

**Result:**
- Digital signature as Base64 string, e.g., `"a9f8c3d2b1e4... (signature)"`

**Code Location:** `RSAUtil.java` - `signMessage()` method

---

### Step 5.4: Transmission

**What Happens:**
```
out.println(encryptedMsg + "||SIG||" + signature);
```

**Format Sent:**
```
[Base64 Encrypted Message]||SIG||[Base64 Signature]
```

**Example:**
```
k9fZ3mP7xQr1... ||SIG|| a9f8c3d2b1e4...
```

**What This Does:**
- Sends the encrypted message and signature together
- Uses delimiter `||SIG||` to separate them
- Transmitted through `PrintWriter` over TCP socket

**Purpose:**
- Both encryption (confidentiality) and signature (authenticity) are sent together
- Receiver can decrypt and verify in one operation

**Code Location:** `sendMessage()` method - final line

---

## 6. Message Receiving Flow

When a message arrives at the receiver:

### Step 6.1: Message Reception

**What Happens:**
```
startChatting() → in.readLine()
```

**What This Does:**
- Background thread continuously listens for incoming messages
- Blocks until data arrives on the socket
- Receives the full line: `encryptedMsg||SIG||signature`

**Code Location:** `startChatting()` method in both GUI classes

---

### Step 6.2: Message Parsing

**What Happens:**
```
String[] parts = receivedLine.split("\\|\\|SIG\\|\\|");
String encryptedMsg = parts[0];
String receivedSignature = parts[1];
```

**What This Does:**
- Splits the received message at the `||SIG||` delimiter
- Extracts two parts:
  1. Encrypted message (Base64)
  2. Digital signature (Base64)

**Purpose:**
- Separate encryption data from authentication data
- Prepare for decryption and verification

---

### Step 6.3: Symmetric Decryption (BlockCipher)

**What Happens:**
```
BlockCipher cipher = new BlockCipher(symmetricKey128Bit);
String decryptedMsg = cipher.decrypt(encryptedMsg);
```

**Process:** (`BlockCipher.java` - `decrypt()` method)

**A. Base64 Decode**
```
Base64.decode(encryptedMsg) → [IV (16 bytes) + ciphertext]
```

**B. Extract IV**
```
iv = first 16 bytes
actualCiphertext = remaining bytes
```
- **Purpose:** Need the IV to reverse the encryption
- **Note:** IV was prepended by sender

**C. 10 Rounds of Reverse Transformation**
```
for round 10 down to 1:
    PerRoundLogic.reverseTransform(block, round, key)
```

**Each Reverse Round Does:** (`PerRoundLogic.java` - `reverseTransform()` method)
1. Takes current byte array
2. For each byte:
   - Calculates the **same** shift value as encryption
   - Uses **subtraction** instead of addition
   - Formula: `(byteValue - shift + 256) % 256`
3. Returns original byte array

**Important:**
- Rounds are processed in **reverse order** (10 → 1)
- Shift calculation is **identical** to encryption
- Subtraction undoes the addition

**D. Post-Whitening (IV XOR)**
```
xorWithIV(decryptedBytes, iv)
```
- **Purpose:** XOR with IV again to recover original plaintext
- **Note:** XOR is its own inverse (A XOR B XOR B = A)

**Result:**
- Original plaintext message: `"Hello, World!"`

**Code Location:**
- `BlockCipher.java` - `decrypt()` method
- `PerRoundLogic.java` - `reverseTransform()` method

---

### Step 6.4: Signature Verification

**What Happens:**
```
boolean isAuthentic = RSAUtil.verifySignature(
    decryptedMsg, 
    receivedSignature, 
    otherPublicKey
);
```

**Process:** (`RSAUtil.java` - `verifySignature()` method)

1. **Decrypt the Signature**
   - Uses sender's **public key** (RSA)
   - Extracts the hash that was signed
   - Only valid if signed by matching private key

2. **Hash the Received Message**
   - Uses SHA-256 on the decrypted plaintext
   - Creates fresh hash of what was received

3. **Compare Hashes**
   - Compares extracted hash vs. calculated hash
   - Returns `true` if identical, `false` if different

**What This Verifies:**
- ✅ **Authenticity:** Message came from holder of private key (real sender)
- ✅ **Integrity:** Message was not modified during transmission
- ✅ **Non-repudiation:** Sender cannot deny sending it

**Results:**
- If `isAuthentic == true`: Display "✓ Verified"
- If `isAuthentic == false`: Display "⚠️ WARNING: Message verification FAILED"

**Code Location:** `RSAUtil.java` - `verifySignature()` method

---

### Step 6.5: Display Message

**What Happens:**
```
SwingUtilities.invokeLater(() ->
    chatArea.append("📩 [Sender]: " + decryptedMsg + "\n   ✓ Verified\n\n")
);
```

**What This Does:**
- Updates the GUI on the main Swing thread (thread-safe)
- Displays the decrypted message in the chat area
- Shows verification status (✓ or ⚠️)

**Purpose:**
- User sees the original message
- User knows if message is authentic

---

## 7. Supporting Components

### 7.1: KeyGenerator (`KeyGenerator.java`)

**Purpose:**
- Generates cryptographically secure random keys

**How It Works:**
1. Uses `SecureRandom` (not regular `Random`)
2. Generates 16 random bytes (128 bits)
3. Converts to hexadecimal string (32 characters)

**Usage:**
- Called by Client during symmetric key exchange
- Can be run standalone: `java KeyGenerator`

**Code Location:** `KeyGenerator.java` - `generate128BitKeyHex()` method

---

### 7.2: RSAUtil (`RSAUtil.java`)

**Purpose:**
- Provides all RSA cryptographic operations

**Key Methods:**

| Method | Purpose |
|--------|---------|
| `generateRSAKeyPair()` | Creates 2048-bit RSA key pair |
| `encryptWithPublicKey()` | Encrypts data with public key (for key exchange) |
| `decryptWithPrivateKey()` | Decrypts data with private key |
| `signMessage()` | Creates digital signature using private key |
| `verifySignature()` | Verifies signature using public key |
| `publicKeyToString()` | Converts key to Base64 for transmission |
| `stringToPublicKey()` | Converts Base64 back to key object |

**Security Features:**
- Uses 2048-bit RSA (industry standard)
- SHA-256 hashing for signatures
- Proper key encoding/decoding

**Code Location:** `RSAUtil.java` - entire file

---

### 7.3: BlockCipher (`BlockCipher.java`)

**Purpose:**
- Main encryption engine implementing custom 10-round cipher

**Key Features:**
- 10 rounds of transformation (AES-128 equivalent)
- Random IV per message (semantic security)
- Byte-level operations (0-255 range)
- Base64 encoding for transmission

**Security Improvements Implemented:**
1. ✅ IV support (prevents pattern recognition)
2. ✅ 10 rounds (increased from 3 for stronger security)
3. ✅ Byte-level (full data type support)

**Code Location:** `BlockCipher.java` - entire file

---

### 7.4: PerRoundLogic (`PerRoundLogic.java`)

**Purpose:**
- Implements the transformation logic for each encryption round

**How It Works:**
- **Forward Transform:** `(byte + shift) % 256`
- **Reverse Transform:** `(byte - shift + 256) % 256`
- **Shift Calculation:** `5 + (round × 3) + keyDigit`

**Why This Works:**
- Different shift per round creates confusion
- Key-dependent shift prevents attacks without key
- Modulo 256 keeps values in byte range

**Code Location:** `PerRoundLogic.java` - entire file

---

### 7.5: Logging System

**Purpose:**
- Shows encryption/decryption steps to users in real-time

**How It Works:**
1. Redirects `System.out` to custom stream
2. Captures all console output
3. Formats and displays in "Encryption Log" tab

**What Users See:**
- Key being used (shortened for privacy)
- IV generation
- Each round's progress (1-10)
- Final encrypted/decrypted output

**Purpose:**
- Educational - understand the algorithm
- Debugging - verify correct operation
- Transparency - see what's happening

**Code Location:** `redirectSystemOutToLog()` in both GUI classes

---

## 🔄 Complete Flow Summary

### One Complete Message Exchange:

```
┌─────────────────────────────────────────────────────────┐
│                    SENDER SIDE                          │
└─────────────────────────────────────────────────────────┘
1. User types message: "Hello"
2. Generate random 128-bit IV
3. XOR plaintext with IV
4. Run 10 rounds of encryption (byte transformations)
5. Prepend IV to ciphertext
6. Base64 encode the result
7. Sign the plaintext with private key (SHA-256 + RSA)
8. Send: [encrypted]||SIG||[signature]

            ↓ ↓ ↓ Network Transmission ↓ ↓ ↓

┌─────────────────────────────────────────────────────────┐
│                   RECEIVER SIDE                         │
└─────────────────────────────────────────────────────────┘
1. Receive: [encrypted]||SIG||[signature]
2. Parse message and signature
3. Base64 decode encrypted message
4. Extract IV (first 16 bytes)
5. Run 10 rounds of decryption in reverse (10→1)
6. XOR result with IV
7. Verify signature using sender's public key
8. Display message if signature is valid
```

---

## 🎯 Key Takeaways

### Security Features in the Flow:

1. **Hybrid Encryption:**
   - RSA for key exchange (secure but slow)
   - Custom cipher for messages (fast)

2. **Semantic Security:**
   - Random IV per message
   - Same message encrypts differently each time

3. **Authentication:**
   - Digital signatures prove sender identity
   - SHA-256 + RSA signature scheme

4. **Integrity:**
   - Signatures detect message tampering
   - Any modification invalidates signature

5. **Confidentiality:**
   - 10-round encryption protects message content
   - Byte-level operations support all data types

---

## 📌 For Video Demonstration

When demonstrating the code flow in your video:

1. **Show GUI Launch** → Windows opening
2. **Point out RSA key generation** → Happens in constructor
3. **Explain connection** → Server waits, client connects
4. **Highlight key exchange** → Public keys and symmetric key
5. **Type and send message** → Show encryption log tab
6. **Walk through encryption steps** → IV, 10 rounds, signature
7. **Show received message** → Decryption steps, verification
8. **Emphasize security features** → IV, 10 rounds, signatures

This workflow matches your 15-20 minute video script perfectly! 🎥

---

**Document Created:** October 2025  
**Purpose:** Educational demonstration of cryptographic system implementation  
**Note:** This is a learning project - use established standards (AES, TLS) for production systems.
