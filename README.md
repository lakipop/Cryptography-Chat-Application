# Fleurdelyx Custom Cryptographic Algorithm

[![Java](https://img.shields.io/badge/Java-11+-orange.svg)](https://www.oracle.com/java/)
[![Cryptography](https://img.shields.io/badge/Cryptography-Custom%20Algorithm-red.svg)]()

A **hybrid cryptographic system** implementing a custom 10-round block cipher with IV support and byte-level operations, combined with RSA asymmetric encryption for secure messaging.

---

## 📋 Overview

**Fleurdelyx** is an educational cryptographic project demonstrating modern encryption principles through a secure chat application. The system combines:

- **Custom 10-Round Block Cipher** - Symmetric encryption for message confidentiality
- **RSA 2048-bit Encryption** - Asymmetric encryption for secure key exchange
- **Digital Signatures (SHA-256)** - Message authentication and integrity verification
- **Initialization Vectors (IV)** - Semantic security for pattern protection

This project shows how real-world encryption systems work, similar to applications like WhatsApp or Signal, while remaining simple enough to understand for educational purposes.

---

## ✨ Key Features

- **Hybrid Encryption**: RSA for key exchange + Custom cipher for messages
- **Semantic Security**: Random IV ensures same message encrypts differently each time
- **10 Rounds**: Strong diffusion matching AES-128 security level with multi-chunk shuffling
- **Dual IV Embedding**: XOR obfuscation + physical insertion for maximum unpredictability
- **Byte-Level**: Full 0-255 range supporting any data type (text, files, images)
- **Digital Signatures**: Proves message authenticity and detects tampering
- **Real-time Chat**: Client-server application with encrypted communication
- **File Transfer**: Secure transmission of documents, images, and files with optimized performance
- **Educational Logging**: Step-by-step encryption visualization with hex displays and percentages
- **Visual Logging**: Watch encryption/decryption process step-by-step

---

## � How to Run

### Prerequisites
- Java JDK 11 or higher
- Command line / Terminal

### Quick Start

**1. Compile the Project**
```bash
cd d:\Projects\Fleurdelyx-Cryptographic-Algorithm\src
> **Use this method — Easiest way to run**
>
> Run the compiled files with the provided batch script:
>
> ```bat
> START_CHAT.bat
> ```

**2. Start the Server**
```bash
java ChatServerGUI
```
- Opens "🔐 Server" window
- Waits for client connection on port 12345

**3. Start the Client**
```bash
java ChatClientGUI
```
- Opens "👤 Client" window
- Automatically connects to server

**4. Start Chatting**
- Type message in either window
- Click "Send 🔒" or press Enter
- View encryption logs in second tab

---

## 🔐 Security Techniques

### 1. Hybrid Encryption

**What:** Combines two types of encryption for optimal security and performance.

**How it works:**
- **RSA (Asymmetric)**: Used once at startup to exchange the symmetric key
  - Public + Private key pairs (2048-bit)
  - Slow but very secure
  - Protects key transmission
  
- **Fleurdelyx Cipher (Symmetric)**: Used for all messages
  - Single shared 128-bit key
  - Fast and efficient
  - Encrypts actual message content

**Benefit:** Security of RSA with speed of symmetric encryption.

### 2. Initialization Vector (IV)

**What:** 128-bit random value generated fresh for every message.

**Purpose:** Provides semantic security - same plaintext produces different ciphertext each time.

**How:** XORed with plaintext before encryption, then prepended to ciphertext for decryption.

**Benefit:** Prevents pattern analysis attacks. Even sending "Hello" 100 times creates 100 unique encrypted outputs.

### 3. Digital Signatures

**What:** Cryptographic proof of message authenticity using SHA-256 and RSA.

**Process:**
1. Hash message with SHA-256
2. Encrypt hash with sender's private key = signature
3. Receiver verifies with sender's public key

**Benefit:** Proves who sent the message and detects any tampering.

---

## 🧮 Custom Algorithm Logic

### Fleurdelyx 10-Round Block Cipher

**Design Philosophy:** Position-dependent byte transformations with chunk-based shuffling through multiple rounds.

**Encryption Process:**

```
Plaintext → Convert to Bytes → XOR with IV → 10 Rounds (Split/Shuffle/Transform) → Dual IV Embed → Base64 → Ciphertext
```

**Each Round Transformation:**
```
1. Split data into 2-5 dynamic chunks (size varies by round and key)
2. Shuffle chunks using Fisher-Yates algorithm (key-seeded)
3. For each byte at position i:
     shift = 5 + (round × 3) + key_digit
     encrypted_byte = (byte + shift) mod 256
4. Concatenate shuffled chunks
```

**Dual IV Embedding:**
```
1. XOR Strategy: XOR IV at multiple calculated positions (obfuscation)
2. INSERT Strategy: Break IV into chunks, insert at strategic positions (diffusion)
   Result: [Cipher] + [IV-Chunk1] + [Cipher] + [IV-Chunk2] + ... (interleaved)
```

**Why 10 Rounds?**
- Matches AES-128 standard (10 rounds for 128-bit keys)
- Creates sufficient confusion (complex key-ciphertext relationship)
- Provides strong diffusion (changes spread throughout message)
- Chunk shuffling adds structural unpredictability
- Balances security with performance

**Decryption Process:**
```
Ciphertext → Base64 Decode → Extract Dual-Embedded IV → Reverse 10 Rounds (Unshuffle/Reverse Transform) → XOR with IV → Plaintext
```

**Reverse transformation:**
```
1. Extract IV chunks from insertion positions
2. Remove XOR from strategic positions
3. For each round (10 down to 1):
     Reverse byte transformation: decrypted_byte = (byte - shift + 256) mod 256
     Unshuffle chunks back to original order
4. Concatenate original chunks
```

---

## 🛡️ Security Benefits

### What We Achieve

| Security Property | Implementation | Benefit |
|------------------|----------------|---------|
| **Confidentiality** | 10-round encryption | Message content protected from eavesdroppers |
| **Semantic Security** | Random IV per message | Same message looks different each time |
| **Authenticity** | RSA digital signatures | Proves sender identity |
| **Integrity** | SHA-256 hashing | Detects message tampering |
| **Non-repudiation** | Private key signatures | Sender cannot deny sending |

### Protection Against Attacks

- ✅ **Pattern Analysis**: Random IV prevents statistical analysis
- ✅ **Known-Plaintext**: Each encryption unique due to IV
- ✅ **Brute Force**: 2^128 key space = trillions of years to crack
- ✅ **Man-in-the-Middle**: RSA key exchange prevents interception
- ✅ **Message Tampering**: Signature verification detects changes

---

## 📊 Comparison with AES

### Similarities to AES-128

| Aspect | Fleurdelyx | AES-128 |
|--------|-----------|---------|
| **Rounds** | 10 rounds | 10 rounds |
| **Key Size** | 128 bits | 128 bits |
| **Design** | Substitution cipher | Substitution-permutation |
| **Approach** | Byte transformations | Byte substitution + mixing |

### Key Differences

| Feature | Fleurdelyx | AES-128 |
|---------|-----------|---------|
| **Standardization** | Educational custom | NIST standard (approved) |
| **Peer Review** | Not reviewed | Extensively analyzed |
| **S-Boxes** | Formula-based shifts | Pre-computed tables |
| **MixColumns** | Not implemented | Yes (diffusion layer) |
| **Production Use** | ❌ Educational only | ✅ Industry standard |

### Security Level

**Fleurdelyx:**
- 🟢 Strong for educational demonstration
- 🟢 Follows proven cryptographic principles (rounds, IV, key-dependent)
- 🟡 Not professionally audited or standardized
- 🔴 Should NOT be used in production systems

**AES-128:**
- 🟢 Military-grade encryption
- 🟢 Used worldwide (banks, governments, messaging apps)
- 🟢 Withstood decades of cryptanalysis
- 🟢 Production-ready and trusted

**Recommendation:** Use AES for real applications, use Fleurdelyx to learn how encryption works.

---

## 📚 Educational Value

### What You Learn from This Project

**1. Cryptographic Concepts**
- How symmetric and asymmetric encryption work together
- Why initialization vectors are essential
- How digital signatures provide authentication
- The importance of multiple encryption rounds

**2. Security Principles**
- Confusion: Making key-ciphertext relationship complex
- Diffusion: Spreading changes throughout the message
- Semantic security: Same input creates different outputs
- Hybrid systems: Combining different encryption types

**3. Practical Implementation**
- Key generation and exchange protocols
- Round-based transformation logic
- Signature creation and verification
- Network security in client-server applications

**4. Real-World Parallels**
- Similar to TLS/SSL (web security)
- Comparable to WhatsApp/Signal encryption
- Like PGP email encryption
- Principles used in VPNs

### Lessons Learned

✅ **Don't roll your own crypto** - Use established standards like AES for production  
✅ **Multiple layers matter** - Encryption + signatures provide complete security  
✅ **IVs are crucial** - Without them, patterns emerge and security weakens  
✅ **Key exchange is hard** - RSA solves the "how do we share keys securely" problem  
✅ **Testing is essential** - Cryptography must be verified to ensure correctness  

---

## 🗂️ Project Structure

```
Cryptography-Chat-Application/
│
├── src/
│   ├── crypto/
│   │   ├── BlockCipher.java           # 10-round encryption engine with dual IV embedding
│   │   ├── PerRoundLogic.java         # Multi-chunk shuffling and byte transformations
│   │   ├── KeyGenerator.java          # Secure random key generation
│   │   └── RSAUtil.java               # RSA operations and digital signatures
│   │
│   ├── ui/
│   │   ├── ChatServerFX.java          # JavaFX server application
│   │   ├── ChatClientFX.java          # JavaFX client application
│   │   └── controllers/
│   │       ├── ServerController.java  # Server UI logic and file handling
│   │       └── ClientController.java  # Client UI logic and file handling
│   │
│   └── resources/
│       ├── fxml/                      # JavaFX UI layouts
│       └── css/                       # Modern green/zinc theme
│
├── test/
│   └── crypto/
│       └── CryptoTestSuite.java       # Comprehensive encryption tests
│
├── README.md                          # Project documentation
└── pom.xml                            # Maven configuration
```

---

## � Academic Context

This project was developed as an educational demonstration for a university mini-project on **Symmetric Key Encryption**. It showcases:

- Understanding of cryptographic principles
- Implementation of security best practices
- Hybrid encryption system design
- Real-time secure communication
- Modern UI/UX development

**⚠️ Important:** This is an educational implementation. For production systems, always use established standards like **AES-256**, **RSA with proper PKI**, and **TLS/SSL** for network security.

---

## 📝 License

This project is created for educational purposes to demonstrate cryptographic principles.

**Disclaimer:** This custom cipher is for learning only. Do not use in production. Use AES, RSA, and TLS for real applications.

---

## � References & Further Reading

- **RSA Algorithm**: Rivest, Shamir, Adleman (1977)
- **SHA-256**: NIST FIPS 180-4
- **Java Cryptography Architecture**: Oracle Documentation
- **Block Cipher Design**: Applied Cryptography by Bruce Schneier
- **AES Standard**: NIST FIPS 197

---

<div align="center">

**🔐 Built with Java | Secured with Hybrid Cryptography | Designed for Learning 🔐**

⭐ Educational demonstration of modern encryption principles

</div>
