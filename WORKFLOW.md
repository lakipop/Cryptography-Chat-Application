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

### Step 1.1: Server Launch (`ChatServerFX.java`)

**What Happens:**
- User runs `java ui.ChatServerFX` or executes the Maven/Gradle launch configuration
- The JavaFX `start()` method executes and loads the FXML layout
- JavaFX creates the application window with modern green/zinc theme

**What This Does:**
- Initializes the JavaFX graphical user interface
- Loads `chat_server.fxml` layout definition
- Connects to `ServerController` for UI logic
- Applies CSS styling from `chat_theme.css`
- Prepares the application to accept connections

**Code Location:** `ChatServerFX.java` - lines 1-50, `ServerController.java`

---

### Step 1.2: Client Launch (`ChatClientFX.java`)

**What Happens:**
- User runs `java ui.ChatClientFX` or executes the Maven/Gradle launch configuration
- The JavaFX `start()` method executes and loads the FXML layout
- JavaFX creates the application window with modern green/zinc theme

**What This Does:**
- Initializes the client interface
- Loads `chat_client.fxml` layout definition
- Connects to `ClientController` for UI logic
- Applies CSS styling from `chat_theme.css`
- Prepares to connect to the server

**Code Location:** `ChatClientFX.java` - lines 1-50, `ClientController.java`

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
- `crypto/RSAUtil.java` - `generateRSAKeyPair()` method
- `ChatServerFX.java` / `ChatClientFX.java` - Application startup
- `ServerController.java` / `ClientController.java` - Controller initialization

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
JavaFX loads FXML layout → FXMLLoader connects to Controller → CSS styling applied
```

**What This Does:**
- Creates the main window layout using FXML declarative UI:
  - **Chat Area:** TextArea displaying conversation messages
  - **Encryption Log Area:** TextArea showing detailed encryption/decryption steps  
  - **Input Field:** TextField for message entry
  - **Send Button:** Button to encrypt and transmit message
  - **File Transfer Controls:** Button for selecting and sending files
- Applies modern green/zinc theme via `chat_theme.css`
- Binds UI elements to controller methods

**Purpose:**
- Provides user interface for chatting
- Allows users to see encryption process in real-time
- Enables file transfer functionality

**Code Location:** 
- FXML layouts: `resources/fxml/chat_server.fxml`, `chat_client.fxml`
- Controllers: `ServerController.java`, `ClientController.java`
- Styling: `resources/css/chat_theme.css`

---

### Step 2.3: Log Redirection Setup

**What Happens:**
```
redirectSystemOutToTextArea()
```

**What This Does:**
- Captures all `System.out.println()` outputs from cryptography classes
- Redirects them to the "Encryption Log" TextArea instead of console
- Uses `Platform.runLater()` to safely update JavaFX UI from background threads
- Formats log messages with emojis and structure

**Purpose:**
- Shows users what happens during encryption/decryption
- Educational - helps understand the algorithm step-by-step
- Demonstrates: IV generation, round transformations, shuffle patterns, IV embedding

**Code Location:** 
- `ServerController.java` - `redirectSystemOutToTextArea()` method
- `ClientController.java` - `redirectSystemOutToTextArea()` method
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

**Code Location:** `ServerController.java` - `setupNetworking()` method

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

**Code Location:** `ClientController.java` - `setupNetworking()` method

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

**Process:** (`crypto/BlockCipher.java` - `encrypt()` method)

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

**C. 10 Rounds of Multi-Chunk Transformation**
```
for round 1 to 10:
    PerRoundLogic.splitAndMix(block, round, key)     // Split into chunks & shuffle
    PerRoundLogic.transform(block, round, key)       // Byte transformations
