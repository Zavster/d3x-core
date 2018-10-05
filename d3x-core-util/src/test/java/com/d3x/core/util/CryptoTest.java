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

import java.io.File;
import java.security.Key;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests the Crypto class
 *
 * @author Xavier Witdouck
 */
public class CryptoTest {

    @Test()
    public void sha256() {
        String expected = "6b90ee33357f0e0c694d1b6ea332e29a563b1e36c3214a04c4b0ee9b25a7807e";
        String actual = Crypto.sha256("Xavier Witdouck");
        Assert.assertEquals(actual, expected);
    }


    @Test()
    public void symmetric() throws Exception {
        Key key = Crypto.createKey("AES", 128, Option.empty());
        Crypto crypto = new Crypto("AES", key);
        String expected = "Xavier Witdouck";
        String cipherText = crypto.encrypt(expected);
        String clearText = crypto.decrypt(cipherText);
        Assert.assertNotEquals(cipherText, clearText);
        Assert.assertEquals(clearText, expected);
    }


    @Test()
    public void asymmetric() throws Exception {
        File publicFile = File.createTempFile("public", ".key");
        File privateFile = File.createTempFile("public", ".key");
        try {
            Crypto.createRsaKeyPair(publicFile, privateFile);
            Crypto publicKey = Crypto.withPublicKey("RSA", publicFile.toURI().toURL());
            Crypto privateKey = Crypto.withPrivateKey("RSA", privateFile.toURI().toURL());
            String expected = "Xavier Witdouck";
            String cipherText1 = publicKey.encrypt(expected);
            String cipherText2 = privateKey.encrypt(expected);
            String clearText1 = privateKey.decrypt(cipherText1);
            String clearText2 = publicKey.decrypt(cipherText2);
            Assert.assertNotEquals(cipherText1, clearText1);
            Assert.assertNotEquals(cipherText2, clearText2);
            Assert.assertNotEquals(cipherText1, cipherText2);
            Assert.assertEquals(clearText1, expected);
            Assert.assertEquals(clearText2, expected);
        } finally {
            publicFile.delete();
            privateFile.delete();
        }
    }

}
