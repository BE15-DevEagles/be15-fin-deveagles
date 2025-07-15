package com.deveagles.be15_deveagles_be.common.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AesConfig {

  private final String key;
  private final String iv;
  private final SecretKeySpec secretKey;
  private final IvParameterSpec ivSpec;

  public AesConfig(@Value("${aes.key}") String key, @Value("${aes.iv}") String iv) {
    this.key = key;
    this.iv = iv;
    this.secretKey = new SecretKeySpec(key.getBytes(), "AES");
    this.ivSpec = new IvParameterSpec(iv.getBytes());
  }

  public String encrypt(String plainText) {
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(encrypted);

    } catch (Exception e) {
      throw new RuntimeException("암호화 실패", e);
    }
  }

  public String decrypt(String cipherText) {
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
      byte[] decoded = Base64.getDecoder().decode(cipherText);
      byte[] decrypted = cipher.doFinal(decoded);
      return new String(decrypted, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("복호화 실패", e);
    }
  }
}
