package com.cfgglobal.common.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * Created with IntelliJ IDEA.
 * User: jacky zhung
 * Date: 14-12-3
 * Time: 下午3:08
 */
public class DESUtil {

    /**
     * 加密逻辑方法
     *
     * @param message
     * @param key
     * @return
     * @throws Exception
     */
    private static byte[] encryptProcess(String message, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        DESKeySpec desKeySpec = new DESKeySpec(key.getBytes("UTF-8"));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
        IvParameterSpec iv = new IvParameterSpec(key.getBytes("UTF-8"));
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        return cipher.doFinal(message.getBytes("UTF-8"));
    }

    /**
     * 解密逻辑方法
     *
     * @param message
     * @param key
     * @return
     * @throws Exception
     */
    private static String decryptProcess(String message, String key) throws Exception {
        byte[] bytesrc = convertHexString(message);
        Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
        DESKeySpec desKeySpec = new DESKeySpec(key.getBytes("UTF-8"));
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
        IvParameterSpec iv = new IvParameterSpec(key.getBytes("UTF-8"));
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
        byte[] retByte = cipher.doFinal(bytesrc);
        return new String(retByte);
    }

    /**
     * 16进制数组数转化
     *
     * @param ss
     * @return
     */
    private static byte[] convertHexString(String ss) throws Exception {
        byte digest[] = new byte[ss.length() / 2];
        for (int i = 0; i < digest.length; i++) {
            String byteString = ss.substring(2 * i, 2 * i + 2);
            int byteValue = Integer.parseInt(byteString, 16);
            digest[i] = (byte) byteValue;
        }
        return digest;
    }

    /**
     * 十六进制数转化
     *
     * @param b
     * @return
     * @throws Exception
     */
    private static String toHexString(byte b[]) throws Exception {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            String plainText = Integer.toHexString(0xff & b[i]);
            if (plainText.length() < 2)
                plainText = "0" + plainText;
            hexString.append(plainText);
        }

        return hexString.toString();
    }

    /**
     * 加密方法
     */
    public static String encrypt(String message, String key) throws Exception {
        String orignStr = java.net.URLEncoder.encode(message, "utf-8");
        String enStr = toHexString(encryptProcess(orignStr, key));
        return enStr;
    }


    /**
     * 解密方法
     */
    public static String decrypt(String message, String key) throws Exception {
        String decStr = java.net.URLDecoder.decode(decryptProcess(message, key), "utf-8");
        return decStr;
    }

    /**
     * 测试Main方法
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        String key = "ASdf12!@"; //密码只能为8位
        String message = "M00000894,donation";
        String enStr = encrypt(message, key);
        System.out.println("加密后:" + enStr);
        String decStr = decrypt(enStr, key);
        System.out.println("解密后:" + decStr);
    }
}
