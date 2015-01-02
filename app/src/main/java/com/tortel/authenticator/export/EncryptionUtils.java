package com.tortel.authenticator.export;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * A class to assist reading/writing AES encrypted files.
 */
public class EncryptionUtils {

    public static void writeFile(String fileName, String key, String content)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IOException {
        File file = new File(fileName);
        byte[] keyBytes = getKey(key);

        OutputStreamWriter osw;

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(keyBytes);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

        FileOutputStream fos = new FileOutputStream(file);
        CipherOutputStream cos = new CipherOutputStream(fos, cipher);
        osw = new OutputStreamWriter(cos, "UTF-8");

        BufferedWriter out = new BufferedWriter(osw);
        out.write(content);
        out.close();
    }

    public static String readFile(String fileName, String key)
            throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException,
            IOException {
        File file = new File(fileName);
        byte[] keyBytes = getKey(key);
        InputStreamReader isr;

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(keyBytes);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

        FileInputStream fis = new FileInputStream(file);
        CipherInputStream cis = new CipherInputStream(fis, cipher);
        isr = new InputStreamReader(cis, "UTF-8");

        BufferedReader in = new BufferedReader(isr);
        String line = in.readLine();
        System.out.println("Text read: <" + line + ">");
        in.close();
        return line;
    }

    private static byte[] getKey(String password) throws UnsupportedEncodingException {
        String key = "";
        while (key.length() < 16)
            key += password;
        return key.substring(0, 16).getBytes("UTF-8");
    }

}