```

**Each Round Does:** (`crypto/PerRoundLogic.java`)

**Part 1: Split and Mix** (`splitAndMix()` method)
1. **Calculate Number of Chunks**
   - Formula: `Math.min(5, Math.max(2, (dataLength / 2) + (round % 3)))`
   - Result: 2-5 chunks depending on data size and round
   
2. **Calculate Dynamic Boundaries**
   - Uses round number and key sum for variation
   - Formula: `basePos + ((round * 13 + keySum * 7 + i * 5) % variation)`
   - Result: Chunk sizes vary by round and key
   
3. **Fisher-Yates Shuffle**
   - Seed: `round * 31 + keySum`
   - Randomly reorders chunks using key-dependent seed
   - Result: Unpredictable chunk arrangement
   
4. **Log Output Example:**
   ```
   [Round 1] Split into 3 chunks: [0]=3bytes [1]=2bytes [2]=2bytes
   Shuffle pattern: [2, 0, 1] (chunk 2 first, then 0, then 1)
   ```

**Part 2: Byte Transformation** (`transform()` method)
1. Takes current byte array (already shuffled)
2. For each byte (0-255 range):
   - Calculates shift value based on:
     - Round number
     - Key digit at that position
     - Fixed base shift (5)
   - Formula: `(byteValue + shift) % 256`
3. Returns transformed byte array

**Purpose:**
- **Confusion:** Makes relationship between key and ciphertext complex
- **Diffusion:** Changes in plaintext spread throughout ciphertext via chunking
- **Structural Unpredictability:** Chunk shuffling breaks positional patterns
- **10 rounds** provide strong security (like AES-128)

**D. Dual IV Embedding**
```
embedIVMultiPosition(ciphertext, iv, key)
```

**Strategy 1: XOR Obfuscation** (`BlockCipher.java` - lines 172-273)
1. **Calculate Strategic Positions**
   - Uses `calculateIVPositions(ciphertext, key)` method
   - XOR-based hash mixing: Knuth multiplicative hash
   - Unpredictable positions based on key
   - Example positions: 8 (72.7%), 15 (36.4%), etc.
   
2. **XOR IV at Positions**
   - XORs each IV byte at calculated positions
   - Obfuscates IV within ciphertext
   - Makes IV extraction difficult

**Strategy 2: Physical Insertion**
1. **Break IV into Chunks**
   - Splits 16-byte IV into 4 chunks (4 bytes each)
   - Log example: `Chunk1[BCF2A3BC] Chunk2[BD724CAF]...`
   
2. **Calculate Insertion Positions**
   - Positions based on data size percentages
   - Example: Chunk1 at 6.3%, Chunk2 at 43.8%, etc.
   
3. **Insert IV Chunks**
   - Physically inserts IV chunks at calculated positions
   - Creates interleaved structure: `[Cipher][IV-Chunk1][Cipher][IV-Chunk2]...`
   - Log shows combining: 
     ```
     [Cipher:2D] (0.0% - 6.3%)
     [IV-Chunk1:BCF2A3BC] <- inserted at position 1 (6.3%)
     [Cipher:A7] (6.3% - 43.8%)
     [IV-Chunk2:BD724CAF] <- inserted at position 2 (43.8%)
     ...
     ```

**Result:**
- Dual-embedded IV with both XOR and physical insertion
- IV position unpredictable without key knowledge
- Maximum diffusion and obfuscation

**E. Base64 Encoding**
```
Base64.encode(dual_embedded_ciphertext)
```
- **Purpose:** Converts binary data to text-safe format
- **Result:** String that can be sent over text-based protocols

**Final Result:** 
- Encrypted message as Base64 string, e.g., `"k9fZ3mP7x... (long string)"`

**Code Location:** 
- `crypto/BlockCipher.java` - `encrypt()`, `embedIVMultiPosition()`, `calculateIVPositions()`
- `crypto/PerRoundLogic.java` - `splitAndMix()`, `transform()`

**Educational Logging:**
- Full hex displays for text messages showing all steps
- Minimal single-line logs for file transfers (performance)
- Automatic mode switching via `setFileTransferMode(boolean)`

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

**Code Location:** `crypto/RSAUtil.java` - `signMessage()` method

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

**Process:** (`crypto/BlockCipher.java` - `decrypt()` method)

**A. Base64 Decode**
```
Base64.decode(encryptedMsg) → dual_embedded_ciphertext
```

**B. Extract Dual-Embedded IV**
```
extractIVMultiPosition(ciphertext, key) → [iv, actualCiphertext]
```

**Strategy 1: Remove Inserted IV Chunks** (`BlockCipher.java` - lines 275-328)
1. **Calculate Insertion Positions**
   - Same positions as encryption (based on data size percentages)
   - Example: Chunk1 at 6.3%, Chunk2 at 43.8%, etc.
   
2. **Extract IV Chunks**
   - Reads 4 bytes at each calculated position
   - Collects 4 chunks (16 bytes total)
   - Removes chunks from ciphertext
   
3. **Reconstruct IV**
   - Concatenates extracted chunks: `Chunk1 + Chunk2 + Chunk3 + Chunk4`
   - Result: 16-byte IV

**Strategy 2: Remove XOR Obfuscation**
1. **Calculate XOR Positions**
   - Uses `calculateIVPositions(ciphertext, key)` (same as encryption)
   - XOR-based hash mixing provides unpredictable positions
   
2. **Remove XOR**
   - XORs ciphertext at calculated positions with IV bytes
   - XOR is its own inverse: `(data XOR iv) XOR iv = data`
   
3. **Clean Ciphertext**
   - Result: Original ciphertext without IV embedding

**C. 10 Rounds of Reverse Transformation**
```
for round 10 down to 1:
    PerRoundLogic.unsplitAndUnmix(block, round, key)  // Unshuffle chunks
    PerRoundLogic.reverseTransform(block, round, key)  // Reverse byte shifts
