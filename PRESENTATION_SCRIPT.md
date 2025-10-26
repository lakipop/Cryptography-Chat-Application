# 🎤 PRESENTATION SCRIPT - Fleurdelyx Encryption Demo
**Time: 5-10 minutes | Group 06 | Presenter: Lakindu Sadumina**

---

## 📌 SECTION 1: INTRODUCTION (1 minute)
### 🗣️ **[SPEAK NATURALLY]**

```
Hello everyone. My name is Lakindu Sadumina.

I am from Group 06, and today I will show you our cryptography project.

We created our own encryption algorithm called "Fleurdelyx".

[Point to screen]

This is a chat application. We can send encrypted messages securely.

Let me show you how the encryption works step by step.

I will use my own name "Lakindu Sadumina" as the example message.
```

**[PAUSE - Take a breath]**

---

## 📌 SECTION 2: ENCRYPTION PROCESS DEMO (4-5 minutes)
### 🖥️ **[SHOW ON SCREEN + SPEAK]**

### **Step 1: Generate Random IV**
```
First, the system generates a random IV.

IV means Initialization Vector. It is 16 bytes.

[Point to IV on screen]

Every time we encrypt, we get different IV.

This is why same message looks different each time.

This is called "semantic security".
```

**[VISUAL HINT: Show IV hex values on screen]**
- Example: `BC F2 A3 BC BD 72 4C AF E8 A1 9C 73 4F 2D 8B 61`

---

### **Step 2: Convert Message to Bytes**
```
Next, we convert my name to bytes.

"Lakindu Sadumina" becomes 16 bytes in hexadecimal.

[Point to hex values]

Each letter has a number. For example:
L = 4C
a = 61
k = 6B

All 16 characters become 16 bytes.
```

**[VISUAL HINT: Show conversion on screen]**
```
Text: L   a   k   i   n   d   u  (space) S   a   d   u   m   i   n   a
Hex:  4C  61  6B  69  6E  64  75  20     53  61  64  75  6D  69  6E  61
```

---

### **Step 3: Pre-Whitening (XOR with IV)**
```
Now we mix the message with the IV.

We use XOR operation. This is like mixing colors.

[Point to XOR results]

For example:
4C XOR BC = F0
61 XOR F2 = 93

After XOR, the data looks completely random already.

This is the first protection layer.
```

**[VISUAL HINT: Show a few XOR calculations]**
```
Position 0: 4C XOR BC = F0
Position 1: 61 XOR F2 = 93
Position 2: 6B XOR A3 = C8
```

---

### **Step 4: Multi-Round Transformation (Main Part)**
```
Now comes the main encryption.

We do 10 rounds of transformation.

Each round has TWO steps:
1. Split and shuffle the data
2. Transform each byte

Let me show Round 1 as example.
```

---

#### **Round 1 - Part A: Split and Shuffle**
```
[Point to chunks on screen]

First, we split the 16 bytes into 5 chunks.

The sizes are different: 5 bytes, 3 bytes, 3 bytes, 2 bytes, 3 bytes.

Then we shuffle them using Fisher-Yates algorithm.

We use a seed number: Round number times 31, plus key sum.

Seed = 1 × 31 + 1589 = 1620

With this seed, we shuffle the chunks.

[Point to shuffled result]

Now the chunks are in different order.

This makes the pattern unpredictable.
```

**[VISUAL HINT: Show chunk splitting and shuffle pattern]**
```
BEFORE shuffle:
[Chunk0: 5 bytes][Chunk1: 3 bytes][Chunk2: 3 bytes][Chunk3: 2 bytes][Chunk4: 3 bytes]

AFTER shuffle (pattern: [3,1,4,0,2]):
[Chunk3][Chunk1][Chunk4][Chunk0][Chunk2]
```

---

#### **Round 1 - Part B: Byte Transformation**
```
After shuffling, we transform each byte.

We use this formula:
New byte = (old byte + shift value) mod 256

The shift value depends on:
- Round number
- Position in data
- Secret key

For example, in Round 1:
Shift = 5 + (1 × 3) + key byte = 8 + key byte

Each byte gets different shift value.

After transformation, Round 1 is complete.
```

**[VISUAL HINT: Show one or two byte transformations]**
```
Byte at position 0: 06 + 87 = 93 (in hex: 5D)
Byte at position 1: 22 + 91 = 113 (in hex: 71)
```

---

#### **Rounds 2-10: Continue Process**
```
We repeat this process 10 times.

Each round:
- Splits data into different number of chunks (2 to 5)
- Uses different shuffle pattern
- Uses bigger shift values

Round 2 uses 4 chunks.
Round 3 uses 3 chunks.
And so on...

By Round 10, the data is completely scrambled.

Nobody can recognize the original message.
```

**[VISUAL HINT: Just show final result]**
```
After Round 10: [2F, 8A, E3, 47, B9, 1C, D6, 5E, A2, F4, 6B, C8, 91, 3D, 7F, BA]
```

---

