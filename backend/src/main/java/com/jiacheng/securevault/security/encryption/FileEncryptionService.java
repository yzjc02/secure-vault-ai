package com.jiacheng.securevault.security.encryption;

import com.jiacheng.securevault.audit.enums.AuditAction;
import com.jiacheng.securevault.audit.enums.AuditResourceType;
import com.jiacheng.securevault.audit.service.AuditLogService;
import com.jiacheng.securevault.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;

@Service
public class FileEncryptionService {

    public static final String ALGORITHM = "AES_GCM";

    private static final byte[] MAGIC = "SVAIENC1".getBytes(StandardCharsets.US_ASCII);
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int AES_256_KEY_BYTES = 32;

    private final FileEncryptionProperties properties;
    private final Environment environment;
    private final AuditLogService auditLogService;
    private final SecureRandom secureRandom = new SecureRandom();

    private SecretKeySpec keySpec;

    public FileEncryptionService(FileEncryptionProperties properties, Environment environment) {
        this(properties, environment, null);
    }

    @Autowired
    public FileEncryptionService(FileEncryptionProperties properties,
                                 Environment environment,
                                 AuditLogService auditLogService) {
        this.properties = properties;
        this.environment = environment;
        this.auditLogService = auditLogService;
    }

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            return;
        }
        this.keySpec = new SecretKeySpec(resolveKey(), "AES");
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public byte[] encrypt(byte[] plaintext) {
        if (!properties.isEnabled()) {
            return plaintext == null ? new byte[0] : plaintext.clone();
        }
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext == null ? new byte[0] : plaintext);
            ByteArrayOutputStream output = new ByteArrayOutputStream(MAGIC.length + 1 + iv.length + ciphertext.length);
            output.write(MAGIC);
            output.write(iv.length);
            output.write(iv);
            output.write(ciphertext);
            return output.toByteArray();
        } catch (GeneralSecurityException | IOException ex) {
            throw new BusinessException(500, "File encryption failed");
        }
    }

    public byte[] decrypt(byte[] storedBytes) {
        if (storedBytes == null || storedBytes.length == 0) {
            return new byte[0];
        }
        if (!isEncryptedPayload(storedBytes)) {
            return storedBytes.clone();
        }
        if (!properties.isEnabled()) {
            recordDecryptFailure();
            throw new BusinessException(500, "File decryption failed");
        }
        try {
            ByteBuffer buffer = ByteBuffer.wrap(storedBytes);
            byte[] magic = new byte[MAGIC.length];
            buffer.get(magic);
            int ivLength = Byte.toUnsignedInt(buffer.get());
            if (ivLength < 8 || ivLength > 32 || buffer.remaining() <= ivLength) {
                recordDecryptFailure();
                throw new BusinessException(500, "File decryption failed");
            }
            byte[] iv = new byte[ivLength];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (AEADBadTagException ex) {
            recordDecryptFailure();
            throw new BusinessException(500, "File decryption failed");
        } catch (BusinessException ex) {
            throw ex;
        } catch (GeneralSecurityException | RuntimeException ex) {
            recordDecryptFailure();
            throw new BusinessException(500, "File decryption failed");
        }
    }

    public boolean isEncryptedPayload(byte[] storedBytes) {
        return storedBytes != null
                && storedBytes.length > MAGIC.length + 1
                && Arrays.equals(MAGIC, Arrays.copyOf(storedBytes, MAGIC.length));
    }

    private void recordDecryptFailure() {
        if (auditLogService != null) {
            auditLogService.recordForCurrentUser(AuditAction.FILE_DECRYPT_FAILURE,
                    AuditResourceType.FILE, null, false, "File decrypt failed");
        }
    }

    private byte[] resolveKey() {
        String configuredKey = properties.getKey();
        if (StringUtils.hasText(configuredKey)) {
            return decodeConfiguredKey(configuredKey.trim());
        }
        if (isProductionProfile()) {
            throw new IllegalStateException("FILE_ENCRYPTION_KEY must be configured in production");
        }
        return loadOrCreateDevKey();
    }

    private byte[] decodeConfiguredKey(String configuredKey) {
        try {
            byte[] decoded = Base64.getDecoder().decode(configuredKey);
            if (decoded.length == AES_256_KEY_BYTES) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to passphrase derivation.
        }
        if (configuredKey.length() >= AES_256_KEY_BYTES) {
            return sha256(configuredKey.getBytes(StandardCharsets.UTF_8));
        }
        throw new IllegalStateException("FILE_ENCRYPTION_KEY must be Base64-encoded 32 bytes or at least 32 characters");
    }

    private byte[] loadOrCreateDevKey() {
        Path keyPath = Paths.get(properties.getDevKeyFile()).toAbsolutePath().normalize();
        try {
            if (Files.isRegularFile(keyPath)) {
                String existing = Files.readString(keyPath, StandardCharsets.UTF_8).trim();
                return decodeConfiguredKey(existing);
            }
            byte[] key = new byte[AES_256_KEY_BYTES];
            secureRandom.nextBytes(key);
            Files.createDirectories(keyPath.getParent());
            Files.writeString(keyPath, Base64.getEncoder().encodeToString(key), StandardCharsets.UTF_8);
            return key;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to initialize local file encryption key");
        }
    }

    private boolean isProductionProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile -> profile.equals("prod") || profile.equals("production"));
    }

    private byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to initialize file encryption key");
        }
    }
}