```

**Each Reverse Round Does:** (`crypto/PerRoundLogic.java`)

**Part 1: Unsplit and Unmix** (`unsplitAndUnmix()` method - lines 129-205)
1. **Recreate Shuffle Pattern**
   - Same Fisher-Yates algorithm with same seed
   - Seed: `round * 31 + keySum`
   - Result: Know which chunks were swapped
   
2. **Calculate Shuffled Boundaries**
   - CRITICAL: Boundaries match shuffled chunk order, not original
   - Formula: `shuffledSizes[i] = boundaries[shufflePattern[i]+1] - boundaries[shufflePattern[i]]`
   - Example: If pattern was [2,0,1], read chunk2 size first, then chunk0, then chunk1
   
3. **Extract Shuffled Chunks**
   - Uses `shuffledSizes` to read correct chunk sizes from data
   - System.arraycopy reads bytes using shuffled boundaries
   
4. **Unshuffle to Original Order**
   - Formula: `originalChunks[shufflePattern[i]] = shuffledChunks[i]`
   - Reverses the Fisher-Yates shuffle
   - Result: Chunks back in original positions
   
5. **Concatenate Original Chunks**
   - Joins chunks: `chunk0 + chunk1 + chunk2 + ...`
   - Result: Data in original order before shuffling

**Part 2: Reverse Byte Transformation** (`reverseTransform()` method)
1. Takes current byte array (already unshuffled)
2. For each byte:
   - Calculates the **same** shift value as encryption
   - Uses **subtraction** instead of addition
   - Formula: `(byteValue - shift + 256) % 256`
3. Returns original byte array

**Important:**
- Rounds are processed in **reverse order** (10 → 1)
- Unshuffle BEFORE reverse transformation (mirror of encryption)
- Shuffle pattern recreation must be identical to encryption
- Shift calculation is **identical** to encryption

**D. Post-Whitening (IV XOR)**
```
xorWithIV(decryptedBytes, iv)
```
- **Purpose:** XOR with IV again to recover original plaintext
- **Note:** XOR is its own inverse (A XOR B XOR B = A)

**Result:**
- Original plaintext message: `"Hello, World!"`

**Code Location:**
- `crypto/BlockCipher.java` - `decrypt()`, `extractIVMultiPosition()`
- `crypto/PerRoundLogic.java` - `unsplitAndUnmix()`, `reverseTransform()`

**Educational Logging:**
- Shows IV extraction from both strategies
- Displays unshuffle patterns for each round
- Full hex displays for educational purposes
- Minimal logs for file decryption (performance)

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

**Code Location:** `crypto/RSAUtil.java` - `verifySignature()` method

---

### Step 6.5: Display Message

**What Happens:**
```
Platform.runLater(() ->
    chatArea.appendText("📩 [Sender]: " + decryptedMsg + "\n   ✓ Verified\n\n")
);
```

**What This Does:**
- Updates the GUI on the JavaFX application thread (thread-safe)
- Displays the decrypted message in the chat area
- Shows verification status (✓ or ⚠️)

**Purpose:**
- User sees the original message
- User knows if message is authentic

---

## 7. Supporting Components

### 7.1: KeyGenerator (`crypto/KeyGenerator.java`)

**Purpose:**
- Generates cryptographically secure random keys

**How It Works:**
1. Uses `SecureRandom` (not regular `Random`)
2. Generates 16 random bytes (128 bits)
3. Converts to hexadecimal string (32 characters)

**Usage:**
- Called by Client during symmetric key exchange
- Can be run standalone: `java crypto.KeyGenerator`

**Code Location:** `crypto/KeyGenerator.java` - `generate128BitKeyHex()` method

---

### 7.2: RSAUtil (`crypto/RSAUtil.java`)

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

**Code Location:** `crypto/RSAUtil.java` - entire file

---

### 7.3: BlockCipher (`crypto/BlockCipher.java`)

**Purpose:**
- Main encryption engine implementing custom 10-round cipher with advanced IV embedding

**Key Features:**
- **10 rounds** of multi-chunk transformation (AES-128 equivalent security level)
- **Random IV** per message (semantic security)
- **Dual IV Embedding:** XOR obfuscation + physical insertion strategies
- **Multi-chunk shuffling:** Fisher-Yates algorithm with 2-5 dynamic chunks per round
- **Key-dependent positions:** Unpredictable via XOR-based hash mixing
- **Byte-level operations:** Full 0-255 range supporting any data type
- **Dual logging modes:** Full educational logs vs. minimal file transfer logs
- **Base64 encoding** for safe transmission

**Key Methods:**

| Method | Purpose |
|--------|---------|
| `encrypt(byte[], byte[])` | Full encryption pipeline with dual IV embedding |
| `decrypt(byte[], byte[])` | Full decryption pipeline with dual IV extraction |
| `embedIVMultiPosition()` | Dual strategy IV embedding (XOR + INSERT) |
| `extractIVMultiPosition()` | Dual strategy IV extraction |
| `calculateIVPositions()` | Key-dependent position calculation using XOR hash mixing |
| `setFileTransferMode(boolean)` | Switch between full/minimal logging |
| `log()` | Educational logging (text messages) |
| `minimalLog()` | Performance logging (file transfers) |

**Security Implementations:**
1. ✅ IV support with dual embedding (prevents pattern recognition and IV extraction)
2. ✅ 10 rounds with multi-chunk shuffling (strong diffusion)
3. ✅ Fisher-Yates shuffle with key-seeded randomization
4. ✅ Unpredictable IV positions via Knuth multiplicative hash
5. ✅ Byte-level full range support (any data type)

**Code Location:** `crypto/BlockCipher.java` - 443 lines

---

### 7.4: PerRoundLogic (`crypto/PerRoundLogic.java`)

**Purpose:**
- Implements the transformation logic for each encryption round with multi-chunk shuffling

**Key Methods:**

| Method | Purpose |
|--------|---------|
| `splitAndMix()` | Splits data into 2-5 dynamic chunks and shuffles using Fisher-Yates |
| `unsplitAndUnmix()` | Reverses chunk shuffling and reconstructs original order |
| `transform()` | Forward byte transformation with key-dependent shifts |
| `reverseTransform()` | Reverse byte transformation (decryption) |

**Splitting and Shuffling:**
- **Dynamic Chunks:** 2-5 chunks based on data size and round number
- **Variable Boundaries:** Chunk sizes vary by round and key sum
- **Fisher-Yates Shuffle:** Randomizes chunk order with seed = `round * 31 + keySum`
- **Unpredictable Structure:** Same key needed to recreate shuffle pattern

**Byte Transformations:**
- **Forward Transform:** `(byte + shift) % 256`
- **Reverse Transform:** `(byte - shift + 256) % 256`
- **Shift Calculation:** `5 + (round × 3) + keyDigit`

**Why This Works:**
- Multi-chunk shuffling breaks positional patterns
- Different shift per round creates confusion
- Key-dependent operations prevent attacks without key
- Modulo 256 keeps values in byte range
- Reversible operations ensure correct decryption

**Critical Implementation Details:**
- **Encryption:** Split → Shuffle → Transform → Concatenate
- **Decryption:** Extract using shuffled sizes → Unshuffle → Reverse transform
- **Shuffle Pattern:** Must be identical in both directions
- **Boundary Calculation:** Uses shuffled chunk order, not original

**Code Location:** `crypto/PerRoundLogic.java` - 212 lines

---

### 7.5: File Transfer System

**Purpose:**
- Secure file encryption and transmission with optimized performance

**Components:**

**FileTransferHandler** (in controllers)
- Chunked file reading and encryption
- Progress tracking and UI updates
- Automatic file transfer mode switching
- File metadata transmission (name, size, type)

**Key Features:**
- **Automatic Mode Detection:** Detects file vs. text message
- **Performance Optimization:** Minimal logging during file operations
- **User Notifications:** Alerts user when switching to minimal logs
- **Seamless Integration:** Uses same BlockCipher engine as text messages

**Workflow:**
1. User selects file via file picker
2. FileTransferHandler reads file in chunks
3. `BlockCipher.setFileTransferMode(true)` enables minimal logging
4. Each chunk encrypted separately with unique IV
5. Progress displayed in UI
6. User notified: "📝 Encryption logs minimized for performance"
7. File transfer mode automatically disabled after completion

**Code Location:**
- `ui/controllers/ServerController.java` - File handling logic
- `ui/controllers/ClientController.java` - File handling logic

---

### 7.6: Logging System

**Purpose:**
- Shows encryption/decryption steps to users in real-time with educational value

**How It Works:**
1. Redirects `System.out` to custom PrintStream
2. Captures all console output from crypto classes
3. Uses `Platform.runLater()` for thread-safe JavaFX UI updates
4. Formats and displays in dedicated log TextArea

**Dual Mode Operation:**

**Full Educational Mode (Text Messages):**
- Complete hex displays of transformations
- IV breakdown showing chunk positions
- Shuffle patterns with percentages
- Strategic position calculations
- Combining visualizations
- Example: `[Round 1] Split into 3 chunks: [0]=3bytes [1]=2bytes [2]=2bytes`

**Minimal Performance Mode (File Transfers):**
- Single-line operation summaries
- No hex displays
- Example: `[ENCRYPT] 1024 bytes → IV gen → Pre-whiten → 10 rounds → IV embed → Base64`

**What Users See:**
- Key being used (shortened for privacy)
- IV generation and embedding strategy
- Each round's progress (1-10) with chunk shuffling
- Strategic positions for IV embedding
- Final encrypted/decrypted output
- Performance notifications for file transfers

**Purpose:**
- Educational - understand the algorithm step-by-step
- Debugging - verify correct operation
- Transparency - see cryptographic process
- Performance - avoid UI lag during file transfers

**Code Location:** 
- `ServerController.java` - `redirectSystemOutToTextArea()` method
- `ClientController.java` - `redirectSystemOutToTextArea()` method
- `BlockCipher.java` - `log()` and `minimalLog()` methods

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
4. Run 10 rounds of encryption:
   - Split data into 2-5 dynamic chunks
   - Shuffle chunks using Fisher-Yates (key-seeded)
   - Apply byte transformations with key-dependent shifts
   - Concatenate shuffled chunks
5. Embed IV using dual strategy:
   - XOR IV at multiple calculated positions (obfuscation)
   - Break IV into chunks and insert at positions (diffusion)
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
4. Extract dual-embedded IV:
   - Remove inserted IV chunks from calculated positions
   - Remove XOR obfuscation from calculated positions
   - Reconstruct 16-byte IV
5. Run 10 rounds of decryption in reverse (10→1):
   - Reverse byte transformations (subtraction instead of addition)
   - Recreate shuffle pattern using same seed
   - Extract shuffled chunks using correct boundaries
   - Unshuffle chunks back to original order
   - Concatenate original chunks
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
