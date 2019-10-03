package org.talend.dataquality.workshop;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

public class AESToolkit {

    static String initVector = "RandomInitVector"; // 16 bytes IV

    public static String encrypt(String key, String value) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());

            return Base64.encodeBase64String(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("failed to encrypt");
        }
    }

    public static String decrypt(String key, String encrypted) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(Base64.decodeBase64(encrypted));

            return new String(original);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("failed to decrypt");
        }
    }

    /**
     * read the reference results in plain text, encrypt with AES.
     * Then copy the encrypted data from console output back to file.
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            final String passPhrase = args[0];
            try (InputStream in = EvaluationProgram.class.getResourceAsStream("reference_data.csv")) {
                List<String> lines = IOUtils.readLines(in, "UTF-8");
                lines.stream().forEach(line -> System.out.println(encrypt(passPhrase, line)));
            }
        } else {
            System.err.println("put the pass phrase as param.");
        }
    }
}
