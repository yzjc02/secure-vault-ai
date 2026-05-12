package com.jiacheng.securevault.security.encryption;

import com.jiacheng.securevault.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileEncryptionServiceTest {

    @Test
    void encryptThenDecryptShouldReturnOriginalContent() {
        FileEncryptionService service = serviceWithKey(1);
        byte[] plaintext = "module8 secret content".getBytes(StandardCharsets.UTF_8);

        byte[] ciphertext = service.encrypt(plaintext);

        assertThat(service.decrypt(ciphertext)).isEqualTo(plaintext);
    }

    @Test
    void samePlaintextShouldProduceDifferentCiphertext() {
        FileEncryptionService service = serviceWithKey(1);
        byte[] plaintext = "same plaintext".getBytes(StandardCharsets.UTF_8);

        byte[] first = service.encrypt(plaintext);
        byte[] second = service.encrypt(plaintext);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void ciphertextShouldNotContainPlaintext() {
        FileEncryptionService service = serviceWithKey(1);
        String secret = "MODULE8_TOP_SECRET_UNIT";

        byte[] ciphertext = service.encrypt(secret.getBytes(StandardCharsets.UTF_8));

        assertThat(new String(ciphertext, StandardCharsets.ISO_8859_1)).doesNotContain(secret);
    }

    @Test
    void decryptWithWrongKeyShouldFailSafely() {
        byte[] ciphertext = serviceWithKey(1).encrypt("private".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> serviceWithKey(2).decrypt(ciphertext))
                .isInstanceOf(BusinessException.class)
                .hasMessage("File decryption failed");
    }

    @Test
    void plainLegacyFileWithoutHeaderShouldStillBeReadable() {
        FileEncryptionService service = serviceWithKey(1);
        byte[] legacyPlaintext = "legacy plaintext".getBytes(StandardCharsets.UTF_8);

        assertThat(service.decrypt(legacyPlaintext)).isEqualTo(legacyPlaintext);
    }

    private FileEncryptionService serviceWithKey(int seed) {
        FileEncryptionProperties properties = new FileEncryptionProperties();
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (seed + i);
        }
        properties.setKey(Base64.getEncoder().encodeToString(key));
        FileEncryptionService service = new FileEncryptionService(properties, new MockEnvironment());
        service.init();
        return service;
    }
}