### **Step 5: Dual IV Embedding**
```
Now we have encrypted data.

But we need to hide the IV also.

We use TWO methods to hide IV:

Method 1: XOR Obfuscation
We XOR the IV bytes at secret positions in the ciphertext.
Only the key can find these positions.

Method 2: Physical Insertion
We break IV into 4 pieces.
We insert these pieces at calculated positions.

[Point to interleaved structure]

Now the IV is hidden inside the encrypted data.

Attacker cannot find the IV without the key.
```

**[VISUAL HINT: Show interleaved structure diagram]**
```
Final structure:
[Cipher][IV-piece1][Cipher][IV-piece2][Cipher][IV-piece3][Cipher][IV-piece4][Cipher]
```

---

### **Step 6: Base64 Encoding**
```
Finally, we encode everything with Base64.

This makes the encrypted data safe to send over network.

[Point to final output]

This is the final encrypted message.

It looks random. Nobody can read it.

Only the person with correct key can decrypt.
```

---

## 📌 SECTION 3: SIGNATURE VERIFICATION (1 minute)
### 🗣️ **[SPEAK]**

```
Now, how does the receiver know this message is really from me?

We use RSA digital signature.

[Point to RSA process on screen]

Before sending, I sign the message with my private key.

The receiver verifies with my public key.

If signature is valid, they know:
1. The message is from me
2. Nobody changed the message

This protects against "man-in-the-middle attack".

Even if someone intercepts the message, they cannot change it.

Because they don't have my private key.
```

---

## 📌 SECTION 4: SECURITY BENEFITS (2 minutes)
### 🗣️ **[SPEAK WITH CONFIDENCE]**

```
Let me explain the security advantages.

Our algorithm protects CIA:
- Confidentiality
- Integrity  
- Authentication

[Point to each as you say]

**Confidentiality:**
The message is encrypted. Nobody can read it without key.

We use 128-bit key. That is 2^128 possible keys.

This is 340 undecillion combinations.

Brute force attack is impossible.


**Integrity:**
We use digital signature with RSA.

If someone changes even 1 bit, the signature breaks.

The receiver knows the message was modified.


**Authentication:**
Digital signature proves who sent the message.

Only I have my private key.

So only I can create valid signature.
```

---

### **Protection Against Attacks:**

```
Let me give real examples:

**1. Man-in-the-Middle Attack:**
[Point to diagram]

Attacker intercepts my message to server.

They cannot read it - it is encrypted.

They cannot change it - signature will break.

They cannot replay old message - each has unique IV.

Our system blocks this attack.


**2. Frequency Analysis Attack:**
In old ciphers, letter 'E' appears most frequently.

Attacker can guess patterns.

[Point to screen]

In our cipher, we use:
- Multi-chunk shuffling
- Position-dependent transformation
- Random IV

Same letter 'a' in my name appears 4 times.

But all encrypt to different values!

No pattern to analyze.


**3. Known Plaintext Attack:**
Attacker knows some plaintext and ciphertext pairs.

In our system, same plaintext gives different ciphertext every time.

Because of random IV.

[Point to example]

"Lakindu Sadumina" encrypted now: [shows hex]
"Lakindu Sadumina" encrypted again: [different hex]

Completely different!

This is semantic security.
```

---

## 📌 SECTION 5: CONCLUSION (30 seconds)
### 🗣️ **[SPEAK]**

```
In summary:

Our Fleurdelyx algorithm uses:
- 10 rounds of encryption
- Multi-chunk shuffling with Fisher-Yates
- Dual IV embedding for extra security
- RSA signature for authentication

It protects confidentiality, integrity, and authentication.

It resists modern attacks like man-in-the-middle and frequency analysis.

[Point to working chat application]

And it works in real-time for secure communication.

Thank you for your attention.

Do you have any questions?
```

---

## 📋 QUICK REFERENCE CARD (Keep This With You!)

### **Key Points to Remember:**

1. **IV = Random 16 bytes** → Semantic security
2. **Pre-whitening = XOR with IV** → First protection
3. **10 Rounds = Split → Shuffle → Transform** → Main encryption
4. **Fisher-Yates = Shuffle chunks** → Seed = round × 31 + keySum
5. **Dual IV Embedding = XOR + Physical insertion** → Hide IV
6. **RSA Signature = Authentication** → Proves sender identity
7. **CIA Protected:**
   - **C**onfidentiality = Encryption with 128-bit key
   - **I**ntegrity = Digital signature
   - **A**uthentication = RSA public/private key

### **If You Forget Something:**
- Just point to the screen and say: "As you can see here..."
- Take your time with the hex numbers - they don't need to be perfect
- Focus on the CONCEPTS, not exact values

---

## 🎯 PRACTICE TIPS:

1. **Practice the introduction 3 times** - make it smooth
2. **For encryption demo:** Point to screen, speak slowly
3. **For attacks section:** Use hand gestures (point, block, etc.)
4. **For conclusion:** Speak with confidence, make eye contact

**Remember:** Your English is good enough! Speak slowly and clearly. The demonstration will help explain! 💪

Good luck with your presentation! 🌟
