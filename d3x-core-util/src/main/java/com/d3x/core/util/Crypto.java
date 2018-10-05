/*
 * Copyright (C) 2018 D3X Systems - All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.d3x.core.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * A class that implements symmetric encryption using the JCE library shipped with the JRE
 *
 * @author Xavier Witdouck
 */
public class Crypto {

    private final Cipher encryptCipher;
    private final Cipher decryptCipher;

    /**
     * Constructor
     * @param algorithm the crypto algorithm
     * @param key       the key for algorithm
     */
    public Crypto(String algorithm, Key key) {
        try {
            this.encryptCipher = Cipher.getInstance(algorithm);
            this.encryptCipher.init(Cipher.ENCRYPT_MODE, key);
            this.decryptCipher = Cipher.getInstance(algorithm);
            this.decryptCipher.init(Cipher.DECRYPT_MODE, key);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to initialize Cryptography: " + t.getMessage(), t);
        }
    }


    /**
     * Returns a Crypto instance initialized from a public key
     * @param algorithm the algorithm name
     * @param url       the url to load key from
     * @return          the Crypto instance
     */
    public static Crypto withPublicKey(String algorithm, URL url) {
        try {
            final KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
            final byte[] bytes = IO.readBytes(url.openStream());
            final EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
            final Key key = keyFactory.generatePublic(keySpec);
            return new Crypto(algorithm, key);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize public key from " + url, ex);
        }
    }


    /**
     * Returns a Crypto instance initialized from a private key
     * @param algorithm the algorithm name
     * @param url       the url to load key from
     * @return          the Crypto instance
     */
    public static Crypto withPrivateKey(String algorithm, URL url) {
        try {
            final KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
            final byte[] bytes = IO.readBytes(url.openStream());
            final EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
            final Key key = keyFactory.generatePrivate(keySpec);
            return new Crypto(algorithm, key);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialise private key from " + url, ex);
        }
    }


    /**
     * Encrypts the text using the symmetric key for this class
     * @param text      the text to encrypt
     * @return          the cipher text, base 64 encoded
     * @throws RuntimeException    if this operation fails
     */
    public String encrypt(String text) {
        try {
            synchronized (encryptCipher) {
                final byte[] clearBytes = text.getBytes();
                final byte[] cipherBytes = encryptCipher.doFinal(clearBytes);
                return new String(Base64.getEncoder().encode(cipherBytes));
            }
        } catch (Exception t) {
            throw new RuntimeException("Failed to encrypt text using cipher", t);
        }
    }

    /**
     * Decrypts the cipher text using the symmetric key for this class
     * @param cipherText    the text to decrypt, base 64 encoded
     * @return              the clear text
     * @throws RuntimeException    if this operation fails
     */
    public String decrypt(String cipherText) {
        try {
            synchronized (decryptCipher) {
                final byte[] cipherBytes = Base64.getDecoder().decode(cipherText);
                final byte[] clearBytes = decryptCipher.doFinal(cipherBytes);
                return new String(clearBytes);
            }
        } catch (Exception t) {
            throw new RuntimeException("Failed to decrypt text using cipher", t);
        }
    }


    /**
     * Returns the secret key from the URL specified
     * @param url       the url to load key from
     * @param algorithm the cryptographic algorithm
     * @return          the secret key
     * @throws IOException  if key fails to load
     */
    public static SecretKey getSecretKey(URL url, String algorithm) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("The secret key url cannot be null");
        } else if (algorithm == null) {
            throw new IllegalArgumentException("The secret key algorithm cannot be null");
        } else {
            InputStream is = null;
            try {
                is = new BufferedInputStream(url.openStream());
                final byte[] bytes = new byte[1024 * 10];
                final int read = is.read(bytes);
                return new SecretKeySpec(bytes, 0, read, algorithm);
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }
    }


    /**
     * Generates a new RSA public/private key pair to the file specified
     * @param publicFile        the file to write public key
     * @param privateFile       the file to write private key
     */
    public static void createRsaKeyPair(File publicFile, File privateFile) {
        try {
            final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            final KeyPair keyPair = keyPairGenerator.generateKeyPair();
            final PrivateKey privateKey = keyPair.getPrivate();
            final PublicKey publicKey = keyPair.getPublic();
            IO.writeBytes(privateKey.getEncoded(), new FileOutputStream(privateFile));
            IO.writeBytes(publicKey.getEncoded(), new FileOutputStream(publicFile));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate key pair", ex);
        }
    }



    /**
     * Returns the SHA-256 hashed representation of the string
     * @param string    the string to hash
     * @return          the hashed string
     */
    public static String sha256(String string) {
        return new String(Crypto.sha256(string.getBytes(StandardCharsets.UTF_8)));
    }


    /**
     * Returns the SHA-256 hashed representation of the string
     * @param bytes     the bytes to hash
     * @return          the hashed bytes
     */
    public static byte[] sha256(byte[] bytes) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(bytes);
            final StringBuilder hexString = new StringBuilder();
            for (byte x : hash) {
                final String hex = Integer.toHexString(0xff & x);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().getBytes();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Failed to SHA-256 hash bytes", ex);
        }
    }


    /**
     * Generates a new secret key using the arguments specified
     * @param algorithm     the symmetric algorithm
     * @param keySize       the key isEmpty
     * @param file          the file to write the secret key to
     * @return              the newly generated key
     * @throws Exception    if key generation or persistence fails
     */
    public static SecretKey createKey(String algorithm, int keySize, Option<File> file) throws Exception {
        OutputStream os = null;
        try {
            final KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
            keyGen.init(keySize);
            final SecretKey secretKey = keyGen.generateKey();
            if (file.isPresent()) {
                os = new BufferedOutputStream(new FileOutputStream(file.get()));
                os.write(secretKey.getEncoded());
                System.out.println("Generated key output to: " + file.get().getAbsolutePath());
            }
            return secretKey;
        } catch (Throwable t) {
            throw new Exception("Failed to generate cryptographic secret key", t);
        } finally {
            IO.close(os);
        }
    }


    private static void shell() throws Exception {
        BufferedReader reader = null;
        try {
            final Matcher inputMatcher = Pattern.compile("(.+)\\s+(.+)").matcher("");
            final URL url = Crypto.class.getResource("/keys/kronos.dat");
            final SecretKey secretKey = Crypto.getSecretKey(url, "AES");
            final Crypto crypto = new Crypto("AES", secretKey);
            reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("d3x>");
            while (true) {
                final String input = reader.readLine();
                if (input == null) {
                    System.out.print("d3x>");
                } else if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                    System.out.println("Goodbye...");
                    System.exit(0);
                } else if (inputMatcher.reset(input).matches()) {
                    final String command = inputMatcher.group(1);
                    final String value = inputMatcher.group(2);
                    if (command.equalsIgnoreCase("encrypt")) {
                        System.out.println(crypto.encrypt(value));
                        System.out.print("d3x>");
                    } else if (command.equalsIgnoreCase("decrypt")) {
                        System.out.println(crypto.decrypt(value));
                        System.out.print("d3x>");
                    }
                } else {
                    System.out.println("Bad command");
                    System.out.print("zavtech>");
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

}
